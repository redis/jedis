package redis.clients.jedis;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.ConnectionFailoverException;
import redis.clients.jedis.mcf.PingStrategy;
import redis.clients.jedis.util.JedisAsserts;
import redis.clients.jedis.mcf.HealthCheckStrategy;
import redis.clients.jedis.mcf.InitializationPolicy;

/**
 * Configuration class for multi-database Redis deployments with automatic failover and failback
 * capabilities.
 * <p>
 * This configuration enables seamless failover between multiple Redis databases endpoints by
 * providing comprehensive settings for retry logic, circuit breaker behavior, health checks, and
 * failback mechanisms. It is designed to work with
 * {@link redis.clients.jedis.mcf.MultiDbConnectionProvider} to provide high availability and
 * disaster recovery capabilities.
 * </p>
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li><strong>Multi-Database Support:</strong> Configure multiple Redis endpoints with individual
 * weights and health checks</li>
 * <li><strong>Circuit Breaker Pattern:</strong> Automatic failure detection and circuit opening
 * based on configurable thresholds</li>
 * <li><strong>Retry Logic:</strong> Configurable retry attempts with exponential backoff for
 * transient failures</li>
 * <li><strong>Health Check Integration:</strong> Pluggable health check strategies for proactive
 * monitoring</li>
 * <li><strong>Automatic Failback:</strong> Intelligent failback to higher-priority databases when
 * they recover</li>
 * <li><strong>Weight-Based Routing:</strong> Priority-based database selection using configurable
 * weights</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 *
 * <pre>
 * {
 *   &#64;code
 *   // Configure individual databases
 *   DatabaseConfig primary = DatabaseConfig.builder(primaryEndpoint, clientConfig).weight(1.0f)
 *       .build();
 *
 *   DatabaseConfig secondary = DatabaseConfig.builder(secondaryEndpoint, clientConfig).weight(0.5f)
 *       .healthCheckEnabled(true).build();
 *
 *   // Build multi-database configuration
 *   MultiDbConfig config = MultiDbConfig.builder(primary, secondary)
 *       .failureDetector(CircuitBreakerConfig.builder().failureRateThreshold(10.0f).build())
 *       .commandRetry(RetryConfig.builder().maxAttempts(3).build()).failbackSupported(true)
 *       .gracePeriod(10000).build();
 *
 *   // Use with connection provider
 *   MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config);
 * }
 * </pre>
 * <p>
 * The configuration leverages <a href="https://resilience4j.readme.io/docs">Resilience4j</a> for
 * circuit breaker and retry implementations, providing battle-tested fault tolerance patterns.
 * </p>
 * @see redis.clients.jedis.mcf.MultiDbConnectionProvider
 * @see redis.clients.jedis.mcf.HealthCheckStrategy
 * @see redis.clients.jedis.mcf.PingStrategy
 * @see redis.clients.jedis.mcf.LagAwareStrategy
 * @since 7.0
 */
// TODO: move
@Experimental
public final class MultiDbConfig {

  /**
   * Functional interface for creating {@link HealthCheckStrategy} instances for specific Redis
   * endpoints.
   * <p>
   * This supplier pattern allows for flexible health check strategy creation, enabling different
   * strategies for different endpoints or dynamic configuration based on endpoint characteristics.
   * </p>
   * <p>
   * <strong>Common Implementations:</strong>
   * </p>
   * <ul>
   * <li>{@link redis.clients.jedis.mcf.PingStrategy#DEFAULT} - Uses Redis PING command for health
   * checks</li>
   * <li>Custom implementations for specific monitoring requirements</li>
   * <li>Redis Enterprise implementations using REST API monitoring</li>
   * </ul>
   * @see redis.clients.jedis.mcf.HealthCheckStrategy
   * @see redis.clients.jedis.mcf.PingStrategy
   * @see redis.clients.jedis.mcf.LagAwareStrategy
   */
  public static interface StrategySupplier {
    /**
     * Creates a {@link HealthCheckStrategy} instance for the specified Redis endpoint.
     * @param hostAndPort the Redis endpoint (host and port) to create a health check strategy for
     * @param jedisClientConfig the Jedis client configuration containing connection settings,
     *          authentication, and other client parameters. May be null for implementations that
     *          don't require client configuration
     * @return a configured {@link HealthCheckStrategy} instance for monitoring the specified
     *         endpoint
     * @throws IllegalArgumentException if the hostAndPort is null or invalid
     */
    HealthCheckStrategy get(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig);
  }

  /**
   * Configuration for command retry behavior.
   * <p>
   * This class encapsulates all retry-related settings including maximum attempts, wait duration,
   * exponential backoff, and exception handling. It provides a clean separation of retry concerns
   * from other configuration aspects.
   * </p>
   * @since 7.0
   */
  public static final class RetryConfig {

    private final int maxAttempts;
    private final Duration waitDuration;
    private final int exponentialBackoffMultiplier;
    private final List<Class> includedExceptionList;
    private final List<Class> ignoreExceptionList;

    private RetryConfig(Builder builder) {
      this.maxAttempts = builder.maxAttempts;
      this.waitDuration = Duration.ofMillis(builder.waitDuration);
      this.exponentialBackoffMultiplier = builder.exponentialBackoffMultiplier;
      this.includedExceptionList = builder.includedExceptionList;
      this.ignoreExceptionList = builder.ignoreExceptionList;
    }

    /**
     * Returns the maximum number of retry attempts including the initial call.
     * @return maximum retry attempts
     */
    public int getMaxAttempts() {
      return maxAttempts;
    }

    /**
     * Returns the base wait duration between retry attempts.
     * @return wait duration between retries
     */
    public Duration getWaitDuration() {
      return waitDuration;
    }

    /**
     * Returns the exponential backoff multiplier for retry wait duration.
     * @return exponential backoff multiplier
     */
    public int getExponentialBackoffMultiplier() {
      return exponentialBackoffMultiplier;
    }

    /**
     * Returns the list of exception classes that trigger retry attempts.
     * @return list of exception classes that are retried, never null
     */
    public List<Class> getIncludedExceptionList() {
      return includedExceptionList;
    }

    /**
     * Returns the list of exception classes that are ignored for retry purposes.
     * @return list of exception classes to ignore for retries, may be null
     */
    public List<Class> getIgnoreExceptionList() {
      return ignoreExceptionList;
    }

    /**
     * Creates a new Builder instance for configuring RetryConfig.
     * @return new Builder instance with default values
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * Builder for {@link RetryConfig}.
     */
    public static final class Builder {

