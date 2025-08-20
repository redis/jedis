package redis.clients.jedis.mcf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.Endpoint;

public class HealthCheckCollection {

    private Map<Endpoint, HealthCheck> healthChecks = new ConcurrentHashMap<Endpoint, HealthCheck>();

    public HealthCheck add(HealthCheck healthCheck) {
        return healthChecks.put(healthCheck.getEndpoint(), healthCheck);
    }

    public HealthCheck[] addAll(HealthCheck[] healthChecks) {
        HealthCheck[] old = new HealthCheck[healthChecks.length];
        for (int i = 0; i < healthChecks.length; i++) {
            old[i] = add(healthChecks[i]);
        }
        return old;
    }

    public HealthCheck remove(Endpoint endpoint) {
        HealthCheck old = healthChecks.remove(endpoint);
        if (old != null) {
            old.stop();
        }
        return old;
    }

    public HealthCheck remove(HealthCheck healthCheck) {
        HealthCheck[] temp = new HealthCheck[1];
        healthChecks.computeIfPresent(healthCheck.getEndpoint(), (key, existing) -> {
            if (existing == healthCheck) {
                temp[0] = existing;
                return null;
            }
            return existing;
        });
        return temp[0];
    }

    public HealthCheck get(Endpoint endpoint) {
        return healthChecks.get(endpoint);
    }

    public void close() {
        for (HealthCheck healthCheck : healthChecks.values()) {
            healthCheck.stop();
        }
    }

}
