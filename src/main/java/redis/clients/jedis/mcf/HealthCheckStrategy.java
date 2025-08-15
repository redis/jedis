package redis.clients.jedis.mcf;

import java.io.Closeable;

public interface HealthCheckStrategy extends Closeable {

    public static class Config {
        protected final int interval;
        protected final int timeout;
        protected final int minConsecutiveSuccessCount;

        public Config(int interval, int timeout, int minConsecutiveSuccessCount) {
            this.interval = interval;
            this.timeout = timeout;
            this.minConsecutiveSuccessCount = minConsecutiveSuccessCount;
        }
    }

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

    /**
     * Get the maximum wait duration (in milliseconds) for the endpoint to enter into a stable state.
     * @return the maximum wait duration in milliseconds
     */
    default int getMaxWaitDuration() {
        // Add one to account for the initial check
        return ((minConsecutiveSuccessCount() + 1) * (getInterval() + getTimeout()));
    }

}