      private int maxAttempts = RETRY_MAX_ATTEMPTS_DEFAULT;
      private int waitDuration = RETRY_WAIT_DURATION_DEFAULT;
      private int exponentialBackoffMultiplier = RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT;
      private List<Class> includedExceptionList = RETRY_INCLUDED_EXCEPTIONS_DEFAULT;
      private List<Class> ignoreExceptionList = null;

      /**
       * Sets the maximum number of retry attempts including the initial call.
       * @param maxAttempts maximum number of attempts (must be &gt;= 1)
       * @return this builder instance for method chaining
       */
      public Builder maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
      }

      /**
       * Sets the base wait duration between retry attempts in milliseconds.
       * @param waitDuration wait duration in milliseconds (must be &gt;= 0)
       * @return this builder instance for method chaining
       */
      public Builder waitDuration(int waitDuration) {
        this.waitDuration = waitDuration;
        return this;
      }

      /**
       * Sets the exponential backoff multiplier for retry wait duration.
       * @param exponentialBackoffMultiplier exponential backoff multiplier (must be &gt;= 1)
       * @return this builder instance for method chaining
       */
      public Builder exponentialBackoffMultiplier(int exponentialBackoffMultiplier) {
        this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
        return this;
      }

      /**
       * Sets the list of exception classes that trigger retry attempts.
       * @param includedExceptionList list of exception classes that should be retried
       * @return this builder instance for method chaining
       */
      public Builder includedExceptionList(List<Class> includedExceptionList) {
        this.includedExceptionList = includedExceptionList;
        return this;
      }

      /**
       * Sets the list of exception classes that are ignored for retry purposes.
       * @param ignoreExceptionList list of exception classes to ignore for retries
       * @return this builder instance for method chaining
       */
      public Builder ignoreExceptionList(List<Class> ignoreExceptionList) {
        this.ignoreExceptionList = ignoreExceptionList;
        return this;
      }

