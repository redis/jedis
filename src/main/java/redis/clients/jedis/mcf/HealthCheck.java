
package redis.clients.jedis.mcf;

import java.util.AbstractMap.SimpleEntry;
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

public class HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

    private Endpoint endpoint;
    private HealthCheckStrategy strategy;
    private AtomicReference<SimpleEntry<Long, HealthStatus>> statusRef = new AtomicReference<SimpleEntry<Long, HealthStatus>>();
    private Consumer<HealthStatusChangeEvent> statusChangeCallback;

    private ScheduledExecutorService scheduler;
    private ExecutorService executor = Executors.newCachedThreadPool();

    HealthCheck(Endpoint endpoint, HealthCheckStrategy strategy,
        Consumer<HealthStatusChangeEvent> statusChangeCallback) {
        this.endpoint = endpoint;
        this.strategy = strategy;
        this.statusChangeCallback = statusChangeCallback;
        statusRef.set(new SimpleEntry<>(0L, HealthStatus.UNKNOWN));

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
        return statusRef.get().getValue();
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
        SimpleEntry<Long, HealthStatus> newStatus = new SimpleEntry<>(owner, status);
        SimpleEntry<Long, HealthStatus> oldStatus = statusRef.getAndUpdate(current -> {
            if (current.getKey() < owner) {
                return newStatus;
            }
            return current;
        });
        if (oldStatus.getValue() != status) {
            // notify listeners
            notifyListeners(oldStatus.getValue(), status);
        }
    }

    private void notifyListeners(HealthStatus oldStatus, HealthStatus newStatus) {
        if (statusChangeCallback != null) {
            statusChangeCallback.accept(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
        }
    }

    public long getMaxWaitDuration() {
        return strategy.getMaxWaitDuration();
    }

}
