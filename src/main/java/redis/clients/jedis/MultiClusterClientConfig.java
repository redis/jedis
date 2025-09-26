package redis.clients.jedis;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.ConnectionFailoverException;
import redis.clients.jedis.mcf.EchoStrategy;
import redis.clients.jedis.mcf.JedisFailoverThresholdsExceededException;
import redis.clients.jedis.mcf.HealthCheckStrategy;

/**
 * Configuration class for multi-cluster Redis deployments with automatic failover and failback
 * capabilities.
 * <p>
 * This configuration enables seamless failover between multiple Redis clusters, databases, or
 * endpoints by providing comprehensive settings for retry logic, circuit breaker behavior, health
 * checks, and failback mechanisms. It is designed to work with
 * {@link redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider} to provide high availability
 * and disaster recovery capabilities.
 * </p>
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li><strong>Multi-Cluster Support:</strong> Configure multiple Redis endpoints with individual
 * weights and health checks</li>
 * <li><strong>Circuit Breaker Pattern:</strong> Automatic failure detection and circuit opening
 * based on configurable thresholds</li>
 * <li><strong>Retry Logic:</strong> Configurable retry attempts with exponential backoff for
 * transient failures</li>
 * <li><strong>Health Check Integration:</strong> Pluggable health check strategies for proactive
 * monitoring</li>
 * <li><strong>Automatic Failback:</strong> Intelligent failback to higher-priority clusters when
 * they recover</li>
 * <li><strong>Weight-Based Routing:</strong> Priority-based cluster selection using configurable
 * weights</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * 
 * <pre>
 * {
 *   &#64;code
 *   // Configure individual clusters
 *   ClusterConfig primary = ClusterConfig.builder(primaryEndpoint, clientConfig).weight(1.0f)
 *       .build();
 *
 *   ClusterConfig secondary = ClusterConfig.builder(secondaryEndpoint, clientConfig).weight(0.5f)
 *       .healthCheckEnabled(true).build();
 *
 *   // Build multi-cluster configuration
 *   MultiClusterClientConfig config = MultiClusterClientConfig.builder(primary, secondary)
 *       .circuitBreakerFailureRateThreshold(10.0f).retryMaxAttempts(3).failbackSupported(true)
 *       .gracePeriod(10000).build();
 *
 *   // Use with connection provider
 *   MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
 *       config);
 * }
 * </pre>
 * <p>
 * The configuration leverages <a href="https://resilience4j.readme.io/docs">Resilience4j</a> for
 * circuit breaker and retry implementations, providing battle-tested fault tolerance patterns.
 * </p>
 * @see redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider
 * @see redis.clients.jedis.mcf.HealthCheckStrategy
 * @see redis.clients.jedis.mcf.EchoStrategy
 * @see redis.clients.jedis.mcf.LagAwareStrategy
 * @since 7.0
 */
