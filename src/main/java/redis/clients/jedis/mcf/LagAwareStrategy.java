package redis.clients.jedis.mcf;

import java.io.IOException;
import java.util.List;

public class LagAwareStrategy implements HealthCheckStrategy {

    private int interval;
    private int timeout;

    public LagAwareStrategy(int healthCheckInterval, int healthCheckTimeout) {
        this.interval = healthCheckInterval;
        this.timeout = healthCheckTimeout;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        RedisRestAPIHelper helper = new RedisRestAPIHelper(endpoint.getHost(), String.valueOf(endpoint.getPort()),
            "admin", "admin");
        try {
            List<String> bdbs = helper.getBdbs();
            if (bdbs.size() > 0) {
                if ("available".equals(helper.checkBdbAvailability(bdbs.get(0)))) {
                    return HealthStatus.HEALTHY;
                }
            }
        } catch (IOException e) {
            // log error
            return HealthStatus.UNHEALTHY;
        }
        return HealthStatus.UNHEALTHY;
    }
}
