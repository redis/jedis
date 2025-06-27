package redis.clients.jedis.mcf;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.annots.Experimental;

/**
 * Configuration options for CircuitBreakerCommandExecutor
 */
@Experimental
public class FailoverOptions {
    private final boolean retryOnFailover;
    private final StrategySupplier healthCheckStrategySupplier;
    private final float weight;
    private boolean failback;

    private static StrategySupplier defaultStrategySupplier = (endpoint) -> new NoOpStrategy();

    private FailoverOptions(Builder builder) {
        this.retryOnFailover = builder.retryOnFailover;
        this.healthCheckStrategySupplier = builder.healthCheckStrategySupplier == null ? defaultStrategySupplier
            : builder.healthCheckStrategySupplier;
        this.weight = builder.weight;
        this.failback = builder.failback;
    }

    public static interface StrategySupplier {
        HealthCheckStrategy get(HostAndPort hostAndPort);
    }

    public HealthCheckStrategy getFailoverHealthCheckStrategy(HostAndPort hostAndPort) {
        return healthCheckStrategySupplier.get(hostAndPort);
    }

    public float getWeight() {
        return weight;
    }

    public boolean isFailbackEnabled() {
        return failback;
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
        private StrategySupplier healthCheckStrategySupplier;
        private float weight = 1.0f;
        private boolean failback;

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

        public Builder healthCheckStrategySupplier(StrategySupplier healthCheckStrategySupplier) {
            this.healthCheckStrategySupplier = healthCheckStrategySupplier;
            return this;
        }

        public Builder weight(float weight) {
            this.weight = weight;
            return this;
        }

        public Builder failback(boolean failbackEnabled) {
            this.failback = failbackEnabled;
            return this;
        }

        public Builder healthCheckStrategy(HealthCheckStrategy healthCheckStrategy) {
            this.healthCheckStrategySupplier = (hostAndPort) -> healthCheckStrategy;
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