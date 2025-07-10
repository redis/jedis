package redis.clients.jedis.mcf;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
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

    private FailoverOptions(Builder builder) {
        this.retryOnFailover = builder.retryOnFailover;
        if (builder.healthCheckStrategySupplier != null) {
            this.healthCheckStrategySupplier = builder.healthCheckStrategySupplier;
        } else {
            this.healthCheckStrategySupplier = builder.enableHealthCheck ? EchoStrategy.DEFAULT : null;
        }
        this.weight = builder.weight;
        this.failback = builder.failback;

    }

    public static interface StrategySupplier {
        /**
         * Creates a HealthCheckStrategy for the given endpoint.
         * @param hostAndPort the endpoint to create a strategy for
         * @param jedisClientConfig the client configuration, may be null for implementations that don't need it
         * @return a HealthCheckStrategy instance
         */
        HealthCheckStrategy get(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig);
    }

    public StrategySupplier getStrategySupplier() {
        return healthCheckStrategySupplier;
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
        private boolean enableHealthCheck = false;

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
            if (healthCheckStrategySupplier == null) {
                throw new IllegalArgumentException("healthCheckStrategySupplier must not be null");
            }
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
            if (healthCheckStrategy == null) {
                throw new IllegalArgumentException("healthCheckStrategy must not be null");
            }
            this.healthCheckStrategySupplier = (hostAndPort, jedisClientConfig) -> healthCheckStrategy;
            return this;
        }

        public Builder enableHealthCheck(boolean enableHealthCheck) {
            this.enableHealthCheck = enableHealthCheck;
            if (!enableHealthCheck) {
                this.healthCheckStrategySupplier = null;
            }
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