      /**
       * Builds and returns a new RetryConfig instance.
       * @return new RetryConfig instance with configured settings
       */
      public RetryConfig build() {
        return new RetryConfig(this);
      }
    }
  }

  /**
   * Configuration for circuit breaker failure detection.
   * <p>
   * This class encapsulates all circuit breaker-related settings including failure rate threshold,
   * sliding window size, minimum failures, and exception handling.
   * </p>
   * @since 7.0
   */
  public static final class CircuitBreakerConfig {

    private final float failureRateThreshold;
    private final int slidingWindowSize;
    private final int minNumOfFailures;
    private final List<Class> includedExceptionList;
    private final List<Class> ignoreExceptionList;

    private CircuitBreakerConfig(Builder builder) {
      this.failureRateThreshold = builder.failureRateThreshold;
      this.slidingWindowSize = builder.slidingWindowSize;
      this.minNumOfFailures = builder.minNumOfFailures;
      this.includedExceptionList = builder.includedExceptionList;
      this.ignoreExceptionList = builder.ignoreExceptionList;
    }

    /**
     * Returns the failure rate threshold percentage for circuit breaker activation.
     * <p>
     * 0.0f means failure rate is ignored, and only minimum number of failures is considered.
     * </p>
     * <p>
     * When the failure rate exceeds both this threshold and the minimum number of failures, the
     * circuit breaker transitions to the OPEN state and starts short-circuiting calls, immediately
     * failing them without attempting to reach the Redis database. This prevents cascading failures
     * and allows the system to fail over to the next available database.
     * </p>
     * <p>
     * <strong>Range:</strong> 0.0 to 100.0 (percentage)
     * </p>
     * @return failure rate threshold as a percentage (0.0 to 100.0)
     * @see #getMinNumOfFailures()
     */
    public float getFailureRateThreshold() {
      return failureRateThreshold;
    }

    /**
     * Returns the size of the sliding window used to record call outcomes when the circuit breaker
     * is CLOSED.
     * <p>
     * <strong>Default:</strong> {@value #CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT}
     * </p>
     * @return sliding window size (calls or seconds depending on window type)
     */
    public int getSlidingWindowSize() {
      return slidingWindowSize;
    }

    /**
     * Returns the minimum number of failures before circuit breaker is tripped.
     * <p>
     * 0 means minimum number of failures is ignored, and only failure rate is considered.
     * </p>
     * <p>
     * When the number of failures exceeds both this threshold and the failure rate threshold, the
     * circuit breaker will trip and prevent further requests from being sent to the database until
     * it has recovered.
     * </p>
     * @return minimum number of failures before circuit breaker is tripped
     * @see #getFailureRateThreshold()
     */
    public int getMinNumOfFailures() {
      return minNumOfFailures;
    }

    /**
     * Returns the list of exception classes that are recorded as circuit breaker failures and
     * increase the failure rate.
     * <p>
     * Any exception that matches or inherits from the classes in this list counts as a failure for
     * circuit breaker calculations, unless explicitly ignored via
     * {@link #getIgnoreExceptionList()}. If you specify this list, all other exceptions count as
     * successes unless they are explicitly ignored.
     * </p>
     * <p>
     * <strong>Default:</strong> {@link JedisConnectionException}
     * </p>
     * @return list of exception classes that count as failures, never null
     * @see #getIgnoreExceptionList()
     */
    public List<Class> getIncludedExceptionList() {
      return includedExceptionList;
    }

    /**
     * Returns the list of exception classes that are ignored by the circuit breaker and neither
     * count as failures nor successes.
     * <p>
     * Any exception that matches or inherits from the classes in this list will not affect circuit
     * breaker failure rate calculations, even if the exception is included in
     * {@link #getIncludedExceptionList()}.
     * </p>
     * <p>
     * <strong>Default:</strong> null (no exceptions ignored)
     * </p>
     * @return list of exception classes to ignore for circuit breaker calculations, may be null
     * @see #getIncludedExceptionList()
     */
    public List<Class> getIgnoreExceptionList() {
      return ignoreExceptionList;
    }

    /**
     * Creates a new Builder instance for configuring CircuitBreakerConfig.
     * @return new Builder instance with default values
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * Builder for {@link CircuitBreakerConfig}.
     */
    public static final class Builder {

      private float failureRateThreshold = CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT;
      private int slidingWindowSize = CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT;
      private int minNumOfFailures = CIRCUITBREAKER_THRESHOLD_MIN_NUM_OF_FAILURES_DEFAULT;
      private List<Class> includedExceptionList = CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT;
      private List<Class> ignoreExceptionList = null;

      /**
       * Sets the failure rate threshold percentage that triggers circuit breaker activation.
       * @param failureRateThreshold failure rate threshold as percentage (0.0 to 100.0)
       * @return this builder instance for method chaining
       */
      public Builder failureRateThreshold(float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
        return this;
      }

      /**
       * Sets the size of the sliding window for circuit breaker calculations.
       * @param slidingWindowSize sliding window size
       * @return this builder instance for method chaining
       */
      public Builder slidingWindowSize(int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
        return this;
      }

      /**
       * Sets the minimum number of failures before circuit breaker is tripped.
       * @param minNumOfFailures minimum number of failures
       * @return this builder instance for method chaining
       */
      public Builder minNumOfFailures(int minNumOfFailures) {
        this.minNumOfFailures = minNumOfFailures;
        return this;
      }

      /**
       * Sets the list of exception classes that are recorded as circuit breaker failures.
       * @param includedExceptionList list of exception classes that count as failures
       * @return this builder instance for method chaining
       */
      public Builder includedExceptionList(List<Class> includedExceptionList) {
        this.includedExceptionList = includedExceptionList;
        return this;
      }

      /**
       * Sets the list of exception classes that are ignored by the circuit breaker.
       * @param ignoreExceptionList list of exception classes to ignore
       * @return this builder instance for method chaining
       */
      public Builder ignoreExceptionList(List<Class> ignoreExceptionList) {
        this.ignoreExceptionList = ignoreExceptionList;
        return this;
      }

      /**
       * Builds and returns a new CircuitBreakerConfig instance.
       * @return new CircuitBreakerConfig instance with configured settings
       */
      public CircuitBreakerConfig build() {
        return new CircuitBreakerConfig(this);
      }
    }
  }

  // ============ Default Configuration Constants ============

  /** Default maximum number of retry attempts including the initial call. */
  private static final int RETRY_MAX_ATTEMPTS_DEFAULT = 3;

  /** Default wait duration between retry attempts in milliseconds. */
  private static final int RETRY_WAIT_DURATION_DEFAULT = 500;

  /** Default exponential backoff multiplier for retry wait duration. */
  private static final int RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT = 2;

  /** Default list of exceptions that trigger retry attempts. */
  private static final List<Class> RETRY_INCLUDED_EXCEPTIONS_DEFAULT = Arrays
      .asList(JedisConnectionException.class);

  /** Default failure rate threshold percentage for circuit breaker activation. */
  private static final float CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT = 10.0f;

  /** Minimum number of failures before circuit breaker is tripped. */
  private static final int CIRCUITBREAKER_THRESHOLD_MIN_NUM_OF_FAILURES_DEFAULT = 1000;

  /** Default sliding window size for circuit breaker failure tracking. */
  private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT = 2;

  /** Default list of exceptions that are recorded as circuit breaker failures. */
  private static final List<Class> CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT = Arrays
      .asList(JedisConnectionException.class);

  /** Default list of exceptions that trigger fallback to next available database. */
  private static final List<Class<? extends Throwable>> FALLBACK_EXCEPTIONS_DEFAULT = Arrays
      .asList(CallNotPermittedException.class, ConnectionFailoverException.class);

  /** Default interval in milliseconds for checking if failed databases have recovered. */
  private static final long FAILBACK_CHECK_INTERVAL_DEFAULT = 120000;

  /**
   * Default grace period in milliseconds to keep databases disabled after they become unhealthy.
   */
  private static final long GRACE_PERIOD_DEFAULT = 60000;

  /** Default maximum number of failover attempts. */
  private static final int MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT = 10;

  /** Default delay in milliseconds between failover attempts. */
  private static final int DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT = 12000;

  /** Array of database configurations defining the available Redis endpoints and their settings. */
  private final DatabaseConfig[] databaseConfigs;

  // ============ Retry Configuration ============
  // Based on Resilience4j Retry: https://resilience4j.readme.io/docs/retry

  /**
   * Encapsulated retry configuration for command execution.
   * <p>
   * This provides a cleaner API for configuring retry behavior by grouping all retry-related
   * settings into a single configuration object.
   * </p>
   * @see RetryConfig
   */
  private RetryConfig commandRetry;

  // ============ Circuit Breaker Configuration ============

  /**
   * Encapsulated circuit breaker configuration for failure detection.
   * <p>
   * This provides a cleaner API for configuring circuit breaker behavior by grouping all circuit
   * breaker-related settings into a single configuration object.
   * </p>
   * @see CircuitBreakerConfig
   */
  private CircuitBreakerConfig failureDetector;

  /**
   * List of exception classes that trigger fallback to the next available database.
   * <p>
   * When these exceptions occur, the system will attempt to failover to the next available database
   * based on weight priority. This enables immediate failover for specific error conditions without
   * waiting for circuit breaker thresholds.
   * </p>
   * <p>
   * <strong>Default:</strong> {@link CallNotPermittedException},
   * {@link ConnectionFailoverException}
   * </p>
   * @see #getFallbackExceptionList()
   */
  private List<Class<? extends Throwable>> fallbackExceptionList;

  // ============ Failover Configuration ============

  /**
   * Whether to retry failed commands during the failover process.
   * <p>
   * When enabled, commands that fail during failover will be retried according to the configured
   * retry settings. When disabled, failed commands during failover will immediately return the
   * failure to the caller.
   * </p>
   * <p>
   * <strong>Default:</strong> false
   * </p>
   * @see #isRetryOnFailover()
   * @see #commandRetry
   */
  private boolean retryOnFailover;

  /**
   * Whether automatic failback to higher-priority databases is supported.
   * <p>
   * When enabled, the system will automatically monitor failed databases using health checks and
   * failback to higher-priority (higher weight) databases when they recover. When disabled, manual
   * intervention is required to failback.
   * </p>
   * <p>
   * <strong>Default:</strong> true
   * </p>
   * @see #isFailbackSupported()
   * @see #failbackCheckInterval
   * @see #gracePeriod
   */
  private boolean isFailbackSupported;

  /**
   * Interval in milliseconds between checks for failback opportunities to recovered databases.
   * <p>
   * This setting controls how frequently the system checks if a higher-priority database has
   * recovered and is available for failback. Lower values provide faster failback but increase
   * monitoring overhead.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #FAILBACK_CHECK_INTERVAL_DEFAULT} milliseconds (5 seconds)
   * </p>
   * @see #getFailbackCheckInterval()
   * @see #isFailbackSupported
   * @see #gracePeriod
   */
  private long failbackCheckInterval;

  /**
   * Grace period in milliseconds to keep databases disabled after they become unhealthy.
   * <p>
   * After a database is marked as unhealthy, it remains disabled for this grace period before being
   * eligible for failback, even if health checks indicate recovery. This prevents rapid oscillation
   * between databases during intermittent failures.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #GRACE_PERIOD_DEFAULT} milliseconds (10 seconds)
   * </p>
   * @see #getGracePeriod()
   * @see #isFailbackSupported
   * @see #failbackCheckInterval
   */
  private long gracePeriod;

  /**
   * Whether to forcefully terminate connections during failover for faster database switching.
   * <p>
   * When enabled, existing connections to the failed database are immediately closed during
   * failover, potentially reducing failover time but may cause some in-flight operations to fail.
   * When disabled, connections are closed gracefully.
   * </p>
   * <p>
   * <strong>Default:</strong> false
   * </p>
   * @see #isFastFailover()
   */
  private boolean fastFailover;

  /**
   * Maximum number of failover attempts.
   * <p>
   * This setting controls how many times the system will attempt to failover to a different
   * database before giving up. For example, if set to 3, the system will make 1 initial attempt
   * plus 2 failover attempts for a total of 3 attempts.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT}
   * </p>
   * @see #getMaxNumFailoverAttempts()
   */
  private int maxNumFailoverAttempts;

  /**
   * Delay in milliseconds between failover attempts.
   * <p>
   * This setting controls how long the system will wait before attempting to failover to a
   * different database. For example, if set to 1000, the system will wait 1 second before
   * attempting to failover to a different database.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT} milliseconds
   * </p>
   * @see #getDelayInBetweenFailoverAttempts()
   */
  private int delayInBetweenFailoverAttempts;

  /**
   * Initialization policy that determines when the multi-database connection is ready to be
   * returned based on the availability of individual database connections.
   * <p>
   * The policy is evaluated based on the completion status of database health checks, and the
   * decision to continue waiting, succeed, or fail is based on the number of available, pending,
   * and failed connections.
   * </p>
   * <p>
   * <strong>Default:</strong> {@link InitializationPolicy.BuiltIn#ONE_AVAILABLE}
   * </p>
   * @see InitializationPolicy
   * @see #getInitializationPolicy()
   */
  private InitializationPolicy initializationPolicy;

  /**
   * Constructs a new MultiDbConfig with the specified database configurations.
   * <p>
   * This constructor validates that at least one database configuration is provided and that all
   * configurations are non-null. Use the {@link Builder} class for more convenient configuration
   * with default values.
   * </p>
   * @param databaseConfigs array of database configurations defining the available Redis endpoints
   * @throws JedisValidationException if databaseConfigs is null or empty
   * @throws IllegalArgumentException if any database configuration is null
   * @see Builder#Builder(DatabaseConfig[])
   */
  public MultiDbConfig(DatabaseConfig[] databaseConfigs) {

    if (databaseConfigs == null || databaseConfigs.length < 1) throw new JedisValidationException(
        "DatabaseClientConfigs are required for MultiDbConnectionProvider");

    for (DatabaseConfig databaseConfig : databaseConfigs) {
      if (databaseConfig == null)
        throw new IllegalArgumentException("DatabaseClientConfigs must not contain null elements");
    }
    this.databaseConfigs = databaseConfigs;
  }

  /**
   * Returns the array of database configurations defining available Redis endpoints.
   * @return array of database configurations, never null or empty
   */
  public DatabaseConfig[] getDatabaseConfigs() {
    return databaseConfigs;
  }

  /**
   * Returns the encapsulated retry configuration for command execution.
   * <p>
   * This provides access to all retry-related settings through a single configuration object.
   * </p>
   * @return retry configuration, never null
   * @see RetryConfig
   */
  public RetryConfig getCommandRetry() {
    return commandRetry;
  }

  /**
   * Returns the encapsulated circuit breaker configuration for failure detection.
   * <p>
   * This provides access to all circuit breaker-related settings through a single configuration
   * object.
   * </p>
   * @return circuit breaker configuration, never null
   * @see CircuitBreakerConfig
   */
  public CircuitBreakerConfig getFailureDetector() {
    return failureDetector;
  }

  /**
   * Returns the list of exception classes that trigger immediate fallback to next database.
   * @return list of exception classes that trigger fallback, never null
   * @see #fallbackExceptionList
   */
  public List<Class<? extends Throwable>> getFallbackExceptionList() {
    return fallbackExceptionList;
  }

  /**
   * Returns whether failed commands are retried during failover.
   * @return true if commands are retried during failover, false otherwise
   * @see #retryOnFailover
   */
  public boolean isRetryOnFailover() {
    return retryOnFailover;
  }

  /**
   * Returns whether automatic failback to higher-priority databases is supported.
   * @return true if automatic failback is enabled, false if manual failback is required
   * @see #isFailbackSupported
   */
  public boolean isFailbackSupported() {
    return isFailbackSupported;
  }

  /**
   * Returns the interval between checks for failback opportunities.
   * @return failback check interval in milliseconds
   * @see #failbackCheckInterval
   */
  public long getFailbackCheckInterval() {
    return failbackCheckInterval;
  }

  /**
   * Returns the grace period to keep databases disabled after they become unhealthy.
   * @return grace period in milliseconds
   * @see #gracePeriod
   */
  public long getGracePeriod() {
    return gracePeriod;
  }

  /**
   * Returns the maximum number of failover attempts.
   * @return maximum number of failover attempts
   * @see #maxNumFailoverAttempts
   */
  public int getMaxNumFailoverAttempts() {
    return maxNumFailoverAttempts;

  }

  /**
   * Returns the delay in milliseconds between failover attempts.
   * @return delay in milliseconds between failover attempts
   * @see #delayInBetweenFailoverAttempts
   */
  public int getDelayInBetweenFailoverAttempts() {
    return delayInBetweenFailoverAttempts;
  }

  /**
   * Returns whether connections are forcefully terminated during failover.
   * @return true if fast failover is enabled, false for graceful failover
   * @see #fastFailover
   */
  public boolean isFastFailover() {
    return fastFailover;
  }

  /**
   * Returns the initialization policy that determines when the multi-database connection is ready.
   * <p>
   * The policy is evaluated based on the completion status of database health checks, and the
   * decision to continue waiting, succeed, or fail is based on the number of available, pending,
   * and failed connections.
   * </p>
   * @return the initialization policy, defaults to
   *         {@link InitializationPolicy.BuiltIn#MAJORITY_AVAILABLE}
   * @see InitializationPolicy
   * @see #initializationPolicy
   */
  public InitializationPolicy getInitializationPolicy() {
    return initializationPolicy;
  }

  /**
   * Creates a new Builder instance for configuring MultiDbConfig.
   * <p>
   * At least one database configuration must be added to the builder before calling build(). Use
   * the endpoint() methods to add database configurations.
   * </p>
   * @return new Builder instance
   * @throws JedisValidationException if databaseConfigs is null or empty
   * @see Builder#Builder(DatabaseConfig[])
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new Builder instance for configuring MultiDbConfig.
   * @param databaseConfigs array of database configurations defining available Redis endpoints
   * @return new Builder instance
   * @throws JedisValidationException if databaseConfigs is null or empty
   * @see Builder#Builder(DatabaseConfig[])
   */
  public static Builder builder(DatabaseConfig[] databaseConfigs) {
    return new Builder(databaseConfigs);
  }

  /**
   * Creates a new Builder instance for configuring MultiDbConfig.
   * @param databaseConfigs list of database configurations defining available Redis endpoints
   * @return new Builder instance
   * @throws JedisValidationException if databaseConfigs is null or empty
   * @see Builder#Builder(List)
   */
  public static Builder builder(List<DatabaseConfig> databaseConfigs) {
    return new Builder(databaseConfigs);
  }

  /**
   * Configuration class for individual Redis database endpoints within a multi-database setup.
   * <p>
   * Each DatabaseConfig represents a single Redis endpoint that can participate in the
   * multi-database failover system. It encapsulates the connection details, weight for
   * priority-based selection, and health check configuration for that endpoint.
   * </p>
   * @see Builder
   * @see StrategySupplier
   * @see redis.clients.jedis.mcf.HealthCheckStrategy
   */
  public static class DatabaseConfig {

    /** The Redis endpoint (host and port) for this database. */
    private final Endpoint endpoint;

    /** Jedis client configuration containing connection settings and authentication. */
    private final JedisClientConfig jedisClientConfig;

    /** Optional connection pool configuration for managing connections to this database. */
    private GenericObjectPoolConfig<Connection> connectionPoolConfig;

    /**
     * Weight value for database selection priority. Higher weights indicate higher priority.
     * Default value is 1.0f.
     */
    private float weight = 1.0f;

    /**
     * Strategy supplier for creating health check instances for this database. Default is
     * PingStrategy.DEFAULT.
     */
    private StrategySupplier healthCheckStrategySupplier;

    /**
     * Constructs a DatabaseConfig with basic endpoint and client configuration.
     * <p>
     * This constructor creates a database configuration with default settings: weight of 1.0f and
     * PingStrategy for health checks. Use the {@link Builder} for more advanced configuration
     * options.
     * </p>
     * @param endpoint the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @throws IllegalArgumentException if endpoint or clientConfig is null
     */
    public DatabaseConfig(Endpoint endpoint, JedisClientConfig clientConfig) {
      this.endpoint = endpoint;
      this.jedisClientConfig = clientConfig;
    }

    /**
     * Constructs a DatabaseConfig with endpoint, client, and connection pool configuration.
     * <p>
     * This constructor allows specification of connection pool settings in addition to basic
     * endpoint configuration. Default weight of 1.0f and PingStrategy for health checks are used.
     * </p>
     * @param endpoint the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @param connectionPoolConfig the connection pool configuration
     * @throws IllegalArgumentException if endpoint or clientConfig is null
     */
    public DatabaseConfig(Endpoint endpoint, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> connectionPoolConfig) {
      this.endpoint = endpoint;
      this.jedisClientConfig = clientConfig;
      this.connectionPoolConfig = connectionPoolConfig;
    }

    /**
     * Private constructor used by the Builder to create configured instances.
     * @param builder the builder containing configuration values
     */
    private DatabaseConfig(Builder builder) {
      this.endpoint = builder.endpoint;
      this.jedisClientConfig = builder.jedisClientConfig;
      this.connectionPoolConfig = builder.connectionPoolConfig;
      this.weight = builder.weight;
      this.healthCheckStrategySupplier = builder.healthCheckStrategySupplier;
    }

    /**
     * Returns the Redis endpoint (host and port) for this database.
     * @return the host and port information
     */
    public Endpoint getEndpoint() {
      return endpoint;
    }

    /**
     * Creates a new Builder instance for configuring a DatabaseConfig.
     * @param endpoint the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @return new Builder instance
     * @throws IllegalArgumentException if endpoint or clientConfig is null
     */

    public static Builder builder(Endpoint endpoint, JedisClientConfig clientConfig) {
      return new Builder(endpoint, clientConfig);
    }

    /**
     * Returns the Jedis client configuration for this database.
     * @return the client configuration containing connection settings and authentication
     */
    public JedisClientConfig getJedisClientConfig() {
      return jedisClientConfig;
    }

    /**
     * Returns the connection pool configuration for this database.
     * @return the connection pool configuration, may be null if not specified
     */
    public GenericObjectPoolConfig<Connection> getConnectionPoolConfig() {
      return connectionPoolConfig;
    }

    /**
     * Returns the weight value used for database selection priority.
     * <p>
     * Higher weight values indicate higher priority. During failover, databases are selected in
     * descending order of weight (highest weight first).
     * </p>
     * @return the weight value, default is 1.0f
     */
    public float getWeight() {
      return weight;
    }

    /**
     * Returns the health check strategy supplier for this database.
     * <p>
     * The strategy supplier is used to create health check instances that monitor this database's
     * availability. Returns null if health checks are disabled.
     * </p>
     * @return the health check strategy supplier, or null if health checks are disabled
     * @see StrategySupplier
     * @see redis.clients.jedis.mcf.HealthCheckStrategy
     */
    public StrategySupplier getHealthCheckStrategySupplier() {
      return healthCheckStrategySupplier;
    }

    /**
     * Builder class for creating DatabaseConfig instances with fluent configuration API.
     * <p>
     * The Builder provides a convenient way to configure database settings including connection
     * pooling, weight-based priority, and health check strategies. All configuration methods return
     * the builder instance for method chaining.
     * </p>
     * <p>
     * <strong>Default Values:</strong>
     * </p>
     * <ul>
     * <li><strong>Weight:</strong> 1.0f (standard priority)</li>
     * <li><strong>Health Check:</strong> {@link redis.clients.jedis.mcf.PingStrategy#DEFAULT}</li>
     * <li><strong>Connection Pool:</strong> null (uses default pooling)</li>
     * </ul>
     */
    public static class Builder {
      /** The Redis endpoint for this database configuration. */
      private Endpoint endpoint;

      /** The Jedis client configuration. */
      private JedisClientConfig jedisClientConfig;

      /** Optional connection pool configuration. */
      private GenericObjectPoolConfig<Connection> connectionPoolConfig;

      /** Weight for database selection priority. Default: 1.0f */
      private float weight = 1.0f;

      /** Health check strategy supplier. Default: PingStrategy.DEFAULT */
      private StrategySupplier healthCheckStrategySupplier = PingStrategy.DEFAULT;

      /**
       * Constructs a new Builder with required endpoint and client configuration.
       * @param endpoint the Redis endpoint (host and port)
       * @param clientConfig the Jedis client configuration
       * @throws IllegalArgumentException if endpoint or clientConfig is null
       */
      public Builder(Endpoint endpoint, JedisClientConfig clientConfig) {
        this.endpoint = endpoint;
        this.jedisClientConfig = clientConfig;
      }

      /**
       * Sets the connection pool configuration for this database.
       * <p>
       * Connection pooling helps manage connections efficiently and provides better performance
       * under load. If not specified, default pooling behavior will be used.
       * </p>
       * @param connectionPoolConfig the connection pool configuration
       * @return this builder instance for method chaining
       */
      public Builder connectionPoolConfig(
          GenericObjectPoolConfig<Connection> connectionPoolConfig) {
        this.connectionPoolConfig = connectionPoolConfig;
        return this;
      }

      /**
       * Sets the weight value for database selection priority.
       * <p>
       * Weight determines the priority order for database selection during failover. Databases with
       * higher weights are preferred over those with lower weights. The system will attempt to use
       * the highest-weight healthy database available.
       * </p>
       * <p>
       * <strong>Examples:</strong>
       * </p>
       * <ul>
       * <li><strong>1.0f:</strong> Standard priority (default)</li>
       * <li><strong>0.8f:</strong> Lower priority (secondary database)</li>
       * <li><strong>0.1f:</strong> Lowest priority (backup database)</li>
       * </ul>
       * @param weight the weight value for priority-based selection
       * @return this builder instance for method chaining
       */
      public Builder weight(float weight) {
        this.weight = weight;
        return this;
      }

      /**
       * Sets a custom health check strategy supplier for this database.
       * <p>
       * The strategy supplier creates health check instances that monitor this database's
       * availability. Different databases can use different health check strategies based on their
       * specific requirements.
       * </p>
       * @param healthCheckStrategySupplier the health check strategy supplier
       * @return this builder instance for method chaining
       * @throws IllegalArgumentException if healthCheckStrategySupplier is null
       * @see StrategySupplier
       * @see redis.clients.jedis.mcf.HealthCheckStrategy
       */
      public Builder healthCheckStrategySupplier(StrategySupplier healthCheckStrategySupplier) {
        if (healthCheckStrategySupplier == null) {
          throw new IllegalArgumentException("healthCheckStrategySupplier must not be null");
        }
        this.healthCheckStrategySupplier = healthCheckStrategySupplier;
        return this;
      }

      /**
       * Sets a specific health check strategy instance for this database.
       * <p>
       * This is a convenience method that wraps the provided strategy in a supplier that always
       * returns the same instance. Use this when you have a pre-configured strategy instance.
       * </p>
       * <p>
       * <strong>Note:</strong> The same strategy instance will be reused, so ensure it's
       * thread-safe if multiple databases might use it.
       * </p>
       * @param healthCheckStrategy the health check strategy instance
       * @return this builder instance for method chaining
       * @throws IllegalArgumentException if healthCheckStrategy is null
       * @see #healthCheckStrategySupplier(StrategySupplier)
       */
      public Builder healthCheckStrategy(HealthCheckStrategy healthCheckStrategy) {
        if (healthCheckStrategy == null) {
          throw new IllegalArgumentException("healthCheckStrategy must not be null");
        }
        this.healthCheckStrategySupplier = (hostAndPort, jedisClientConfig) -> healthCheckStrategy;
        return this;
      }

      /**
       * Enables or disables health checks for this database.
       * <p>
       * When health checks are disabled (false), the database will not be proactively monitored for
       * availability. This means:
       * </p>
       * <ul>
       * <li>No background health check threads will be created</li>
       * <li>Failback to this database must be triggered manually</li>
       * <li>The database is assumed to be healthy unless circuit breaker opens</li>
       * </ul>
       * <p>
       * When health checks are enabled (true) and no strategy supplier was previously set, the
       * default {@link redis.clients.jedis.mcf.PingStrategy#DEFAULT} will be used.
       * </p>
       * @param healthCheckEnabled true to enable health checks, false to disable
       * @return this builder instance for method chaining
       */
      public Builder healthCheckEnabled(boolean healthCheckEnabled) {
        if (!healthCheckEnabled) {
          this.healthCheckStrategySupplier = null;
        } else if (healthCheckStrategySupplier == null) {
          this.healthCheckStrategySupplier = PingStrategy.DEFAULT;
        }
        return this;
      }

      /**
       * Builds and returns a new DatabaseConfig instance with the configured settings.
       * @return a new DatabaseConfig instance
       */
      public DatabaseConfig build() {
        return new DatabaseConfig(this);
      }
    }
  }

  /**
   * Builder class for creating MultiDbConfig instances with comprehensive configuration options.
   * <p>
   * The Builder provides a fluent API for configuring all aspects of multi-database failover
   * behavior, including retry logic, circuit breaker settings, and failback mechanisms. It uses
   * sensible defaults based on production best practices while allowing fine-tuning for specific
   * requirements.
   * </p>
   * @see MultiDbConfig
   * @see DatabaseConfig
   */
  public static class Builder {

    /** Array of database configurations defining available Redis endpoints. */
    private final List<DatabaseConfig> databaseConfigs = new ArrayList<>();

    // ============ Retry Configuration Fields ============
    /** Encapsulated retry configuration for command execution. */
    private RetryConfig commandRetry = RetryConfig.builder().build();

    // ============ Circuit Breaker Configuration Fields ============
    /** Encapsulated circuit breaker configuration for failure detection. */
    private CircuitBreakerConfig failureDetector = CircuitBreakerConfig.builder().build();

    /** List of exception classes that trigger immediate fallback to next database. */
    private List<Class<? extends Throwable>> fallbackExceptionList = FALLBACK_EXCEPTIONS_DEFAULT;

    // ============ Failover Configuration Fields ============
    /** Whether to retry failed commands during failover. */
    private boolean retryOnFailover = false;

    /** Whether automatic failback to higher-priority databases is supported. */
    private boolean isFailbackSupported = true;

    /** Interval between checks for failback opportunities in milliseconds. */
    private long failbackCheckInterval = FAILBACK_CHECK_INTERVAL_DEFAULT;

    /** Grace period to keep databases disabled after they become unhealthy in milliseconds. */
    private long gracePeriod = GRACE_PERIOD_DEFAULT;

    /** Whether to forcefully terminate connections during failover. */
    private boolean fastFailover = false;

    /** Maximum number of failover attempts. */
    private int maxNumFailoverAttempts = MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT;

    /** Delay in milliseconds between failover attempts. */
    private int delayInBetweenFailoverAttempts = DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT;

    /** Initialization policy for determining when the multi-database connection is ready. */
    private InitializationPolicy initializationPolicy = InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE;

    /**
     * Constructs a new Builder with the specified database configurations.
     */
    public Builder() {
    }

    /**
     * Constructs a new Builder with the specified database configurations.
     * @param databaseConfigs array of database configurations defining available Redis endpoints
     * @throws JedisValidationException if databaseConfigs is null or empty
     */
    public Builder(DatabaseConfig[] databaseConfigs) {

      this(Arrays.asList(databaseConfigs));
    }

    /**
     * Constructs a new Builder with the specified database configurations.
     * @param databaseConfigs list of database configurations defining available Redis endpoints
     * @throws JedisValidationException if databaseConfigs is null or empty
     */
    public Builder(List<DatabaseConfig> databaseConfigs) {
      this.databaseConfigs.addAll(databaseConfigs);
    }

    /**
     * Adds a pre-configured database configuration.
     * <p>
     * This method allows adding a fully configured DatabaseConfig instance, providing maximum
     * flexibility for advanced configurations including custom health check strategies, connection
     * pool settings, etc.
     * </p>
     * @param databaseConfig the pre-configured database configuration
     * @return this builder
     */
    public Builder database(DatabaseConfig databaseConfig) {
      this.databaseConfigs.add(databaseConfig);
      return this;
    }

    /**
     * Adds a database endpoint with custom client configuration.
     * <p>
     * This method allows specifying database-specific configuration such as authentication, SSL
     * settings, timeouts, etc. This configuration will override the default client configuration
     * for this specific database endpoint.
     * </p>
     * @param endpoint the Redis server endpoint
     * @param weight the weight for this endpoint (higher values = higher priority)
     * @param clientConfig the client configuration for this endpoint
     * @return this builder
     */
    public Builder database(Endpoint endpoint, float weight, JedisClientConfig clientConfig) {

      DatabaseConfig databaseConfig = DatabaseConfig.builder(endpoint, clientConfig).weight(weight)
          .build();

      this.databaseConfigs.add(databaseConfig);
      return this;
    }

    // ============ Retry Configuration Methods ============

    /**
     * Sets the encapsulated retry configuration for command execution.
     * <p>
     * This provides a cleaner API for configuring retry behavior by using a dedicated
     * {@link RetryConfig} object.
     * </p>
     * @param commandRetry the retry configuration
     * @return this builder instance for method chaining
     * @see RetryConfig
     */
    public Builder commandRetry(RetryConfig commandRetry) {
      this.commandRetry = commandRetry;
      return this;
    }

    // ============ Circuit Breaker Configuration Methods ============

    /**
     * Sets the encapsulated circuit breaker configuration for failure detection.
     * <p>
     * This provides a cleaner API for configuring circuit breaker behavior by using a dedicated
     * {@link CircuitBreakerConfig} object.
     * </p>
     * @param failureDetector the circuit breaker configuration
     * @return this builder instance for method chaining
     * @see CircuitBreakerConfig
     */
    public Builder failureDetector(CircuitBreakerConfig failureDetector) {
      this.failureDetector = failureDetector;
      return this;
    }

    /**
     * Sets the list of exception classes that trigger immediate fallback to the next available
     * database.
     * <p>
     * When these exceptions occur, the system will immediately attempt to failover to the next
     * available database without waiting for circuit breaker thresholds. This enables fast failover
     * for specific error conditions.
     * </p>
     * <p>
     * <strong>Default exceptions:</strong>
     * </p>
     * <ul>
     * <li>{@link CallNotPermittedException} - Circuit breaker is open</li>
     * <li>{@link redis.clients.jedis.mcf.ConnectionFailoverException} - Connection-level failover
     * required</li>
     * </ul>
     * @param fallbackExceptionList list of exception classes that trigger immediate fallback
     * @return this builder instance for method chaining
     */
    public Builder fallbackExceptionList(List<Class<? extends Throwable>> fallbackExceptionList) {
      this.fallbackExceptionList = fallbackExceptionList;
      return this;
    }

    // ============ Failover Configuration Methods ============

    /**
     * Sets whether failed commands should be retried during the failover process.
     * <p>
     * When enabled, commands that fail during failover will be retried according to the configured
     * retry settings on the new database. When disabled, failed commands during failover will
     * immediately return the failure to the caller.
     * </p>
     * <p>
     * <strong>Trade-offs:</strong>
     * </p>
     * <ul>
     * <li><strong>Enabled:</strong> Better resilience, potentially longer response times</li>
     * <li><strong>Disabled:</strong> Faster failover, some operations may fail (default)</li>
     * </ul>
     * @param retryOnFailover true to retry failed commands during failover, false otherwise
     * @return this builder instance for method chaining
     */
    public Builder retryOnFailover(boolean retryOnFailover) {
      this.retryOnFailover = retryOnFailover;
      return this;
    }

    /**
     * Sets whether automatic failback to higher-priority databases is supported.
     * <p>
     * When enabled, the system will automatically monitor failed da using health checks and
     * failback to higher-priority (higher weight) databases when they recover. When disabled,
     * failback must be triggered manually.
     * </p>
     * <p>
     * <strong>Requirements for automatic failback:</strong>
     * </p>
     * <ul>
     * <li>Health checks must be enabled on database configurations</li>
     * <li>Grace period must elapse after database becomes unhealthy</li>
     * <li>Higher-priority database must pass health checks</li>
     * </ul>
     * @param supported true to enable automatic failback, false for manual failback only
     * @return this builder instance for method chaining
     */
    public Builder failbackSupported(boolean supported) {
      this.isFailbackSupported = supported;
      return this;
    }

    /**
     * Sets the interval between checks for failback opportunities to recovered databases.
     * <p>
     * This controls how frequently the system checks if a higher-priority database has recovered
     * and is available for failback. Lower values provide faster failback response but increase
     * monitoring overhead.
     * </p>
     * <p>
     * <strong>Typical Values:</strong>
     * </p>
     * <ul>
     * <li><strong>1-2 seconds:</strong> Fast failback for critical applications</li>
     * <li><strong>5 seconds:</strong> Balanced approach (default)</li>
     * <li><strong>10-30 seconds:</strong> Conservative monitoring for stable environments</li>
     * </ul>
     * @param failbackCheckInterval interval in milliseconds between failback checks
     * @return this builder instance for method chaining
     */
    public Builder failbackCheckInterval(long failbackCheckInterval) {
      this.failbackCheckInterval = failbackCheckInterval;
      return this;
    }

    /**
     * Sets the grace period to keep databases disabled after they become unhealthy.
     * <p>
     * After a database is marked as unhealthy, it remains disabled for this grace period before
     * being eligible for failback, even if health checks indicate recovery. This prevents rapid
     * oscillation between databases during intermittent failures.
     * </p>
     * <p>
     * <strong>Considerations:</strong>
     * </p>
     * <ul>
     * <li><strong>Short periods (5-10s):</strong> Faster recovery, risk of oscillation</li>
     * <li><strong>Medium periods (10-30s):</strong> Balanced stability (default: 10s)</li>
     * <li><strong>Long periods (60s+):</strong> Maximum stability, slower recovery</li>
     * </ul>
     * @param gracePeriod grace period in milliseconds
     * @return this builder instance for method chaining
     */
    public Builder gracePeriod(long gracePeriod) {
      this.gracePeriod = gracePeriod;
      return this;
    }

    /**
     * Sets whether to forcefully terminate connections during failover for faster database
     * switching.
     * <p>
     * When enabled, existing connections to the failed database are immediately closed during
     * failover, potentially reducing failover time but may cause some in-flight operations to fail.
     * When disabled, connections are closed gracefully.
     * </p>
     * <p>
     * <strong>Trade-offs:</strong>
     * </p>
     * <ul>
     * <li><strong>Enabled:</strong> Faster failover, potential operation failures</li>
     * <li><strong>Disabled:</strong> Graceful failover, potentially slower (default)</li>
     * </ul>
     * @param fastFailover true for fast failover, false for graceful failover
     * @return this builder instance for method chaining
     */
    public Builder fastFailover(boolean fastFailover) {
      this.fastFailover = fastFailover;
      return this;
    }

    /**
     * Sets the maximum number of failover attempts.
     * <p>
     * This setting controls how many times the system will attempt to failover to a different
     * database before giving up. For example, if set to 3, the system will make 1 initial attempt
     * plus 2 failover attempts for a total of 3 attempts.
     * </p>
     * <p>
     * <strong>Default:</strong> {@value #MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT}
     * </p>
     * @param maxNumFailoverAttempts maximum number of failover attempts
     * @return this builder instance for method chaining
     */
    public Builder maxNumFailoverAttempts(int maxNumFailoverAttempts) {
      this.maxNumFailoverAttempts = maxNumFailoverAttempts;
      return this;
    }

    /**
     * Sets the delay in milliseconds between failover attempts.
     * <p>
     * This setting controls how long the system will wait before attempting to failover to a
     * different database. For example, if set to 1000, the system will wait 1 second before
     * attempting to failover to a different database.
     * </p>
     * <p>
     * <strong>Default:</strong> {@value #DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT} milliseconds
     * </p>
     * @param delayInBetweenFailoverAttempts delay in milliseconds between failover attempts
     * @return this builder instance for method chaining
     */
    public Builder delayInBetweenFailoverAttempts(int delayInBetweenFailoverAttempts) {
      this.delayInBetweenFailoverAttempts = delayInBetweenFailoverAttempts;
      return this;
    }

    /**
     * Sets the initialization policy that determines when the multi-database connection is ready.
     * <p>
     * The policy is evaluated based on the completion status of database health checks, and the
     * decision to continue waiting, succeed, or fail is based on the number of available, pending,
     * and failed connections.
     * </p>
     * <p>
     * <strong>Built-in policies:</strong>
     * </p>
     * <ul>
     * <li>{@link InitializationPolicy.BuiltIn#ALL_AVAILABLE} - All databases need to be
     * available</li>
     * <li>{@link InitializationPolicy.BuiltIn#MAJORITY_AVAILABLE} - Majority of databases need to
     * be available (default)</li>
     * <li>{@link InitializationPolicy.BuiltIn#ONE_AVAILABLE} - At least one database needs to be
     * available</li>
     * </ul>
     * @param initializationPolicy the initialization policy to use
     * @return this builder instance for method chaining
     */
    public Builder initializationPolicy(InitializationPolicy initializationPolicy) {
      JedisAsserts.notNull(initializationPolicy, "initializationPolicy must not be null");
      this.initializationPolicy = initializationPolicy;
      return this;
    }

    /**
     * Builds and returns a new MultiDbConfig instance with all configured settings.
     * <p>
     * This method creates the final configuration object by copying all builder settings to the
     * configuration instance. The builder can be reused after calling build() to create additional
     * configurations with different settings.
     * </p>
     * @return a new MultiDbConfig instance with the configured settings
     */
    public MultiDbConfig build() {

      MultiDbConfig config = new MultiDbConfig(this.databaseConfigs.toArray(new DatabaseConfig[0]));

      // Copy retry configuration
      config.commandRetry = this.commandRetry;

      // Copy circuit breaker configuration
      config.failureDetector = this.failureDetector;

      // Copy fallback and failover configuration
      config.fallbackExceptionList = this.fallbackExceptionList;
      config.retryOnFailover = this.retryOnFailover;
      config.isFailbackSupported = this.isFailbackSupported;
      config.failbackCheckInterval = this.failbackCheckInterval;
      config.gracePeriod = this.gracePeriod;
      config.fastFailover = this.fastFailover;
      config.maxNumFailoverAttempts = this.maxNumFailoverAttempts;
      config.delayInBetweenFailoverAttempts = this.delayInBetweenFailoverAttempts;
      config.initializationPolicy = this.initializationPolicy;

      return config;
    }

  }

}