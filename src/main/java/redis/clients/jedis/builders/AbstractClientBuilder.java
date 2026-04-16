package redis.clients.jedis.builders;

import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;

/**
 * Abstract base class for Redis client builders that provides common configuration options.
 * <p>
 * This class contains shared configuration fields and methods that are common across different
 * Redis client builders (RedisClient.Builder, RedisSentinelClient.Builder, etc.). It helps
 * eliminate code duplication and provides a consistent API for common features.
 * <p>
 * Common features provided:
 * <ul>
 * <li>Connection pool configuration</li>
 * <li>Client-side caching</li>
 * <li>Custom connection providers</li>
 * <li>Key preprocessing</li>
 * <li>JSON object mapping</li>
 * <li>Search dialect configuration</li>
 * </ul>
 * @param <T> the concrete builder type for method chaining
 * @param <C> the client type that this builder creates
 */
public abstract class AbstractClientBuilder<T extends AbstractClientBuilder<T, C>, C> {

  // Common configuration fields
  protected GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
  protected Cache cache = null;
  protected CacheConfig cacheConfig = null;
  protected CommandExecutor commandExecutor = null;
  protected ConnectionProvider connectionProvider = null;

  /**
   * @deprecated This field is deprecated and should be set on JedisClientConfig instead.
   */
  @Deprecated
  protected CommandKeyArgumentPreProcessor keyPreProcessor = null;
  /**
   * @deprecated This field is deprecated and should be set on JedisClientConfig instead.
   */
  @Deprecated
  protected JsonObjectMapper jsonObjectMapper = null;
  /**
   * @deprecated This field is deprecated and should be set on JedisClientConfig instead.
   */
  @Deprecated
  protected int searchDialect = SearchProtocol.DEFAULT_DIALECT;

  // Default retry settings based on RecommendedSettings
  protected int maxAttempts = 5;
  protected Duration maxTotalRetriesDuration = Duration.ofSeconds(10);

  protected JedisClientConfig clientConfig = null;

