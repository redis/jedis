
package redis.clients.jedis.mcf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.mcf.ProbePolicy.ProbeContext;
import redis.clients.jedis.util.JedisAsserts;

public class HealthCheckImpl implements HealthCheck {

  static class HealthProbeContext implements ProbeContext {
    private final ProbePolicy policy;
    private int remainingProbes;
    private int successes;
    private int fails;
    private boolean isCompleted;
    private HealthStatus result;

    HealthProbeContext(ProbePolicy policy, int maxProbes) {
      this.policy = policy;
      this.remainingProbes = maxProbes;
    }

    void record(boolean success) {
      if (success) {
        this.successes++;
      } else {
        this.fails++;
      }
      remainingProbes--;
      ProbePolicy.Decision decision = policy.evaluate(this);
      if (decision == ProbePolicy.Decision.SUCCESS) {
        setCompleted(HealthStatus.HEALTHY);
      } else if (decision == ProbePolicy.Decision.FAIL) {
        setCompleted(HealthStatus.UNHEALTHY);
      }
    }

    public int getRemainingProbes() {
      return remainingProbes;
    }

    public int getSuccesses() {
      return successes;
    }

    public int getFails() {
      return fails;
    }

    void setCompleted(HealthStatus status) {
      this.result = status;
      this.isCompleted = true;
    }

    boolean isCompleted() {
      return isCompleted;
    }

    HealthStatus getResult() {
      return result;
    }
  }

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

  private static AtomicInteger workerCounter = new AtomicInteger(1);
  private static ExecutorService workers = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "jedis-healthcheck-worker-" + workerCounter.getAndIncrement());
    t.setDaemon(true);
    return t;
  });

  private Endpoint endpoint;
  private HealthCheckStrategy strategy;
  private AtomicReference<HealthCheckResult> resultRef = new AtomicReference<HealthCheckResult>();
  private Consumer<HealthStatusChangeEvent> statusChangeCallback;

  private final ScheduledExecutorService scheduler;

  HealthCheckImpl(Endpoint endpoint, HealthCheckStrategy strategy,
      Consumer<HealthStatusChangeEvent> statusChangeCallback) {

    JedisAsserts.isTrue(strategy.getNumProbes() > 0,
      "Number of HealthCheckStrategy probes must be greater than 0");
    this.endpoint = endpoint;
    this.strategy = strategy;
    this.statusChangeCallback = statusChangeCallback;
    resultRef.set(new HealthCheckResult(0L, HealthStatus.UNKNOWN));

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

    try {
      // Wait for graceful shutdown then force if required
      if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      // Force shutdown immediately
      scheduler.shutdownNow();
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
    HealthStatus update = null;
    HealthProbeContext probeContext = new HealthProbeContext(strategy.getPolicy(),
        strategy.getNumProbes());

    while (!probeContext.isCompleted()) {
      Future<HealthStatus> future = workers.submit(this::doHealthCheck);
      try {
        update = future.get(strategy.getTimeout(), TimeUnit.MILLISECONDS);
        probeContext.record(update == HealthStatus.HEALTHY);
      } catch (TimeoutException | ExecutionException e) {
        future.cancel(true);
        if (log.isWarnEnabled()) {
          log.warn(String.format("Health check timed out or failed for %s.", endpoint), e);
        }
        probeContext.record(false);
      } catch (InterruptedException e) {// Health check thread was interrupted
        future.cancel(true);
        Thread.currentThread().interrupt(); // Restore interrupted status
        log.warn(String.format("Health check interrupted for %s.", endpoint), e);
        // thread interrupted, stop health check process
        return;
      }
      if (!probeContext.isCompleted()) {
        try {
          Thread.sleep(strategy.getDelayInBetweenProbes());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // Restore interrupted status
          log.warn(String.format("Health check interrupted while sleeping for %s.", endpoint), e);
          // thread interrupted, stop health check process
          return;
        }
      }
    }

    safeUpdate(me, probeContext.getResult());
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
   * How Parallel Health Checks Can Occur:
   * 1. Timeout scenario: A scheduled health check times out and future.cancel(true)
   *    is called, but the actual health check operation continues running in the
   *    background thread and may complete any time later
   * 2. Scheduler overlap: If a health check takes longer than the configured
   *    interval, the next scheduled check can start before the previous one finishes
   * 3. Interruption handling: When a health check thread is interrupted, it may
   *    still complete its operation before recognizing the interruption
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
    return (strategy.getTimeout() + strategy.getDelayInBetweenProbes()) * strategy.getNumProbes();
  }

}
