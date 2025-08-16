package redis.clients.jedis.mcf;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import redis.clients.jedis.Endpoint;

public class HealthStatusManager {

    private HealthCheckCollection healthChecks = new HealthCheckCollection();
    private final List<HealthStatusListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Endpoint, List<HealthStatusListener>> endpointListeners = new ConcurrentHashMap<Endpoint, List<HealthStatusListener>>();

    public void registerListener(HealthStatusListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(HealthStatusListener listener) {
        listeners.remove(listener);
    }

    public void registerListener(Endpoint endpoint, HealthStatusListener listener) {
        endpointListeners.computeIfAbsent(endpoint, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unregisterListener(Endpoint endpoint, HealthStatusListener listener) {
        endpointListeners.computeIfPresent(endpoint, (k, v) -> {
            v.remove(listener);
            return v;
        });
    }

    public void notifyListeners(HealthStatusChangeEvent eventArgs) {
        endpointListeners.computeIfPresent(eventArgs.getEndpoint(), (k, v) -> {
            for (HealthStatusListener listener : v) {
                listener.onStatusChange(eventArgs);
            }
            return v;
        });
        for (HealthStatusListener listener : listeners) {
            listener.onStatusChange(eventArgs);
        }
    }

    public HealthCheck add(Endpoint endpoint, HealthCheckStrategy strategy) {
        HealthCheck hc = new HealthCheckImpl(endpoint, strategy, this::notifyListeners);
        HealthCheck old = healthChecks.add(hc);
        hc.start();
        if (old != null) {
            old.stop();
        }
        return hc;
    }

    public void addAll(Endpoint[] endpoints, HealthCheckStrategy strategy) {
        for (Endpoint endpoint : endpoints) {
            add(endpoint, strategy);
        }
    }

    public void remove(Endpoint endpoint) {
        HealthCheck old = healthChecks.remove(endpoint);
        if (old != null) {
            old.stop();
        }
    }

    public void removeAll(Endpoint[] endpoints) {
        for (Endpoint endpoint : endpoints) {
            remove(endpoint);
        }
    }

    public HealthStatus getHealthStatus(Endpoint endpoint) {
        HealthCheck healthCheck = healthChecks.get(endpoint);
        return healthCheck != null ? healthCheck.getStatus() : HealthStatus.UNKNOWN;
    }

    public boolean hasHealthCheck(Endpoint endpoint) {
        return healthChecks.get(endpoint) != null;
    }

    public long getMaxWaitFor(Endpoint endpoint) {
        HealthCheck healthCheck = healthChecks.get(endpoint);
        return healthCheck != null ? healthCheck.getTimeout() : 0;
    }
}
