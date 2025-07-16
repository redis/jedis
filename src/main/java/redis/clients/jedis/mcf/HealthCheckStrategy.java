package redis.clients.jedis.mcf;

public interface HealthCheckStrategy {

    int getInterval();

    int getTimeout();

    HealthStatus doHealthCheck(Endpoint endpoint);

}
