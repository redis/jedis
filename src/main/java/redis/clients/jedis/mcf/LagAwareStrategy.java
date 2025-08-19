package redis.clients.jedis.mcf;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.RedisCredentials;

public class LagAwareStrategy implements HealthCheckStrategy {

    public static class Config extends HealthCheckStrategy.Config {

        public static final boolean EXTENDED_CHECK_DEFAULT = true;
        public static final Duration AVAILABILITY_LAG_TOLERANCE_DEFAULT = Duration.ofMillis(100);

        private final Endpoint endpoint;
        private final Supplier<RedisCredentials> credentialsSupplier;

        // Maximum acceptable lag in milliseconds (default: 100);
        private final Duration availability_lag_tolerance;

        // Enable extended lag checking (default: true - performs standard datapath validation and lag validation)
        private final boolean extendedCheckEnabled;

        public Config(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            this(builder(endpoint, credentialsSupplier).interval(1000).timeout(1000).minConsecutiveSuccessCount(3)
                .availabilityLagTolerance(AVAILABILITY_LAG_TOLERANCE_DEFAULT)
                .extendedCheckEnabled(EXTENDED_CHECK_DEFAULT));
        }

        private Config(ConfigBuilder builder) {
            super(builder.interval, builder.timeout, builder.minConsecutiveSuccessCount);

            this.endpoint = builder.endpoint;
            this.credentialsSupplier = builder.credentialsSupplier;
            this.availability_lag_tolerance = builder.availabilityLagTolerance;
            this.extendedCheckEnabled = builder.extendedCheckEnabled;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public Supplier<RedisCredentials> getCredentialsSupplier() {
            return credentialsSupplier;
        }

        public Duration getAvailabilityLagTolerance() {
            return availability_lag_tolerance;
        }

        public boolean isExtendedCheckEnabled() {
            return extendedCheckEnabled;
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
         * Create a new Config instance with default values.
         * <p>
         * Extended checks like lag validation is enabled by default. With a default lag tolerance of 100ms. To perform
         * only standard datapath validation, use {@link #standard(Endpoint, Supplier)}. To configure
         * a custom lag tolerance, use {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
         * </p>
         */
        public static Config create(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return new ConfigBuilder(endpoint, credentialsSupplier).build();
        }

        /**
         * Perform standard datapath validation only.
         * <p>
         * Extended checks like lag validation is disabled by default. To enable extended checks, use
         * {@link #lagAware(Endpoint, Supplier)} or
         * {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
         * </p>
         */
        public static Config standard(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return new ConfigBuilder(endpoint, credentialsSupplier).extendedCheckEnabled(EXTENDED_CHECK_DEFAULT)
                .build();
        }

        /**
         * Perform standard datapath validation and lag validation using the default lag tolerance.
         * <p>
         * To configure a custom lag tolerance, use {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
         * </p>
         */
        public static Config lagAware(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return new ConfigBuilder(endpoint, credentialsSupplier).extendedCheckEnabled(true).build();
        }

        /**
         * Perform standard datapath validation and lag validation using the specified lag tolerance.
         */
        public static Config lagAwareWithTolerance(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier,
            Duration availabilityLagTolerance) {
            return new ConfigBuilder(endpoint, credentialsSupplier).extendedCheckEnabled(true)
                .availabilityLagTolerance(availabilityLagTolerance).build();
        }

        /**
         * Builder for LagAwareStrategy.Config.
         */
        public static class ConfigBuilder extends HealthCheckStrategy.Config.Builder<ConfigBuilder, Config> {
            private final Endpoint endpoint;
            private final Supplier<RedisCredentials> credentialsSupplier;

            // Maximum acceptable lag in milliseconds (default: 100);
            private Duration availabilityLagTolerance = AVAILABILITY_LAG_TOLERANCE_DEFAULT;

            // Enable extended lag checking (default: false - performs only standard datapath validation)
            private boolean extendedCheckEnabled = EXTENDED_CHECK_DEFAULT;

            private ConfigBuilder(Endpoint endpoint, Supplier<RedisCredentials> credentialsSupplier) {
                this.endpoint = endpoint;
                this.credentialsSupplier = credentialsSupplier;
            }

            /**
             * Set the maximum acceptable lag in milliseconds.
             * @param availabilityLagTolerance the lag tolerance in milliseconds (default: 100)
             * @return this builder
             */
            public ConfigBuilder availabilityLagTolerance(Duration availabilityLagTolerance) {
                this.availabilityLagTolerance = availabilityLagTolerance;
                return this;
            }

            /**
             * Enable extended lag checking. When enabled, performs lag validation in addition to standard datapath
             * validation. When disabled performs only standard datapath validation - all slots are available.
             * @param extendedCheckEnabled true to enable extended lag checking (default: false)
             * @return this builder
             */
            public ConfigBuilder extendedCheckEnabled(boolean extendedCheckEnabled) {
                this.extendedCheckEnabled = extendedCheckEnabled;
                return this;
            }

            /**
             * Build the Config instance.
             * @return a new Config instance
             */
            @Override
            public Config build() {
                return new Config(this);
            }
        }

    }

    private static final Logger log = LoggerFactory.getLogger(LagAwareStrategy.class);

    private final Config config;
    private final int interval;
    private final int timeout;
    private final int minConsecutiveSuccessCount;
    private final RedisRestAPI redisRestAPI;
    private String bdbId;

    public LagAwareStrategy(Config config) {
        this.config = config;
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
            if (this.config.isExtendedCheckEnabled()) {
                // Use extended check with lag validation
                if (redisRestAPI.checkBdbAvailability(bdb, true,
                    this.config.getAvailabilityLagTolerance().toMillis())) {
                    return HealthStatus.HEALTHY;
                }
            } else {
                // Use standard datapath validation only
                if (redisRestAPI.checkBdbAvailability(bdb, false)) {
                    return HealthStatus.HEALTHY;
                }
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
