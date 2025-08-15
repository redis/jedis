
package redis.clients.jedis.mcf;

public interface HealthCheck {

    Endpoint getEndpoint();

    HealthStatus getStatus();

    void stop();

    void start();

    long getTimeout();

}
