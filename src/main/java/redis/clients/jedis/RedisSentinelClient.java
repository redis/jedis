package redis.clients.jedis;

import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
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
public class RedisSentinelClient extends BaseRedisClient implements AutoCloseable {

  private final CommandExecutor executor;
  private final SentineledConnectionProvider provider;
  private final CommandObjects commandObjects;
  private final Cache cache;

  /**
   * Package-private constructor for builder.
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
      if (builder.masterName == null || builder.sentinels == null || builder.sentinels.isEmpty()) {
        throw new IllegalArgumentException("Master name and sentinels must be provided");
      }

      if (builder.cache != null) {
        this.provider = new SentineledConnectionProvider(builder.masterName, builder.masterConfig,
            builder.cache, builder.poolConfig, builder.sentinels, builder.sentinelConfig,
            builder.subscribeRetryWaitTimeMillis);
      } else {
        this.provider = new SentineledConnectionProvider(builder.masterName, builder.masterConfig,
            builder.poolConfig, builder.sentinels, builder.sentinelConfig,
            builder.subscribeRetryWaitTimeMillis);
      }
    }

    this.executor = new DefaultCommandExecutor(provider);
    this.commandObjects = new CommandObjects();

    if (builder.masterConfig.getRedisProtocol() != null) {
      this.commandObjects.setProtocol(builder.masterConfig.getRedisProtocol());
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
   * Gets the current master host and port.
   * @return the current master HostAndPort
   */
  public HostAndPort getCurrentMaster() {
    return provider.getCurrentMaster();
  }

  // Client-specific methods (same as RedisClient)

  /**
   * Creates a new pipeline for batching commands.
   * @return a new Pipeline instance
   */
  public Pipeline pipelined() {
    return new Pipeline(provider.getConnection(), true, commandObjects);
  }

  /**
   * Creates a new transaction (MULTI/EXEC block).
   * @return a new Transaction instance
   */
  public Transaction multi() {
    return new Transaction(provider.getConnection(), true, true, commandObjects);
  }

  /**
   * Creates a new transaction with optional MULTI command.
   * @param doMulti whether to execute MULTI command
   * @return a new Transaction instance
   */
  public Transaction transaction(boolean doMulti) {
    return new Transaction(provider.getConnection(), doMulti, true, commandObjects);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.executor);
  }

  /**
   * Creates a new builder for configuring a RedisSentinelClient.
   * @return a new Builder instance
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
    private String masterName = null;
    private Set<HostAndPort> sentinels = null;
    private long subscribeRetryWaitTimeMillis = 5000; // DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS

    // Client configurations
    private JedisClientConfig masterConfig = DefaultJedisClientConfig.builder().build();
    private JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder().build();

    private Builder() {
    }

    @Override
    protected Builder self() {
      return this;
    }

    /**
     * Sets the Redis master name as configured in Sentinel.
     * @param masterName the master name
     * @return this builder
     */
    public Builder masterName(String masterName) {
      this.masterName = masterName;
      return this;
    }

    /**
     * Sets the set of sentinel nodes.
     * @param sentinels the sentinel nodes
     * @return this builder
     */
    public Builder sentinels(Set<HostAndPort> sentinels) {
      this.sentinels = sentinels;
      return this;
    }

    /**
     * Sets the master client configuration.
     * <p>
     * Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations with
     * authentication, SSL, timeouts, and other Redis client settings for the master connection.
     * @param masterConfig the master client configuration
     * @return this builder
     */
    public Builder masterConfig(JedisClientConfig masterConfig) {
      this.masterConfig = masterConfig;
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
     * @param subscribeRetryWaitTimeMillis the retry wait time
     * @return this builder
     */
    public Builder subscribeRetryWaitTimeMillis(long subscribeRetryWaitTimeMillis) {
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
      return this;
    }

    /**
     * Builds the RedisSentinelClient instance.
     * @return a new RedisSentinelClient instance
     */
    public RedisSentinelClient build() {
      return new RedisSentinelClient(this);
    }
  }
}
