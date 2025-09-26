package redis.clients.jedis.mcf;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.RedisCredentials;
import redis.clients.jedis.SslOptions;

import redis.clients.jedis.Endpoint;

public class LagAwareStrategy implements HealthCheckStrategy {

  private static final Logger log = LoggerFactory.getLogger(LagAwareStrategy.class);

  private final Config config;
  private final RedisRestAPI redisRestAPI;
  private String bdbId;

  public LagAwareStrategy(Config config) {
    this.config = config;
    this.redisRestAPI = new RedisRestAPI(config.getRestEndpoint(), config.getCredentialsSupplier(),
        config.getTimeout(), config.getSslOptions());
  }

  @Override
  public int getInterval() {
    return config.interval;
  }

  @Override
  public int getTimeout() {
    return config.timeout;
  }

  @Override
  public int minConsecutiveSuccessCount() {
    return config.minConsecutiveSuccessCount;
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

  public static class Config extends HealthCheckStrategy.Config {

    public static final boolean EXTENDED_CHECK_DEFAULT = true;
    public static final Duration AVAILABILITY_LAG_TOLERANCE_DEFAULT = Duration.ofMillis(100);

    private final Endpoint restEndpoint;
    private final Supplier<RedisCredentials> credentialsSupplier;

    // SSL configuration for HTTPS connections to Redis Enterprise REST API
    private final SslOptions sslOptions;

    // Maximum acceptable lag in milliseconds (default: 100);
    private final Duration availability_lag_tolerance;

    // Enable extended lag checking (default: true - performs lag validation in addition to standard
    // datapath
    // validation )
    private final boolean extendedCheckEnabled;

    public Config(Endpoint restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
      this(builder(restEndpoint, credentialsSupplier).interval(1000).timeout(1000)
          .minConsecutiveSuccessCount(3)
          .availabilityLagTolerance(AVAILABILITY_LAG_TOLERANCE_DEFAULT)
          .extendedCheckEnabled(EXTENDED_CHECK_DEFAULT));
    }

    private Config(ConfigBuilder builder) {
      super(builder.interval, builder.timeout, builder.minConsecutiveSuccessCount);

      this.restEndpoint = builder.restEndpoint;
      this.credentialsSupplier = builder.credentialsSupplier;
      this.sslOptions = builder.sslOptions;
      this.availability_lag_tolerance = builder.availabilityLagTolerance;
      this.extendedCheckEnabled = builder.extendedCheckEnabled;
    }

    public Endpoint getRestEndpoint() {
      return restEndpoint;
    }

    public Supplier<RedisCredentials> getCredentialsSupplier() {
      return credentialsSupplier;
    }

    public SslOptions getSslOptions() {
      return sslOptions;
    }

    public Duration getAvailabilityLagTolerance() {
      return availability_lag_tolerance;
    }

    public boolean isExtendedCheckEnabled() {
      return extendedCheckEnabled;
    }

    /**
     * Create a new builder for LagAwareStrategy.Config.
     * @param restEndpoint the Redis Enterprise REST API endpoint
     * @param credentialsSupplier the credentials supplier
     * @return a new ConfigBuilder instance
     */
    public static ConfigBuilder builder(Endpoint restEndpoint,
        Supplier<RedisCredentials> credentialsSupplier) {
      return new ConfigBuilder(restEndpoint, credentialsSupplier);
    }

    /**
     * Create a new Config instance with default values.
     * <p>
     * Extended checks like lag validation is enabled by default. With a default lag tolerance of
     * 100ms. To perform only standard datapath validation, use
     * {@link #databaseAvailability(Endpoint, Supplier)}. To configure a custom lag tolerance, use
     * {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
     * </p>
     */
    public static Config create(Endpoint restEndpoint,
        Supplier<RedisCredentials> credentialsSupplier) {
      return new ConfigBuilder(restEndpoint, credentialsSupplier).build();
    }

    /**
     * Perform standard datapath validation only.
     * <p>
     * Extended checks like lag validation is disabled by default. To enable extended checks, use
     * {@link #lagAware(Endpoint, Supplier)} or
     * {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
     * </p>
     */
    public static Config databaseAvailability(Endpoint restEndpoint,
        Supplier<RedisCredentials> credentialsSupplier) {
      return new ConfigBuilder(restEndpoint, credentialsSupplier).extendedCheckEnabled(false)
          .build();
    }

    /**
     * Perform standard datapath validation and lag validation using the default lag tolerance.
     * <p>
     * To configure a custom lag tolerance, use
     * {@link #lagAwareWithTolerance(Endpoint, Supplier, Duration)}
     * </p>
     */
    public static Config lagAware(Endpoint restEndpoint,
        Supplier<RedisCredentials> credentialsSupplier) {
      return new ConfigBuilder(restEndpoint, credentialsSupplier).extendedCheckEnabled(true)
          .build();
    }

    /**
     * Perform standard datapath validation and lag validation using the specified lag tolerance.
     */
    public static Config lagAwareWithTolerance(Endpoint restEndpoint,
        Supplier<RedisCredentials> credentialsSupplier, Duration availabilityLagTolerance) {
      return new ConfigBuilder(restEndpoint, credentialsSupplier).extendedCheckEnabled(true)
          .availabilityLagTolerance(availabilityLagTolerance).build();
    }

    /**
     * Builder for LagAwareStrategy.Config.
     */
    public static class ConfigBuilder
        extends HealthCheckStrategy.Config.Builder<ConfigBuilder, Config> {
      private final Endpoint restEndpoint;
      private final Supplier<RedisCredentials> credentialsSupplier;

      // SSL configuration for HTTPS connections
      private SslOptions sslOptions;

      // Maximum acceptable lag in milliseconds (default: 100);
      private Duration availabilityLagTolerance = AVAILABILITY_LAG_TOLERANCE_DEFAULT;

      // Enable extended lag checking
      private boolean extendedCheckEnabled = EXTENDED_CHECK_DEFAULT;

      private ConfigBuilder(Endpoint restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
        this.restEndpoint = restEndpoint;
        this.credentialsSupplier = credentialsSupplier;
      }

      /**
       * Set SSL options for HTTPS connections to Redis Enterprise REST API. This allows
       * configuration of custom truststore, keystore, and SSL parameters for secure connections.
       * @param sslOptions the SSL configuration options
       * @return this builder
       */
      public ConfigBuilder sslOptions(SslOptions sslOptions) {
        this.sslOptions = sslOptions;
        return this;
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
       * Enable extended lag checking. When enabled, performs lag validation in addition to standard
       * datapath validation. When disabled performs only standard datapath validation - all slots
       * are available.
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
}
