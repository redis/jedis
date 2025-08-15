
package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public interface HealthCheck {

    Endpoint getEndpoint();

    HealthStatus getStatus();

    void stop();

    void start();

    long getTimeout();

}
