package redis.clients.jedis.mcf;

import java.io.Closeable;

public interface HealthCheckStrategy extends Closeable {

    int getInterval();

    int getTimeout();

    HealthStatus doHealthCheck(Endpoint endpoint);

    default void close() {
    }

}
