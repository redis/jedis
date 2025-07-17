package redis.clients.jedis;

import java.util.Set;

import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;
import redis.clients.jedis.util.IOUtils;

/**
 * Redis Sentinel client that provides a clean, developer-friendly API for Redis operations with
 * automatic master discovery and failover through Redis Sentinel.
 * <p>
 * This client extends {@link BaseRedisClient} to inherit all Redis command implementations and
 * provides a Builder pattern for configuration. It uses {@link SentineledConnectionProvider} for
 * automatic master discovery and failover.
 * <p>
 * Key features:
 * <ul>
 * <li>Automatic master discovery through Redis Sentinel</li>
 * <li>Automatic failover when master becomes unavailable</li>
 * <li>Builder pattern for complex configuration</li>
 * <li>Automatic connection pooling and resource management</li>
 * <li>Full Redis command support including modules</li>
 * <li>Pipeline and transaction support</li>
 * </ul>
 * <p>
 * Basic usage:
 *
 * <pre>{@code
 * // Simple sentinel configuration
 * Set<HostAndPort> sentinels = Set.of(new HostAndPort("sentinel1", 26379),
 *   new HostAndPort("sentinel2", 26379));
 *
 * try (RedisSentinelClient client = RedisSentinelClient.builder().masterName("mymaster")
 *     .sentinels(sentinels).build()) {
 *   client.set("key", "value");
 *   String value = client.get("key");
 * }
 *
 * // Advanced configuration
 * JedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("secret")
 *     .database(1).build();
 * JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder().password("sentinel-secret")
 *     .build();
 *
 * try (RedisSentinelClient client = RedisSentinelClient.builder().masterName("mymaster")
 *     .sentinels(sentinels).masterConfig(masterConfig).sentinelConfig(sentinelConfig).build()) {
 *   client.set("key", "value");
 * }
 * }</pre>
 *
 * @see BaseRedisClient
 * @see SentineledConnectionProvider
 * @see Builder
 */
public class RedisSentinelClient extends BaseRedisClient {

  private final CommandExecutor executor;
  private final SentineledConnectionProvider provider;
  private final CommandObjects commandObjects;
  private final Cache cache;

  /**
   * Package-private constructor for builder.
   * <p>
   * Creates a new RedisSentinelClient instance using the configuration provided by the builder.
   * This constructor initializes the connection provider, command executor, and other components
   * needed for Redis Sentinel operations.
   * @param builder the builder containing the configuration
   */
  RedisSentinelClient(Builder builder) {
    // Use custom connection provider if provided, otherwise create default sentineled provider
    if (builder.connectionProvider != null) {
      if (!(builder.connectionProvider instanceof SentineledConnectionProvider)) {
        throw new IllegalArgumentException(
            "ConnectionProvider must be a SentineledConnectionProvider");
      }
      this.provider = (SentineledConnectionProvider) builder.connectionProvider;
    } else {
      if (builder.cache != null) {
        this.provider = new SentineledConnectionProvider(builder.primaryName, builder.primaryConfig,
            builder.cache, builder.poolConfig, builder.sentinels, builder.sentinelConfig,
            builder.subscribeRetryWaitTimeMillis);
      } else {
        this.provider = new SentineledConnectionProvider(builder.primaryName, builder.primaryConfig,
            builder.poolConfig, builder.sentinels, builder.sentinelConfig,
            builder.subscribeRetryWaitTimeMillis);
      }
    }

    this.executor = new DefaultCommandExecutor(provider);
    this.commandObjects = new CommandObjects();

    if (builder.primaryConfig.getRedisProtocol() != null) {
      this.commandObjects.setProtocol(builder.primaryConfig.getRedisProtocol());
    }

    if (builder.cache != null) {
      this.cache = builder.cache;
    } else {
      this.cache = null;
    }

    // Apply common configuration from the abstract base class
    builder.applyCommonConfiguration(this.commandObjects);
  }

  @Override
  protected CommandObjects getCommandObjects() {
    return commandObjects;
  }

  @Override
  protected ConnectionProvider getConnectionProvider() {
    return provider;
  }

  @Override
  public Cache getCache() {
    return cache;
  }

