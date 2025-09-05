
package redis.clients.jedis.mcf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.annots.VisibleForTesting;

public class HealthCheckImpl implements HealthCheck {

  private static class HealthCheckResult {
    private final long timestamp;
    private final HealthStatus status;

    public HealthCheckResult(long timestamp, HealthStatus status) {
      this.timestamp = timestamp;
      this.status = status;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public HealthStatus getStatus() {
      return status;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(HealthCheckImpl.class);

  private Endpoint endpoint;
  private HealthCheckStrategy strategy;
  private AtomicReference<HealthCheckResult> resultRef = new AtomicReference<HealthCheckResult>();
  private Consumer<HealthStatusChangeEvent> statusChangeCallback;

  private final ScheduledExecutorService scheduler;
  private final ExecutorService executor;

  HealthCheckImpl(Endpoint endpoint, HealthCheckStrategy strategy,
      Consumer<HealthStatusChangeEvent> statusChangeCallback) {

    this.endpoint = endpoint;
    this.strategy = strategy;
    this.statusChangeCallback = statusChangeCallback;
    resultRef.set(new HealthCheckResult(0L, HealthStatus.UNKNOWN));

    executor = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r, "jedis-healthcheck-worker-" + this.endpoint);
      t.setDaemon(true);
      return t;
    });
    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "jedis-healthcheck-" + this.endpoint);
      t.setDaemon(true);
      return t;
    });
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public HealthStatus getStatus() {
    return resultRef.get().getStatus();
  }

  public void start() {
    scheduler.scheduleAtFixedRate(this::healthCheck, 0, strategy.getInterval(),
      TimeUnit.MILLISECONDS);
  }

  public void stop() {
    strategy.close();
    this.statusChangeCallback = null;
    scheduler.shutdown();
    executor.shutdown();
    try {
      // Wait for graceful shutdown then force if required
      if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
      if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      // Force shutdown immediately
      scheduler.shutdownNow();
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private HealthStatus doHealthCheck() {
    HealthStatus newStatus = strategy.doHealthCheck(endpoint);
    log.trace("Health check completed for {} with status {}", endpoint, newStatus);
    return newStatus;
  }

  private void healthCheck() {
    long me = System.currentTimeMillis();
    Future<HealthStatus> future = executor.submit(this::doHealthCheck);
    HealthStatus update = null;

    try {
      update = future.get(strategy.getTimeout(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException | ExecutionException e) {
      future.cancel(true);
      if (log.isWarnEnabled()) {
        log.warn(String.format("Health check timed out or failed for %s.", endpoint), e);
      }
      update = performHealthCheckWithRetries();
    } catch (InterruptedException e) {
      // Health check thread was interrupted
      future.cancel(true);
      Thread.currentThread().interrupt(); // Restore interrupted status
      if (log.isWarnEnabled()) {
        log.warn(String.format("Health check interrupted for %s.", endpoint), e);
      }
      // thread interrupted, stop health check process
      return;
    }
    safeUpdate(me, update);
  }

  /**
   * Perform health check with retries. All retries are attempted in a single new executor thread,
   * no additional threads are created on each attempt due to resource optimization considerations.
   * Maximum wait time is (timeout + delayBetweenRetries) * numberOfRetries. Main check interval
   * might be impacted if retries take longer than the {@link HealthCheckStrategy#getInterval()}
   * (though {@link HealthCheckImpl#executor} pool can extend, {@link HealthCheckImpl#scheduler} is
   * single threaded).
   * @return the result of retries
   */
  private HealthStatus performHealthCheckWithRetries() {
    int attempts = Math.max(0, strategy.getNumberOfRetries());
    if (attempts == 0) {
      return HealthStatus.UNHEALTHY;
    }

    Future<HealthStatus> retries = executor.submit(() -> {
      for (int i = 0; i < attempts; i++) {
        try {
          Thread.sleep(strategy.getDelayInBetweenRetries());
          return doHealthCheck();
        } catch (InterruptedException e) {
          // Health check thread was interrupted
          Thread.currentThread().interrupt(); // Restore interrupted status
          if (log.isWarnEnabled()) {
            log.warn(
              String.format("Health check retry interrupted for %s. Attempt:%d", endpoint, i + 1),
              e);
          }
          break;
        } catch (Exception e) {
          if (log.isWarnEnabled()) {
            log.warn(String.format("Health check retry failed for %s. Attempt:%d", endpoint, i + 1),
              e);
          }
          // continue to next attempt
        }
      }
      return HealthStatus.UNHEALTHY;
    });

    try {
      long timeout = (strategy.getTimeout() + strategy.getDelayInBetweenRetries()) * attempts;
      return retries.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException | ExecutionException e) {
      retries.cancel(true);
      if (log.isWarnEnabled()) {
        log.warn(String.format("Health check retries timed out or failed for %s", endpoint), e);
      }
    } catch (InterruptedException e) {
      retries.cancel(true);
      Thread.currentThread().interrupt(); // Restore interrupted status
      if (log.isWarnEnabled()) {
        log.warn(String.format("Health check retries interrupted for %s", endpoint), e);
      }
    }
    return HealthStatus.UNHEALTHY;
  }

  /**
   * just to avoid to replace status with an outdated result from another healthCheck
   * 
   * <pre>
   * Health Check Race Condition Prevention
   * 
   * Problem: Async health checks can complete out of order, causing stale results
   * to overwrite newer ones.
   * 
   * Timeline Example:
   * ─────────────────────────────────────────────────────────────────
   * T0: Start Check #1 ────────────────────┐
   * T1: Start Check #2 ──────────┐         │
   * T2:                          │         │
   * T3: Check #2 completes ──────┘         │  → status = "Healthy"
   * T4: Check #1 completes ────────────────┘  → status = "Unhealthy" (STALE!)
   * 
   * 
   * Result: Final status shows "Unhealthy" even though the most recent 
   * check (#2) returned "Healthy"
   * 
   * Solution: Track execution order/timestamp to ignore outdated results
   * </pre>
   * 
   * @param owner the timestamp of the health check that is updating the status
   * @param status the new status to set
   */
  @VisibleForTesting
  void safeUpdate(long owner, HealthStatus status) {
    HealthCheckResult newResult = new HealthCheckResult(owner, status);
    AtomicBoolean wasUpdated = new AtomicBoolean(false);

    HealthCheckResult oldResult = resultRef.getAndUpdate(current -> {
      if (current.getTimestamp() < owner) {
        wasUpdated.set(true);
        return newResult;
      }
      wasUpdated.set(false);
      return current;
    });

    if (wasUpdated.get() && oldResult.getStatus() != status) {
      log.info("Health status changed for {} from {} to {}", endpoint, oldResult.getStatus(),
        status);
      // notify listeners
      notifyListeners(oldResult.getStatus(), status);
    }
  }

  private void notifyListeners(HealthStatus oldStatus, HealthStatus newStatus) {
    if (statusChangeCallback != null) {
      statusChangeCallback.accept(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
    }
  }

  @Override
  public long getMaxWaitFor() {
    return (strategy.getTimeout() + strategy.getDelayInBetweenRetries())
        * (1 + strategy.getNumberOfRetries());
  }

}
