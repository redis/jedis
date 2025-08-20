
package redis.clients.jedis.mcf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Endpoint;

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

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

    private Endpoint endpoint;
    private HealthCheckStrategy strategy;
    private AtomicReference<HealthCheckResult> resultRef = new AtomicReference<HealthCheckResult>();
    private Consumer<HealthStatusChangeEvent> statusChangeCallback;

    private ScheduledExecutorService scheduler;
    private ExecutorService executor = Executors.newCachedThreadPool();

    HealthCheckImpl(Endpoint endpoint, HealthCheckStrategy strategy,
        Consumer<HealthStatusChangeEvent> statusChangeCallback) {

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
        scheduler.scheduleAtFixedRate(this::healthCheck, 0, strategy.getInterval(), TimeUnit.MILLISECONDS);
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

    private void healthCheck() {
        long me = System.currentTimeMillis();
        Future<?> future = executor.submit(() -> {
            HealthStatus newStatus = strategy.doHealthCheck(endpoint);
            safeUpdate(me, newStatus);
            log.trace("Health check completed for {} with status {}", endpoint, newStatus);
        });

        try {
            future.get(strategy.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException e) {
            // Cancel immediately on timeout or exec exception
            future.cancel(true);
            safeUpdate(me, HealthStatus.UNHEALTHY);
            log.warn("Health check timed out or failed for {}", endpoint, e);
        } catch (InterruptedException e) {
            // Health check thread was interrupted
            future.cancel(true);
            safeUpdate(me, HealthStatus.UNHEALTHY);
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("Health check interrupted for {}", endpoint, e);
        }
    }

    // just to avoid to replace status with an outdated result from another healthCheck
    private void safeUpdate(long owner, HealthStatus status) {
        HealthCheckResult newResult = new HealthCheckResult(owner, status);
        HealthCheckResult oldResult = resultRef.getAndUpdate(current -> {
            if (current.getTimestamp() < owner) {
                return newResult;
            }
            return current;
        });
        if (oldResult.getStatus() != status) {
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
        return (strategy.getTimeout() + strategy.getInterval()) * strategy.minConsecutiveSuccessCount();
    }

}