  /**
   * Sets the maximum number of retry attempts for command execution.
   * <p>
   * When a command fails due to a connection error, the executor will retry up to this many times
   * before giving up. Default is 5.
   * @param maxAttempts the maximum number of attempts (must be positive)
   * @return this builder
   */
  public T maxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return self();
  }

  /**
   * Sets the maximum total duration for all retry attempts.
   * <p>
   * This provides a time-based limit on retries in addition to the attempt-based limit. Default is
   * 10 seconds.
   * @param maxTotalRetriesDuration the maximum total retry duration
   * @return this builder
   */
  public T maxTotalRetriesDuration(Duration maxTotalRetriesDuration) {
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
    return self();
  }

  /**
   * Sets the client configuration for Redis connections.
   * <p>
   * The client configuration includes authentication, timeouts, SSL settings, and other
   * connection-specific parameters.
   * @param clientConfig the client configuration
   * @return this builder
   */
  public T clientConfig(JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    return self();
  }

  /**
   * Returns the concrete builder instance for method chaining. This method must be implemented by
   * subclasses to return their own type.
   * @return the concrete builder instance
   */
  protected abstract T self();

  /**
   * Creates a default connection provider based on the current configuration.
   * @return ConnectionProvider
   */
  protected abstract ConnectionProvider createDefaultConnectionProvider();

  /**
   * Creates a default command executor based on the current configuration.
   * @return CommandExecutor
   */
  protected CommandExecutor createDefaultCommandExecutor() {
    return new DefaultCommandExecutor(this.connectionProvider, this.maxAttempts,
        this.maxTotalRetriesDuration);
  }

  /**
   * Creates a default client configuration based on the current configuration. Any deprecated
   * builder-level overrides ({@link #keyPreProcessor}, {@link #jsonObjectMapper},
   * {@link #searchDialect}) are folded into the resulting config so the client constructor sees a
   * single source of truth.
   * @return JedisClientConfig
   */
  protected JedisClientConfig createDefaultClientConfig() {
    return applyDeprecatedCommandObjectFields(DefaultJedisClientConfig.builder()).build();
  }

  /**
   * Layers the deprecated builder-level {@link CommandObjects} knobs onto a
   * {@link DefaultJedisClientConfig.Builder}. Used both when creating a default config from scratch
   * and when augmenting a user-supplied config in {@link #build()}.
   */
  private DefaultJedisClientConfig.Builder applyDeprecatedCommandObjectFields(
      DefaultJedisClientConfig.Builder configBuilder) {
    if (this.keyPreProcessor != null) {
      configBuilder.commandKeyArgumentPreProcessor(this.keyPreProcessor);
    }
    if (this.jsonObjectMapper != null) {
      configBuilder.jsonObjectMapper(this.jsonObjectMapper);
    }
    if (this.searchDialect != SearchProtocol.DEFAULT_DIALECT) {
      configBuilder.searchDialect(this.searchDialect);
    }
    return configBuilder;
  }

  private boolean hasDeprecatedCommandObjectOverrides() {
    return this.keyPreProcessor != null || this.jsonObjectMapper != null
        || this.searchDialect != SearchProtocol.DEFAULT_DIALECT;
  }

  /**
   * Creates the specific client instance with the provided components.
   * <p>
   * This method is called by the generic build() method to instantiate the concrete client type.
   * Each builder implementation should create their specific client type (RedisClient,
   * RedisClusterClient, etc.) using the parameters provided to the builder.
   * @return the configured Redis client instance
   */
  protected abstract C createClient();

  /**
   * Validates the builder-specific configuration.
   * <p>
   * This method is called by the generic build() method to validate configuration specific to each
   * builder type. Implementations should call validateCommonConfiguration() and then perform their
   * own specific validation.
   * @throws IllegalArgumentException if the configuration is invalid
   */
  protected abstract void validateSpecificConfiguration();

  /**
   * Builds the Redis client instance using the common build pattern.
   * <p>
   * This method implements the common build pattern shared across all builder types:
   * <ol>
   * <li>Validates configuration (both common and builder-specific)</li>
   * <li>Creates cache from cacheConfig if provided</li>
   * <li>Creates default connection provider if not already set</li>
   * <li>Creates default command executor if not already set</li>
   * <li>Creates and returns the specific client instance — the client owns {@link CommandObjects}
   * construction via its factory hook.</li>
   * </ol>
   * @return the configured Redis client instance
   */
  public C build() {
    // Validate configuration
    validateSpecificConfiguration();

    // Create cache from config if provided
    if (this.cacheConfig != null) {
      this.cache = CacheFactory.getCache(this.cacheConfig);
    }

    if (this.clientConfig == null) {
      this.clientConfig = createDefaultClientConfig();
    } else if (hasDeprecatedCommandObjectOverrides()) {
      // User supplied a clientConfig AND used the deprecated builder-level setters — layer the
      // setters on top so the explicit builder call wins over whatever the supplied config carried.
      this.clientConfig = applyDeprecatedCommandObjectFields(
        DefaultJedisClientConfig.builder().from(this.clientConfig)).build();
    }

    // Create default connection provider if not set
    if (this.connectionProvider == null) {
      this.connectionProvider = createDefaultConnectionProvider();
    }

    // Create default command executor if not set
    if (this.commandExecutor == null) {
      this.commandExecutor = createDefaultCommandExecutor();
    }

    // Create and return the specific client instance
    return createClient();
  }

  /**
   * Sets the connection pool configuration.
   * <p>
   * The pool configuration controls how connections are managed, including maximum number of
   * connections, idle timeout, and other pooling parameters.
   * @param poolConfig the pool configuration
   * @return this builder
   */
  public T poolConfig(GenericObjectPoolConfig<Connection> poolConfig) {
    this.poolConfig = poolConfig;
    return self();
  }

  /**
   * Sets the client-side cache for caching Redis responses.
   * <p>
   * Client-side caching can improve performance by storing frequently accessed data locally,
   * reducing the number of round trips to the Redis server.
   * @param cache the cache instance
   * @return this builder
   */
  public T cache(Cache cache) {
    this.cache = cache;
    return self();
  }

  /**
   * Sets the cache configuration for client-side caching.
   * <p>
   * Client-side caching can improve performance by storing frequently accessed data locally. The
   * cache will be created from this configuration during the build process.
   * @param cacheConfig the cache configuration
   * @return this builder
   */
  public T cacheConfig(CacheConfig cacheConfig) {
    this.cacheConfig = cacheConfig;
    return self();
  }

  /**
   * Sets a custom connection provider.
   * <p>
   * When a custom connection provider is set, other connection-related configuration may be ignored
   * as the provider is responsible for managing connections. The specific behavior depends on the
   * concrete builder implementation.
   * @param connectionProvider the connection provider
   * @return this builder
   */
  public T connectionProvider(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
    return self();
  }

  /**
   * Sets a custom command executor for executing Redis commands.
   * <p>
   * The command executor is responsible for sending commands to Redis and processing the responses.
   * @param commandExecutor the command executor
   * @return this builder
   */
  public T commandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return self();
  }

  /**
   * Sets a key preprocessor for transforming Redis keys before sending commands.
   * <p>
   * The key preprocessor allows you to modify keys before they are sent to Redis, for example to
   * add prefixes, apply transformations, or implement key routing logic.
   * <p>
   * Example usage:
   *
   * <pre>
   * {@code
   * CommandKeyArgumentPreProcessor keyProcessor = key -> "myapp:" + key;
   * builder.keyPreProcessor(keyProcessor);
   * }
   * </pre>
   *
   * @param keyPreProcessor the key preprocessor
   * @return this builder
   * @deprecated use
   *             {@link DefaultJedisClientConfig.Builder#commandKeyArgumentPreProcessor(CommandKeyArgumentPreProcessor)}
   *             on the {@link JedisClientConfig} passed via
   *             {@link #clientConfig(JedisClientConfig)}. When this setter is used it is folded
   *             into the resulting client config at {@link #build()} time.
   */
  @Deprecated
  public T keyPreProcessor(CommandKeyArgumentPreProcessor keyPreProcessor) {
    this.keyPreProcessor = keyPreProcessor;
    return self();
  }

  /**
   * Sets a custom JSON object mapper for JSON operations.
   * <p>
   * The JSON object mapper is used for serializing and deserializing objects in JSON commands
   * (RedisJSON module). If not set, a default Gson-based mapper will be used.
   * <p>
   * Example usage:
   *
   * <pre>
   * {
   *   &#64;code
   *   JsonObjectMapper customMapper = new MyCustomJsonMapper();
   *   builder.jsonObjectMapper(customMapper);
   * }
   * </pre>
   *
   * @param jsonObjectMapper the JSON object mapper
   * @return this builder
   * @deprecated use {@link DefaultJedisClientConfig.Builder#jsonObjectMapper(JsonObjectMapper)} on
   *             the {@link JedisClientConfig} passed via {@link #clientConfig(JedisClientConfig)}.
   *             When this setter is used it is folded into the resulting client config at
   *             {@link #build()} time.
   */
  @Deprecated
  public T jsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
    this.jsonObjectMapper = jsonObjectMapper;
    return self();
  }

  /**
   * Sets the default search dialect for RediSearch operations.
   * <p>
   * The search dialect determines the query syntax and features available for RediSearch commands.
   * Different dialects support different query features and syntax variations.
   * <p>
   * Default is {@value redis.clients.jedis.search.SearchProtocol#DEFAULT_DIALECT}.
   * @param searchDialect the search dialect version
   * @return this builder
   * @throws IllegalArgumentException if dialect is 0 (not allowed)
   * @deprecated use {@link DefaultJedisClientConfig.Builder#searchDialect(int)} on the
   *             {@link JedisClientConfig} passed via {@link #clientConfig(JedisClientConfig)}. When
   *             this setter is used it is folded into the resulting client config at
   *             {@link #build()} time.
   */
  @Deprecated
  public T searchDialect(int searchDialect) {
    if (searchDialect == 0) {
      throw new IllegalArgumentException("DIALECT=0 cannot be set.");
    }
    this.searchDialect = searchDialect;
    return self();
  }

  /**
   * Validates common configuration parameters.
   * <p>
   * This method can be called by concrete builders to validate the common configuration before
   * building the client.
   * @throws IllegalArgumentException if any common configuration is invalid
   */
  protected void validateCommonConfiguration() {
    if (cache != null || cacheConfig != null) {
      if (clientConfig != null && !canNegotiateResp3(clientConfig)) {
        throw new IllegalArgumentException("Client-side caching is only supported with RESP3.");
      }
    }
  }

  /**
   * Whether the supplied config can result in a RESP3 connection: either the user explicitly
   * requested RESP3, or the protocol is unspecified and auto-negotiation is enabled.
   */
  private static boolean canNegotiateResp3(JedisClientConfig config) {
    RedisProtocol protocol = config.getRedisProtocol();
    if (protocol == RedisProtocol.RESP3) return true;
    return protocol == null && config.isAutoNegotiateProtocol();
  }
}