  @Override
  public <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  @Override
  public <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executor.broadcastCommand(commandObject);
  }

  @Override
  protected <T> T checkAndBroadcastCommand(CommandObject<T> commandObject) {
    // For sentinel Redis, broadcast and execute are the same
    return executeCommand(commandObject);
  }

  // Sentinel-specific methods

  /**
   * Gets the current master host and port as discovered by Redis Sentinel.
   * <p>
   * This method returns the current master server information as determined by the
   * sentinel monitoring system. The master may change during failover operations.
   * @return the current master HostAndPort, or null if no master is currently available
   */
  public HostAndPort getCurrentMaster() {
    return provider.getCurrentMaster();
  }

  // Client-specific methods (same as RedisClient)

  /**
   * Creates a new pipeline for batching commands.
   * <p>
   * Pipelines allow you to send multiple commands to Redis without waiting for individual
   * responses, improving performance for bulk operations. The pipeline will automatically
   * use the current master connection as determined by Redis Sentinel.
   * @return a new Pipeline instance for batching commands
   */
  public Pipeline pipelined() {
    return new Pipeline(provider.getConnection(), true, commandObjects);
  }

  /**
   * Creates a new transaction (MULTI/EXEC block).
   * <p>
   * Transactions ensure that a group of commands are executed atomically. This method
   * automatically executes the MULTI command to start the transaction. The transaction
   * will use the current master connection as determined by Redis Sentinel.
   * @return a new Transaction instance for atomic command execution
   */
  public Transaction multi() {
    return new Transaction(provider.getConnection(), true, true, commandObjects);
  }

  /**
   * Creates a new transaction with optional MULTI command.
   * <p>
   * This method provides more control over transaction initialization. When doMulti is false,
   * you can manually control when to start the transaction. The transaction will use the
   * current master connection as determined by Redis Sentinel.
   * @param doMulti whether to automatically execute the MULTI command to start the transaction
   * @return a new Transaction instance for atomic command execution
   */
  public Transaction transaction(boolean doMulti) {
    return new Transaction(provider.getConnection(), doMulti, true, commandObjects);
  }

  /**
   * Closes the client and releases all associated resources.
   * <p>
   * This method closes the command executor and all underlying connections.
   * After calling this method, the client should not be used for any operations.
   * This method is idempotent and can be called multiple times safely.
   */
  @Override
  public void close() {
    IOUtils.closeQuietly(this.executor);
  }

  /**
   * Creates a new builder for configuring a RedisSentinelClient.
   * <p>
   * The builder provides a fluent API for configuring all aspects of the Redis Sentinel client,
   * including master name, sentinel nodes, authentication, connection pooling, and more.
   * @return a new Builder instance for configuring the client
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for configuring RedisSentinelClient instances.
   * <p>
   * Provides a fluent API for setting sentinel configuration and advanced Redis client features.
   * For detailed client configuration (authentication, SSL, timeouts, etc.), use
   * {@link DefaultJedisClientConfig.Builder} and pass the result to
   * {@link #masterConfig(JedisClientConfig)} and {@link #sentinelConfig(JedisClientConfig)}.
   * <p>
   * Example usage:
   *
   * <pre>{@code
   * // Simple configuration
   * Set<HostAndPort> sentinels = Set.of(new HostAndPort("sentinel1", 26379),
   *   new HostAndPort("sentinel2", 26379));
   * RedisSentinelClient client = RedisSentinelClient.builder().masterName("mymaster")
   *     .sentinels(sentinels).build();
   *
   * // Advanced configuration with custom configs
   * JedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("secret")
   *     .database(1).ssl(true).connectionTimeoutMillis(5000).socketTimeoutMillis(10000).build();
   *
   * JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder().password("sentinel-secret")
   *     .connectionTimeoutMillis(3000).build();
   *
   * RedisSentinelClient client = RedisSentinelClient.builder().masterName("mymaster")
   *     .sentinels(sentinels).masterConfig(masterConfig).sentinelConfig(sentinelConfig)
   *     .subscribeRetryWaitTimeMillis(10000).build();
   * }</pre>
   */
  public static class Builder extends AbstractRedisClientBuilder<Builder, RedisSentinelClient> {
    // Sentinel-specific configuration
    private String primaryName = null;
    private Set<HostAndPort> sentinels = null;
    private long subscribeRetryWaitTimeMillis = 5000; // DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS

    // Client configurations
    private JedisClientConfig primaryConfig = DefaultJedisClientConfig.builder().build();
    private JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder().build();

    private Builder() {
    }

    @Override
    protected Builder self() {
      return this;
    }

    /**
     * Sets the primary (master) name and client configuration.
     * <p>
     * Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations with
     * authentication, SSL, timeouts, and other Redis client settings for the master connection.
     * @param primaryName the name of the master as configured in Redis Sentinel
     * @param primaryConfig the client configuration for connecting to the master
     * @return this builder
     */
    public Builder primary(String primaryName, JedisClientConfig primaryConfig) {
      this.primaryName = primaryName;
      this.primaryConfig = primaryConfig;
      return this;
    }

    /**
     * Sets the master name.
     * <p>
     * This is a convenience method equivalent to calling {@code primary(masterName, DefaultJedisClientConfig.builder().build())}.
     * @param masterName the name of the master as configured in Redis Sentinel
     * @return this builder
     */
    public Builder masterName(String masterName) {
      this.primaryName = masterName;
      return this;
    }

    /**
     * Sets the master client configuration.
     * <p>
     * Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations with
     * authentication, SSL, timeouts, and other Redis client settings for the master connection.
     * @param masterConfig the client configuration for connecting to the master
     * @return this builder
     */
    public Builder masterConfig(JedisClientConfig masterConfig) {
      this.primaryConfig = masterConfig;
      return this;
    }

    /**
     * Sets the set of sentinel nodes.
     * <p>
     * These are the Redis Sentinel instances that will be used for master discovery
     * and failover monitoring. At least one sentinel must be provided.
     * @param sentinels the set of sentinel host and port combinations
     * @return this builder
     */
    public Builder sentinels(Set<HostAndPort> sentinels) {
      this.sentinels = sentinels;
      return this;
    }

    /**
     * Sets the sentinel client configuration.
     * <p>
     * Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations with
     * authentication, SSL, timeouts, and other Redis client settings for sentinel connections.
     * @param sentinelConfig the sentinel client configuration
     * @return this builder
     */
    public Builder sentinelConfig(JedisClientConfig sentinelConfig) {
      this.sentinelConfig = sentinelConfig;
      return this;
    }

    /**
     * Sets a custom connection provider.
     * <p>
     * When provided, this connection provider will be used instead of creating a new
     * SentineledConnectionProvider. The provider must be an instance of
     * SentineledConnectionProvider.
     * @param connectionProvider the connection provider (must be SentineledConnectionProvider)
     * @return this builder
     */
    public Builder connectionProvider(SentineledConnectionProvider connectionProvider) {
      super.connectionProvider(connectionProvider);
      return this;
    }

    /**
     * Sets the subscribe retry wait time in milliseconds.
     * <p>
     * This controls how long to wait before retrying sentinel subscription operations
     * when they fail. Default is 5000 milliseconds (5 seconds).
     * @param subscribeRetryWaitTimeMillis the retry wait time in milliseconds
     * @return this builder
     */
    public Builder subscribeRetryWaitTimeMillis(long subscribeRetryWaitTimeMillis) {
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
      return this;
    }

    /**
     * Builds the RedisSentinelClient instance.
     * @return a new RedisSentinelClient instance
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public RedisSentinelClient build() {
      validateConfiguration();
      return new RedisSentinelClient(this);
    }

    /**
     * Validates the builder configuration.
     */
    private void validateConfiguration() {
      // Validate common configuration
      validateCommonConfiguration();

      // If custom connection provider is set, skip sentinel-specific validation
      if (connectionProvider != null) {
        return;
      }

      if (primaryName == null || primaryName.trim().isEmpty()) {
        throw new IllegalArgumentException("Primary name must be specified");
      }
      if (sentinels == null || sentinels.isEmpty()) {
        throw new IllegalArgumentException("At least one sentinel must be specified");
      }
      if (primaryConfig == null) {
        throw new IllegalArgumentException("Primary configuration cannot be null");
      }
      if (sentinelConfig == null) {
        throw new IllegalArgumentException("Sentinel configuration cannot be null");
      }
      if (subscribeRetryWaitTimeMillis < 0) {
        throw new IllegalArgumentException("Subscribe retry wait time must be non-negative");
      }
    }
  }
}
