package redis.clients.jedis.mcf;

import java.util.function.Function;

public class EchoStrategy implements HealthCheckStrategy {

    private int interval;
    private int timeout;
    private Function<Endpoint, String> echo;

    public EchoStrategy(int healthCheckInterval, int healthCheckTimeout, Function<Endpoint, String> echo) {
        this.interval = healthCheckInterval;
        this.timeout = healthCheckTimeout;
        this.echo = echo;
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
        return "OK".equals(echo.apply(endpoint)) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
    }

}