// TODO: move
@Experimental
public final class MultiClusterClientConfig {

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
   * <li>{@link redis.clients.jedis.mcf.EchoStrategy#DEFAULT} - Uses Redis ECHO command for health
   * checks</li>
   * <li>Custom implementations for specific monitoring requirements</li>
   * <li>Redis Enterprise implementations using REST API monitoring</li>
   * </ul>
   * @see redis.clients.jedis.mcf.HealthCheckStrategy
   * @see redis.clients.jedis.mcf.EchoStrategy
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

  /** Default size of the sliding window for circuit breaker calculations. */
  private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT = 2;

  /** Minimum number of failures before circuit breaker is tripped. */
  private static final int THRESHOLD_MIN_NUM_OF_FAILURES_DEFAULT = 1000;

  /** Default list of exceptions that are recorded as circuit breaker failures. */
  private static final List<Class> CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT = Arrays
      .asList(JedisConnectionException.class);

  /** Default list of exceptions that trigger fallback to next available cluster. */
  private static final List<Class<? extends Throwable>> FALLBACK_EXCEPTIONS_DEFAULT = Arrays.asList(
    CallNotPermittedException.class, ConnectionFailoverException.class,
    JedisFailoverThresholdsExceededException.class);

  /** Default interval in milliseconds for checking if failed clusters have recovered. */
  private static final long FAILBACK_CHECK_INTERVAL_DEFAULT = 5000;

  /** Default grace period in milliseconds to keep clusters disabled after they become unhealthy. */
  private static final long GRACE_PERIOD_DEFAULT = 10000;

  /** Default maximum number of failover attempts. */
  private static final int MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT = 10;

  /** Default delay in milliseconds between failover attempts. */
  private static final int DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT = 12000;

  /** Array of cluster configurations defining the available Redis endpoints and their settings. */
  private final ClusterConfig[] clusterConfigs;

  // ============ Retry Configuration ============
  // Based on Resilience4j Retry: https://resilience4j.readme.io/docs/retry

  /**
   * Maximum number of retry attempts including the initial call as the first attempt.
   * <p>
   * For example, if set to 3, the system will make 1 initial attempt plus 2 retry attempts for a
   * total of 3 attempts before giving up.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #RETRY_MAX_ATTEMPTS_DEFAULT}
   * </p>
   * @see #getRetryMaxAttempts()
   */
  private int retryMaxAttempts;

  /**
   * Fixed wait duration between retry attempts.
   * <p>
   * This duration is used as the base wait time and may be modified by the exponential backoff
   * multiplier to create increasing delays between retry attempts.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #RETRY_WAIT_DURATION_DEFAULT} milliseconds
   * </p>
   * @see #getRetryWaitDuration()
   * @see #retryWaitDurationExponentialBackoffMultiplier
   */
  private Duration retryWaitDuration;

  /**
   * Exponential backoff multiplier applied to the wait duration between retry attempts.
   * <p>
   * The wait duration increases exponentially between attempts using this multiplier. For example,
   * with an initial wait time of 1 second and a multiplier of 2, the retries would occur after
   * delays of: 1s, 2s, 4s, 8s, 16s, etc.
   * </p>
   * <p>
   * <strong>Formula:</strong> {@code actualWaitTime = baseWaitTime * (multiplier ^ attemptNumber)}
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT}
   * </p>
   * @see #getRetryWaitDurationExponentialBackoffMultiplier()
   * @see #retryWaitDuration
   */
  private int retryWaitDurationExponentialBackoffMultiplier;

  /**
   * List of exception classes that are recorded as failures and trigger retry attempts.
   * <p>
   * This parameter supports inheritance - any exception that is an instance of or extends from the
   * specified classes will trigger a retry. If this list is specified, all other exceptions are
   * considered successful unless explicitly ignored.
   * </p>
   * <p>
   * <strong>Default:</strong> {@link JedisConnectionException}
   * </p>
   * @see #getRetryIncludedExceptionList()
   * @see #retryIgnoreExceptionList
   */
  private List<Class> retryIncludedExceptionList;

  /**
   * List of exception classes that are ignored and do not trigger retry attempts.
   * <p>
   * This parameter supports inheritance - any exception that is an instance of or extends from the
   * specified classes will be ignored for retry purposes, even if they are included in the
   * {@link #retryIncludedExceptionList}.
   * </p>
   * <p>
   * <strong>Default:</strong> null (no exceptions ignored)
   * </p>
   * @see #getRetryIgnoreExceptionList()
   * @see #retryIncludedExceptionList
   */
  private List<Class> retryIgnoreExceptionList;

  // ============ Circuit Breaker Configuration ============
  // Based on Resilience4j Circuit Breaker: https://resilience4j.readme.io/docs/circuitbreaker

  /**
   * Failure rate threshold percentage that triggers circuit breaker transition to OPEN state.
   * <p>
   * When the failure rate equals or exceeds this threshold, the circuit breaker transitions to the
   * OPEN state and starts short-circuiting calls, immediately failing them without attempting to
   * reach the Redis cluster. This prevents cascading failures and allows the system to fail over to
   * the next available cluster.
   * </p>
   * <p>
   * <strong>Range:</strong> 0.0 to 100.0 (percentage)
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT}%
   * </p>
   * @see #getCircuitBreakerFailureRateThreshold()
   * @see #circuitBreakerSlidingWindowMinCalls
   */
  private float circuitBreakerFailureRateThreshold;

  /**
   * Size of the sliding window used to record call outcomes when the circuit breaker is CLOSED.
   * <strong>Default:</strong> {@value #CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT}
   * </p>
   * @see #getCircuitBreakerSlidingWindowSize()
   * @see #circuitBreakerSlidingWindowType
   */
  private int circuitBreakerSlidingWindowSize;

  /**
   * List of exception classes that are recorded as circuit breaker failures and increase the
   * failure rate.
   * <p>
   * Any exception that matches or inherits from the classes in this list counts as a failure for
   * circuit breaker calculations, unless explicitly ignored via
   * {@link #circuitBreakerIgnoreExceptionList}. If you specify this list, all other exceptions
   * count as successes unless they are explicitly ignored.
   * </p>
   * <p>
   * <strong>Default:</strong> {@link JedisConnectionException}
   * </p>
   * @see #getCircuitBreakerIncludedExceptionList()
   * @see #circuitBreakerIgnoreExceptionList
   */
  private List<Class> circuitBreakerIncludedExceptionList;

  /**
   * List of exception classes that are ignored by the circuit breaker and neither count as failures
   * nor successes.
   * <p>
   * Any exception that matches or inherits from the classes in this list will not affect circuit
   * breaker failure rate calculations, even if the exception is included in
   * {@link #circuitBreakerIncludedExceptionList}.
   * </p>
   * <p>
   * <strong>Default:</strong> null (no exceptions ignored)
   * </p>
   * @see #getCircuitBreakerIgnoreExceptionList()
   * @see #circuitBreakerIncludedExceptionList
   */
  private List<Class> circuitBreakerIgnoreExceptionList;

  /**
   * List of exception classes that trigger fallback to the next available cluster.
   * <p>
   * When these exceptions occur, the system will attempt to failover to the next available cluster
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
   * @see #retryMaxAttempts
   */
  private boolean retryOnFailover;

  /**
   * Minimum number of failures before circuit breaker is tripped.
   * <p>
   * When the number of failures exceeds this threshold, the circuit breaker will trip and prevent
   * further requests from being sent to the cluster until it has recovered.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #THRESHOLD_MIN_NUM_OF_FAILURES_DEFAULT}
   * </p>
   * @see #getThresholdMinNumOfFailures()
   */
  private int thresholdMinNumOfFailures;

  /**
   * Whether automatic failback to higher-priority clusters is supported.
   * <p>
   * When enabled, the system will automatically monitor failed clusters using health checks and
   * failback to higher-priority (higher weight) clusters when they recover. When disabled, manual
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
   * Interval in milliseconds between checks for failback opportunities to recovered clusters.
   * <p>
   * This setting controls how frequently the system checks if a higher-priority cluster has
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
   * Grace period in milliseconds to keep clusters disabled after they become unhealthy.
   * <p>
   * After a cluster is marked as unhealthy, it remains disabled for this grace period before being
   * eligible for failback, even if health checks indicate recovery. This prevents rapid oscillation
   * between clusters during intermittent failures.
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
   * Whether to forcefully terminate connections during failover for faster cluster switching.
   * <p>
   * When enabled, existing connections to the failed cluster are immediately closed during
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
   * This setting controls how many times the system will attempt to failover to a different cluster
   * before giving up. For example, if set to 3, the system will make 1 initial attempt plus 2
   * failover attempts for a total of 3 attempts.
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
   * different cluster. For example, if set to 1000, the system will wait 1 second before attempting
   * to failover to a different cluster.
   * </p>
   * <p>
   * <strong>Default:</strong> {@value #DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT} milliseconds
   * </p>
   * @see #getDelayInBetweenFailoverAttempts()
   */
  private int delayInBetweenFailoverAttempts;

  /**
   * Constructs a new MultiClusterClientConfig with the specified cluster configurations.
   * <p>
   * This constructor validates that at least one cluster configuration is provided and that all
   * configurations are non-null. Use the {@link Builder} class for more convenient configuration
   * with default values.
   * </p>
   * @param clusterConfigs array of cluster configurations defining the available Redis endpoints
   * @throws JedisValidationException if clusterConfigs is null or empty
   * @throws IllegalArgumentException if any cluster configuration is null
   * @see Builder#Builder(ClusterConfig[])
   */
  public MultiClusterClientConfig(ClusterConfig[] clusterConfigs) {
    if (clusterConfigs == null || clusterConfigs.length < 1) throw new JedisValidationException(
        "ClusterClientConfigs are required for MultiClusterPooledConnectionProvider");
    for (ClusterConfig clusterConfig : clusterConfigs) {
      if (clusterConfig == null)
        throw new IllegalArgumentException("ClusterClientConfigs must not contain null elements");
    }
    this.clusterConfigs = clusterConfigs;
  }

  /**
   * Returns the array of cluster configurations defining available Redis endpoints.
   * @return array of cluster configurations, never null or empty
   */
  public ClusterConfig[] getClusterConfigs() {
    return clusterConfigs;
  }

  /**
   * Returns the maximum number of retry attempts including the initial call.
   * @return maximum retry attempts
   * @see #retryMaxAttempts
   */
  public int getRetryMaxAttempts() {
    return retryMaxAttempts;
  }

  /**
   * Returns the base wait duration between retry attempts.
   * @return wait duration between retries
   * @see #retryWaitDuration
   */
  public Duration getRetryWaitDuration() {
    return retryWaitDuration;
  }

  /**
   * Returns the exponential backoff multiplier for retry wait duration.
   * @return exponential backoff multiplier
   * @see #retryWaitDurationExponentialBackoffMultiplier
   */
  public int getRetryWaitDurationExponentialBackoffMultiplier() {
    return retryWaitDurationExponentialBackoffMultiplier;
  }

  /**
   * Returns the failure rate threshold percentage for circuit breaker activation.
   * @return failure rate threshold as a percentage (0.0 to 100.0)
   * @see #circuitBreakerFailureRateThreshold
   */
  public float getCircuitBreakerFailureRateThreshold() {
    return circuitBreakerFailureRateThreshold;
  }

  /**
   * Returns the size of the sliding window used for circuit breaker calculations.
   * @return sliding window size (calls or seconds depending on window type)
   * @see #circuitBreakerSlidingWindowSize
   * @see #getCircuitBreakerSlidingWindowType()
   */
  public int getCircuitBreakerSlidingWindowSize() {
    return circuitBreakerSlidingWindowSize;
  }

  /**
   * Returns the list of exception classes that trigger retry attempts.
   * @return list of exception classes that are retried, never null
   * @see #retryIncludedExceptionList
   */
  public List<Class> getRetryIncludedExceptionList() {
    return retryIncludedExceptionList;
  }

  /**
   * Returns the list of exception classes that are ignored for retry purposes.
   * @return list of exception classes to ignore for retries, may be null
   * @see #retryIgnoreExceptionList
   */
  public List<Class> getRetryIgnoreExceptionList() {
    return retryIgnoreExceptionList;
  }

  /**
   * Returns the list of exception classes that are recorded as circuit breaker failures.
   * @return list of exception classes that count as failures, never null
   * @see #circuitBreakerIncludedExceptionList
   */
  public List<Class> getCircuitBreakerIncludedExceptionList() {
    return circuitBreakerIncludedExceptionList;
  }

  /**
   * Returns the list of exception classes that are ignored by the circuit breaker.
   * @return list of exception classes to ignore for circuit breaker calculations, may be null
   * @see #circuitBreakerIgnoreExceptionList
   */
  public List<Class> getCircuitBreakerIgnoreExceptionList() {
    return circuitBreakerIgnoreExceptionList;
  }

  /**
   * Returns the list of exception classes that trigger immediate fallback to next cluster.
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
   * Returns the minimum number of failures before circuit breaker is tripped.
   * @return minimum number of failures before circuit breaker is tripped
   * @see #thresholdMinNumOfFailures
   */
  public int getThresholdMinNumOfFailures() {
    return thresholdMinNumOfFailures;
  }

  /**
   * Returns whether automatic failback to higher-priority clusters is supported.
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
   * Returns the grace period to keep clusters disabled after they become unhealthy.
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
   * Creates a new Builder instance for configuring MultiClusterClientConfig.
   * @param clusterConfigs array of cluster configurations defining available Redis endpoints
   * @return new Builder instance
   * @throws JedisValidationException if clusterConfigs is null or empty
   * @see Builder#Builder(ClusterConfig[])
   */
  public static Builder builder(ClusterConfig[] clusterConfigs) {
    return new Builder(clusterConfigs);
  }

  /**
   * Creates a new Builder instance for configuring MultiClusterClientConfig.
   * @param clusterConfigs list of cluster configurations defining available Redis endpoints
   * @return new Builder instance
   * @throws JedisValidationException if clusterConfigs is null or empty
   * @see Builder#Builder(List)
   */
  public static Builder builder(List<ClusterConfig> clusterConfigs) {
    return new Builder(clusterConfigs);
  }

  /**
   * Configuration class for individual Redis cluster endpoints within a multi-cluster setup.
   * <p>
   * Each ClusterConfig represents a single Redis endpoint that can participate in the multi-cluster
   * failover system. It encapsulates the connection details, weight for priority-based selection,
   * and health check configuration for that endpoint.
   * </p>
   * @see Builder
   * @see StrategySupplier
   * @see redis.clients.jedis.mcf.HealthCheckStrategy
   */
  public static class ClusterConfig {

    /** The Redis endpoint (host and port) for this cluster. */
    private HostAndPort hostAndPort;

    /** Jedis client configuration containing connection settings and authentication. */
    private JedisClientConfig jedisClientConfig;

    /** Optional connection pool configuration for managing connections to this cluster. */
    private GenericObjectPoolConfig<Connection> connectionPoolConfig;

    /**
     * Weight value for cluster selection priority. Higher weights indicate higher priority. Default
     * value is 1.0f.
     */
    private float weight = 1.0f;

    /**
     * Strategy supplier for creating health check instances for this cluster. Default is
     * EchoStrategy.DEFAULT.
     */
    private StrategySupplier healthCheckStrategySupplier;

    /**
     * Constructs a ClusterConfig with basic endpoint and client configuration.
     * <p>
     * This constructor creates a cluster configuration with default settings: weight of 1.0f and
     * EchoStrategy for health checks. Use the {@link Builder} for more advanced configuration
     * options.
     * </p>
     * @param hostAndPort the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @throws IllegalArgumentException if hostAndPort or clientConfig is null
     */
    public ClusterConfig(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
      this.hostAndPort = hostAndPort;
      this.jedisClientConfig = clientConfig;
    }

    /**
     * Constructs a ClusterConfig with endpoint, client, and connection pool configuration.
     * <p>
     * This constructor allows specification of connection pool settings in addition to basic
     * endpoint configuration. Default weight of 1.0f and EchoStrategy for health checks are used.
     * </p>
     * @param hostAndPort the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @param connectionPoolConfig the connection pool configuration
     * @throws IllegalArgumentException if hostAndPort or clientConfig is null
     */
    public ClusterConfig(HostAndPort hostAndPort, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> connectionPoolConfig) {
      this.hostAndPort = hostAndPort;
      this.jedisClientConfig = clientConfig;
      this.connectionPoolConfig = connectionPoolConfig;
    }

    /**
     * Private constructor used by the Builder to create configured instances.
     * @param builder the builder containing configuration values
     */
    private ClusterConfig(Builder builder) {
      this.hostAndPort = builder.hostAndPort;
      this.jedisClientConfig = builder.jedisClientConfig;
      this.connectionPoolConfig = builder.connectionPoolConfig;
      this.weight = builder.weight;
      this.healthCheckStrategySupplier = builder.healthCheckStrategySupplier;
    }

    /**
     * Returns the Redis endpoint (host and port) for this cluster.
     * @return the host and port information
     */
    public HostAndPort getHostAndPort() {
      return hostAndPort;
    }

    /**
     * Creates a new Builder instance for configuring a ClusterConfig.
     * @param hostAndPort the Redis endpoint (host and port)
     * @param clientConfig the Jedis client configuration
     * @return new Builder instance
     * @throws IllegalArgumentException if hostAndPort or clientConfig is null
     */
    public static Builder builder(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
      return new Builder(hostAndPort, clientConfig);
    }

    /**
     * Returns the Jedis client configuration for this cluster.
     * @return the client configuration containing connection settings and authentication
     */
    public JedisClientConfig getJedisClientConfig() {
      return jedisClientConfig;
    }

    /**
     * Returns the connection pool configuration for this cluster.
     * @return the connection pool configuration, may be null if not specified
     */
    public GenericObjectPoolConfig<Connection> getConnectionPoolConfig() {
      return connectionPoolConfig;
    }

    /**
     * Returns the weight value used for cluster selection priority.
     * <p>
     * Higher weight values indicate higher priority. During failover, clusters are selected in
     * descending order of weight (highest weight first).
     * </p>
     * @return the weight value, default is 1.0f
     */
    public float getWeight() {
      return weight;
    }

    /**
     * Returns the health check strategy supplier for this cluster.
     * <p>
     * The strategy supplier is used to create health check instances that monitor this cluster's
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
     * Builder class for creating ClusterConfig instances with fluent configuration API.
     * <p>
     * The Builder provides a convenient way to configure cluster settings including connection
     * pooling, weight-based priority, and health check strategies. All configuration methods return
     * the builder instance for method chaining.
     * </p>
     * <p>
     * <strong>Default Values:</strong>
     * </p>
     * <ul>
     * <li><strong>Weight:</strong> 1.0f (standard priority)</li>
     * <li><strong>Health Check:</strong> {@link redis.clients.jedis.mcf.EchoStrategy#DEFAULT}</li>
     * <li><strong>Connection Pool:</strong> null (uses default pooling)</li>
     * </ul>
     */
    public static class Builder {
      /** The Redis endpoint for this cluster configuration. */
      private HostAndPort hostAndPort;

      /** The Jedis client configuration. */
      private JedisClientConfig jedisClientConfig;

      /** Optional connection pool configuration. */
      private GenericObjectPoolConfig<Connection> connectionPoolConfig;

      /** Weight for cluster selection priority. Default: 1.0f */
      private float weight = 1.0f;

      /** Health check strategy supplier. Default: EchoStrategy.DEFAULT */
      private StrategySupplier healthCheckStrategySupplier = EchoStrategy.DEFAULT;

      /**
       * Constructs a new Builder with required endpoint and client configuration.
       * @param hostAndPort the Redis endpoint (host and port)
       * @param clientConfig the Jedis client configuration
       * @throws IllegalArgumentException if hostAndPort or clientConfig is null
       */
      public Builder(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
        this.hostAndPort = hostAndPort;
        this.jedisClientConfig = clientConfig;
      }

      /**
       * Sets the connection pool configuration for this cluster.
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
       * Sets the weight value for cluster selection priority.
       * <p>
       * Weight determines the priority order for cluster selection during failover. Clusters with
       * higher weights are preferred over those with lower weights. The system will attempt to use
       * the highest-weight healthy cluster available.
       * </p>
       * <p>
       * <strong>Examples:</strong>
       * </p>
       * <ul>
       * <li><strong>1.0f:</strong> Standard priority (default)</li>
       * <li><strong>0.8f:</strong> Lower priority (secondary cluster)</li>
       * <li><strong>0.1f:</strong> Lowest priority (backup cluster)</li>
       * </ul>
       * @param weight the weight value for priority-based selection
       * @return this builder instance for method chaining
       */
      public Builder weight(float weight) {
        this.weight = weight;
        return this;
      }

      /**
       * Sets a custom health check strategy supplier for this cluster.
       * <p>
       * The strategy supplier creates health check instances that monitor this cluster's
       * availability. Different clusters can use different health check strategies based on their
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
       * Sets a specific health check strategy instance for this cluster.
       * <p>
       * This is a convenience method that wraps the provided strategy in a supplier that always
       * returns the same instance. Use this when you have a pre-configured strategy instance.
       * </p>
       * <p>
       * <strong>Note:</strong> The same strategy instance will be reused, so ensure it's
       * thread-safe if multiple clusters might use it.
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
       * Enables or disables health checks for this cluster.
       * <p>
       * When health checks are disabled (false), the cluster will not be proactively monitored for
       * availability. This means:
       * </p>
       * <ul>
       * <li>No background health check threads will be created</li>
       * <li>Failback to this cluster must be triggered manually</li>
       * <li>The cluster is assumed to be healthy unless circuit breaker opens</li>
       * </ul>
       * <p>
       * When health checks are enabled (true) and no strategy supplier was previously set, the
       * default {@link redis.clients.jedis.mcf.EchoStrategy#DEFAULT} will be used.
       * </p>
       * @param healthCheckEnabled true to enable health checks, false to disable
       * @return this builder instance for method chaining
       */
      public Builder healthCheckEnabled(boolean healthCheckEnabled) {
        if (!healthCheckEnabled) {
          this.healthCheckStrategySupplier = null;
        } else if (healthCheckStrategySupplier == null) {
          this.healthCheckStrategySupplier = EchoStrategy.DEFAULT;
        }
        return this;
      }

      /**
       * Builds and returns a new ClusterConfig instance with the configured settings.
       * @return a new ClusterConfig instance
       */
      public ClusterConfig build() {
        return new ClusterConfig(this);
      }
    }
  }

  /**
   * Builder class for creating MultiClusterClientConfig instances with comprehensive configuration
   * options.
   * <p>
   * The Builder provides a fluent API for configuring all aspects of multi-cluster failover
   * behavior, including retry logic, circuit breaker settings, and failback mechanisms. It uses
   * sensible defaults based on production best practices while allowing fine-tuning for specific
   * requirements.
   * </p>
   * @see MultiClusterClientConfig
   * @see ClusterConfig
   */
  public static class Builder {

    /** Array of cluster configurations defining available Redis endpoints. */
    private ClusterConfig[] clusterConfigs;

    // ============ Retry Configuration Fields ============
    /** Maximum number of retry attempts including the initial call. */
    private int retryMaxAttempts = RETRY_MAX_ATTEMPTS_DEFAULT;

    /** Wait duration between retry attempts in milliseconds. */
    private int retryWaitDuration = RETRY_WAIT_DURATION_DEFAULT;

    /** Exponential backoff multiplier for retry wait duration. */
    private int retryWaitDurationExponentialBackoffMultiplier = RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT;

    /** List of exception classes that trigger retry attempts. */
    private List<Class> retryIncludedExceptionList = RETRY_INCLUDED_EXCEPTIONS_DEFAULT;

    /** List of exception classes that are ignored for retry purposes. */
    private List<Class> retryIgnoreExceptionList = null;

    // ============ Circuit Breaker Configuration Fields ============
    /** Failure rate threshold percentage for circuit breaker activation. */
    private float circuitBreakerFailureRateThreshold = CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT;

    /** Size of the sliding window for circuit breaker calculations. */
    private int circuitBreakerSlidingWindowSize = CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT;

    /** List of exception classes that are recorded as circuit breaker failures. */
    private List<Class> circuitBreakerIncludedExceptionList = CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT;

    /** List of exception classes that are ignored by the circuit breaker. */
    private List<Class> circuitBreakerIgnoreExceptionList = null;

    /** List of exception classes that trigger immediate fallback to next cluster. */
    private List<Class<? extends Throwable>> fallbackExceptionList = FALLBACK_EXCEPTIONS_DEFAULT;

    // ============ Failover Configuration Fields ============
    /** Whether to retry failed commands during failover. */
    private boolean retryOnFailover = false;

    /** Minimum number of failures before circuit breaker is tripped. */
    private int thresholdMinNumOfFailures = THRESHOLD_MIN_NUM_OF_FAILURES_DEFAULT;

    /** Whether automatic failback to higher-priority clusters is supported. */
    private boolean isFailbackSupported = true;

    /** Interval between checks for failback opportunities in milliseconds. */
    private long failbackCheckInterval = FAILBACK_CHECK_INTERVAL_DEFAULT;

    /** Grace period to keep clusters disabled after they become unhealthy in milliseconds. */
    private long gracePeriod = GRACE_PERIOD_DEFAULT;

    /** Whether to forcefully terminate connections during failover. */
    private boolean fastFailover = false;

    /** Maximum number of failover attempts. */
    private int maxNumFailoverAttempts = MAX_NUM_FAILOVER_ATTEMPTS_DEFAULT;

    /** Delay in milliseconds between failover attempts. */
    private int delayInBetweenFailoverAttempts = DELAY_IN_BETWEEN_FAILOVER_ATTEMPTS_DEFAULT;

    /**
     * Constructs a new Builder with the specified cluster configurations.
     * @param clusterConfigs array of cluster configurations defining available Redis endpoints
     * @throws JedisValidationException if clusterConfigs is null or empty
     */
    public Builder(ClusterConfig[] clusterConfigs) {

      if (clusterConfigs == null || clusterConfigs.length < 1) throw new JedisValidationException(
          "ClusterClientConfigs are required for MultiClusterPooledConnectionProvider");

      this.clusterConfigs = clusterConfigs;
    }

    /**
     * Constructs a new Builder with the specified cluster configurations.
     * @param clusterConfigs list of cluster configurations defining available Redis endpoints
     * @throws JedisValidationException if clusterConfigs is null or empty
     */
    public Builder(List<ClusterConfig> clusterConfigs) {
      this(clusterConfigs.toArray(new ClusterConfig[0]));
    }

    // ============ Retry Configuration Methods ============

    /**
     * Sets the maximum number of retry attempts including the initial call.
     * <p>
     * This controls how many times a failed operation will be retried before giving up. For
     * example, if set to 3, the system will make 1 initial attempt plus 2 retry attempts for a
     * total of 3 attempts.
     * </p>
     * <p>
     * <strong>Recommendations:</strong>
     * </p>
     * <ul>
     * <li><strong>1:</strong> No retries (fail fast)</li>
     * <li><strong>3:</strong> Standard retry behavior (default)</li>
     * <li><strong>5+:</strong> Aggressive retry for critical operations, but be careful of retry
     * storms</li>
     * </ul>
     * @param retryMaxAttempts maximum number of attempts (must be &gt;= 1)
     * @return this builder instance for method chaining
     */
    public Builder retryMaxAttempts(int retryMaxAttempts) {
      this.retryMaxAttempts = retryMaxAttempts;
      return this;
    }

    /**
     * Sets the base wait duration between retry attempts in milliseconds.
     * <p>
     * This duration is used as the base wait time and may be modified by the exponential backoff
     * multiplier to create increasing delays between attempts.
     * </p>
     * <p>
     * <strong>Typical Values:</strong>
     * </p>
     * <ul>
     * <li><strong>100-500ms:</strong> Fast retry for low-latency scenarios</li>
     * <li><strong>500-1000ms:</strong> Standard retry timing (default: 500ms)</li>
     * <li><strong>1000-5000ms:</strong> Conservative retry for high-latency networks</li>
     * </ul>
     * @param retryWaitDuration wait duration in milliseconds (must be &gt;= 0)
     * @return this builder instance for method chaining
     */
    public Builder retryWaitDuration(int retryWaitDuration) {
      this.retryWaitDuration = retryWaitDuration;
      return this;
    }

    /**
     * Sets the exponential backoff multiplier for retry wait duration.
     * <p>
     * The wait duration increases exponentially between attempts using this multiplier. Formula:
     * {@code actualWaitTime = baseWaitTime * (multiplier ^ attemptNumber)}
     * </p>
     * <p>
     * <strong>Example with 500ms base wait and multiplier 2:</strong>
     * </p>
     * <ul>
     * <li>Attempt 1: 500ms wait</li>
     * <li>Attempt 2: 1000ms wait</li>
     * <li>Attempt 3: 2000ms wait</li>
     * </ul>
     * @param retryWaitDurationExponentialBackoffMultiplier exponential backoff multiplier (must be
     *          &gt;= 1)
     * @return this builder instance for method chaining
     */
    public Builder retryWaitDurationExponentialBackoffMultiplier(
        int retryWaitDurationExponentialBackoffMultiplier) {
      this.retryWaitDurationExponentialBackoffMultiplier = retryWaitDurationExponentialBackoffMultiplier;
      return this;
    }

    /**
     * Sets the list of exception classes that trigger retry attempts.
     * <p>
     * Only exceptions that match or inherit from the classes in this list will trigger retry
     * attempts. This parameter supports inheritance - subclasses of the specified exceptions will
     * also trigger retries.
     * </p>
     * <p>
     * <strong>Default:</strong> {@link JedisConnectionException}
     * </p>
     * @param retryIncludedExceptionList list of exception classes that should be retried
     * @return this builder instance for method chaining
     */
    public Builder retryIncludedExceptionList(List<Class> retryIncludedExceptionList) {
      this.retryIncludedExceptionList = retryIncludedExceptionList;
      return this;
    }

    /**
     * Sets the list of exception classes that are ignored for retry purposes.
     * <p>
     * Exceptions that match or inherit from the classes in this list will not trigger retry
     * attempts, even if they are included in the retry included exception list. This allows for
     * fine-grained control over retry behavior.
     * </p>
     * @param retryIgnoreExceptionList list of exception classes to ignore for retries
     * @return this builder instance for method chaining
     */
    public Builder retryIgnoreExceptionList(List<Class> retryIgnoreExceptionList) {
      this.retryIgnoreExceptionList = retryIgnoreExceptionList;
      return this;
    }

    // ============ Circuit Breaker Configuration Methods ============

    /**
     * Sets the failure rate threshold percentage that triggers circuit breaker activation.
     * <p>
     * When the failure rate equals or exceeds this threshold, the circuit breaker transitions to
     * the OPEN state and starts short-circuiting calls, enabling immediate failover to the next
     * available cluster.
     * </p>
     * <p>
     * <strong>Typical Values:</strong>
     * </p>
     * <ul>
     * <li><strong>30-40%:</strong> Aggressive failover for high-availability scenarios</li>
     * <li><strong>50%:</strong> Balanced approach (default)</li>
     * <li><strong>70-80%:</strong> Conservative failover to avoid false positives</li>
     * </ul>
     * @param circuitBreakerFailureRateThreshold failure rate threshold as percentage (0.0 to 100.0)
     * @return this builder instance for method chaining
     */
    public Builder circuitBreakerFailureRateThreshold(float circuitBreakerFailureRateThreshold) {
      checkThresholds(thresholdMinNumOfFailures, circuitBreakerFailureRateThreshold);
      this.circuitBreakerFailureRateThreshold = circuitBreakerFailureRateThreshold;
      return this;
    }

    /**
     * Sets the size of the sliding window for circuit breaker calculations.
     * @param circuitBreakerSlidingWindowSize sliding window size
     * @return this builder instance for method chaining
     */
    public Builder circuitBreakerSlidingWindowSize(int circuitBreakerSlidingWindowSize) {
      this.circuitBreakerSlidingWindowSize = circuitBreakerSlidingWindowSize;
      return this;
    }

    /**
     * Sets the minimum number of failures before circuit breaker is tripped.
     * <p>
     * When the number of failures exceeds this threshold, the circuit breaker will trip and prevent
     * further requests from being sent to the cluster until it has recovered.
     * </p>
     * <p>
     * <strong>Default:</strong> 1000
     * </p>
     * @param thresholdMinNumOfFailures minimum number of failures before circuit breaker is tripped
     * @return this builder instance for method chaining
     */
    public Builder thresholdMinNumOfFailures(int thresholdMinNumOfFailures) {
      checkThresholds(thresholdMinNumOfFailures, circuitBreakerFailureRateThreshold);
      this.thresholdMinNumOfFailures = thresholdMinNumOfFailures;
      return this;
    }

    private void checkThresholds(int failures, float rate) {
      if (failures == 0 && rate == 0) {
        throw new JedisValidationException(
            "Both thresholdMinNumOfFailures and circuitBreakerFailureRateThreshold can not be 0!");
      }
    }

    /**
     * Sets the list of exception classes that are recorded as circuit breaker failures.
     * <p>
     * Only exceptions matching or inheriting from these classes will count as failures for circuit
     * breaker calculations. This allows fine-grained control over which errors should trigger
     * failover.
     * </p>
     * @param circuitBreakerIncludedExceptionList list of exception classes that count as failures
     * @return this builder instance for method chaining
     */
    public Builder circuitBreakerIncludedExceptionList(
        List<Class> circuitBreakerIncludedExceptionList) {
      this.circuitBreakerIncludedExceptionList = circuitBreakerIncludedExceptionList;
      return this;
    }

    /**
     * Sets the list of exception classes that are ignored by the circuit breaker.
     * <p>
     * Exceptions matching or inheriting from these classes will not affect circuit breaker failure
     * rate calculations, even if they are included in the included exception list.
     * </p>
     * @param circuitBreakerIgnoreExceptionList list of exception classes to ignore
     * @return this builder instance for method chaining
     */
    public Builder circuitBreakerIgnoreExceptionList(
        List<Class> circuitBreakerIgnoreExceptionList) {
      this.circuitBreakerIgnoreExceptionList = circuitBreakerIgnoreExceptionList;
      return this;
    }

    /**
     * Sets the list of exception classes that trigger immediate fallback to next cluster.
     * @param circuitBreakerFallbackExceptionList list of exception classes that trigger fallback
     * @return this builder instance for method chaining
     * @deprecated Use {@link #fallbackExceptionList(java.util.List)} instead.
     */
    @Deprecated
    public Builder circuitBreakerFallbackExceptionList(
        List<Class<? extends Throwable>> circuitBreakerFallbackExceptionList) {
      return fallbackExceptionList(circuitBreakerFallbackExceptionList);
    }

    /**
     * Sets the list of exception classes that trigger immediate fallback to the next available
     * cluster.
     * <p>
     * When these exceptions occur, the system will immediately attempt to failover to the next
     * available cluster without waiting for circuit breaker thresholds. This enables fast failover
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
     * retry settings on the new cluster. When disabled, failed commands during failover will
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
     * Sets whether automatic failback to higher-priority clusters is supported.
     * <p>
     * When enabled, the system will automatically monitor failed clusters using health checks and
     * failback to higher-priority (higher weight) clusters when they recover. When disabled,
     * failback must be triggered manually.
     * </p>
     * <p>
     * <strong>Requirements for automatic failback:</strong>
     * </p>
     * <ul>
     * <li>Health checks must be enabled on cluster configurations</li>
     * <li>Grace period must elapse after cluster becomes unhealthy</li>
     * <li>Higher-priority cluster must pass health checks</li>
     * </ul>
     * @param supported true to enable automatic failback, false for manual failback only
     * @return this builder instance for method chaining
     */
    public Builder failbackSupported(boolean supported) {
      this.isFailbackSupported = supported;
      return this;
    }

    /**
     * Sets the interval between checks for failback opportunities to recovered clusters.
     * <p>
     * This controls how frequently the system checks if a higher-priority cluster has recovered and
     * is available for failback. Lower values provide faster failback response but increase
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
     * Sets the grace period to keep clusters disabled after they become unhealthy.
     * <p>
     * After a cluster is marked as unhealthy, it remains disabled for this grace period before
     * being eligible for failback, even if health checks indicate recovery. This prevents rapid
     * oscillation between clusters during intermittent failures.
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
     * Sets whether to forcefully terminate connections during failover for faster cluster
     * switching.
     * <p>
     * When enabled, existing connections to the failed cluster are immediately closed during
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
     * cluster before giving up. For example, if set to 3, the system will make 1 initial attempt
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
     * different cluster. For example, if set to 1000, the system will wait 1 second before
     * attempting to failover to a different cluster.
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
     * Builds and returns a new MultiClusterClientConfig instance with all configured settings.
     * <p>
     * This method creates the final configuration object by copying all builder settings to the
     * configuration instance. The builder can be reused after calling build() to create additional
     * configurations with different settings.
     * </p>
     * @return a new MultiClusterClientConfig instance with the configured settings
     */
    public MultiClusterClientConfig build() {
      MultiClusterClientConfig config = new MultiClusterClientConfig(this.clusterConfigs);

      // Copy retry configuration
      config.retryMaxAttempts = this.retryMaxAttempts;
      config.retryWaitDuration = Duration.ofMillis(this.retryWaitDuration);
      config.retryWaitDurationExponentialBackoffMultiplier = this.retryWaitDurationExponentialBackoffMultiplier;
      config.retryIncludedExceptionList = this.retryIncludedExceptionList;
      config.retryIgnoreExceptionList = this.retryIgnoreExceptionList;

      // Copy circuit breaker configuration
      config.thresholdMinNumOfFailures = this.thresholdMinNumOfFailures;
      config.circuitBreakerFailureRateThreshold = this.circuitBreakerFailureRateThreshold;
      config.circuitBreakerSlidingWindowSize = this.circuitBreakerSlidingWindowSize;
      config.circuitBreakerIncludedExceptionList = this.circuitBreakerIncludedExceptionList;
      config.circuitBreakerIgnoreExceptionList = this.circuitBreakerIgnoreExceptionList;

      // Copy fallback and failover configuration
      config.fallbackExceptionList = this.fallbackExceptionList;
      config.retryOnFailover = this.retryOnFailover;
      config.isFailbackSupported = this.isFailbackSupported;
      config.failbackCheckInterval = this.failbackCheckInterval;
      config.gracePeriod = this.gracePeriod;
      config.fastFailover = this.fastFailover;
      config.maxNumFailoverAttempts = this.maxNumFailoverAttempts;
      config.delayInBetweenFailoverAttempts = this.delayInBetweenFailoverAttempts;

      return config;
    }

  }

}