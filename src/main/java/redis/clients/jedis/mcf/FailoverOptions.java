package redis.clients.jedis.mcf;

import redis.clients.jedis.annots.Experimental;

/**
 * Configuration options for CircuitBreakerCommandExecutor
 */
@Experimental
public class FailoverOptions {
    private final boolean retryOnFailover;

    private FailoverOptions(Builder builder) {
        this.retryOnFailover = builder.retryOnFailover;
    }

    /**
     * Gets whether to retry failed commands during failover
     * @return true if retry is enabled, false otherwise
     */
    public boolean isRetryOnFailover() {
        return retryOnFailover;
    }

    /**
     * Creates a new builder with default options
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for FailoverOptions
     */
    public static class Builder {
        private boolean retryOnFailover = false;

        private Builder() {
        }

        /**
         * Sets whether to retry failed commands during failover
         * @param retry true to retry, false otherwise
         * @return this builder for method chaining
         */
        public Builder retryOnFailover(boolean retry) {
            this.retryOnFailover = retry;
            return this;
        }

        /**
         * Builds a new FailoverOptions instance with the configured options
         * @return a new FailoverOptions instance
         */
        public FailoverOptions build() {
            return new FailoverOptions(this);
        }
    }
}