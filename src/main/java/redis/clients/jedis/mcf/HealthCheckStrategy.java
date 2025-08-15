package redis.clients.jedis.mcf;

import java.io.Closeable;

public interface HealthCheckStrategy extends Closeable {

    /**
     * Get the interval (in milliseconds) between health checks.
     * @return the interval in milliseconds
     */
    int getInterval();

    /**
     * Get the timeout (in milliseconds) for a health check.
     * @return the timeout in milliseconds
     */
    int getTimeout();

    /**
     * Perform the health check for the given endpoint.
     * @param endpoint the endpoint to check
     * @return the health status
     */
    HealthStatus doHealthCheck(Endpoint endpoint);

    /**
     * Close any resources used by the health check strategy.
     */
    default void close() {
    }

    /**
     * Get the minimum number of consecutive successful health checks required to mark the endpoint as healthy.
     * @return the minimum number of consecutive successful health checks
     */
    default int minConsecutiveSuccessCount() {
        return 1;
    }
}
