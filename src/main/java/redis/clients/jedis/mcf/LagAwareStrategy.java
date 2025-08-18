package redis.clients.jedis.mcf;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.RedisCredentials;

public class LagAwareStrategy implements HealthCheckStrategy {

    public static class Config extends HealthCheckStrategy.Config {

        private final Endpoint endpoint;
        private final Supplier<RedisCredentials> credentialsSupplier;

        // Maximum acceptable lag in milliseconds (default: 100);
        private final long availability_lag_tolerance;

        public Config(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            this(endpoint, credentialsSupplier, 1000, 1000, 3, 100);
        }

        private Config(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier, int interval, int timeout,
            int minConsecutiveSuccessCount, long availabilityLagTolerance) {
            super(interval, timeout, minConsecutiveSuccessCount);

            this.endpoint = endpoint;
            this.credentialsSupplier = credentialsSupplier;
            this.availability_lag_tolerance = availabilityLagTolerance;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public Supplier<RedisCredentials> getCredentialsSupplier() {
            return credentialsSupplier;
        }

        public long getAvailabilityLagTolerance() {
            return availability_lag_tolerance;
        }

        /**
         * Create a new builder for LagAwareStrategy.Config.
         * @param endpoint the Redis Enterprise endpoint
         * @param credentialsSupplier the credentials supplier
         * @return a new ConfigBuilder instance
         */
        public static ConfigBuilder builder(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return new ConfigBuilder(endpoint, credentialsSupplier);
        }

        /**
         * Builder for LagAwareStrategy.Config.
         */
        public static class ConfigBuilder extends HealthCheckStrategy.Config.Builder<ConfigBuilder, Config> {
            private final Endpoint endpoint;
            private final Supplier<RedisCredentials> credentialsSupplier;

            // Maximum acceptable lag in milliseconds (default: 100);
            private long availabilityLagTolerance = 100;

            private ConfigBuilder(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
                this.endpoint = endpoint;
                this.credentialsSupplier = credentialsSupplier;
            }

            /**
             * Set the maximum acceptable lag in milliseconds.
             * @param availabilityLagTolerance the lag tolerance in milliseconds (default: 100)
             * @return this builder
             */
            public ConfigBuilder availabilityLagTolerance(long availabilityLagTolerance) {
                this.availabilityLagTolerance = availabilityLagTolerance;
                return this;
            }

            /**
             * Build the Config instance.
             * @return a new Config instance
             */
            @Override
            public Config build() {
                return new Config(endpoint, credentialsSupplier, interval, timeout, minConsecutiveSuccessCount,
                    availabilityLagTolerance);
            }
        }

    }

    private static Logger log = LoggerFactory.getLogger(LagAwareStrategy.class);

    private final int interval;
    private final int timeout;
    private final int minConsecutiveSuccessCount;
    private final RedisRestAPI redisRestAPI;
    private String bdbId;

    public LagAwareStrategy(Config config) {
        this.interval = config.interval;
        this.timeout = config.timeout;
        this.minConsecutiveSuccessCount = config.minConsecutiveSuccessCount;
        this.redisRestAPI = new RedisRestAPI(config.endpoint, config.credentialsSupplier, config.timeout);
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
    public int minConsecutiveSuccessCount() {
        return minConsecutiveSuccessCount;
    }

    @Override
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        try {
            String bdb = bdbId;
            if (bdb == null) {
                // Try to find BDB that matches the database host
                String dbHost = endpoint.getHost();
                List<RedisRestAPI.BdbInfo> bdbs = redisRestAPI.getBdbs();
                RedisRestAPI.BdbInfo matchingBdb = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, dbHost);

                if (matchingBdb == null) {
                    log.warn("No BDB found matching host '{}' for health check", dbHost);
                    return HealthStatus.UNHEALTHY;
                } else {
                    bdb = matchingBdb.getUid();
                    log.debug("Found matching BDB '{}' for host '{}'", bdb, dbHost);
                    bdbId = bdb;
                }
            }
            if (redisRestAPI.checkBdbAvailability(bdb, true)) {
                return HealthStatus.HEALTHY;
            }
        } catch (Exception e) {
            log.error("Error while checking database availability", e);
            bdbId = null;
        }
        return HealthStatus.UNHEALTHY;
    }

    public static HealthCheckStrategy getDefault(Endpoint endpoint, RedisCredentials credentials) {
        return new LagAwareStrategy(new Config(endpoint, () -> credentials));
    }

    public static HealthCheckStrategy getDefault(Endpoint endpoint, Supplier<RedisCredentials> credentialSupplier) {
        return new LagAwareStrategy(new Config(endpoint, credentialSupplier));
    }

    public static final StrategySupplier<Config> DEFAULT = LagAwareStrategy::new;
}
