package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
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
public abstract class AbstractRedisClientBuilder<T extends AbstractRedisClientBuilder<T, C>, C> {

  // Common configuration fields
  protected GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
  protected Cache cache = null;
  protected ConnectionProvider connectionProvider = null;
  protected CommandKeyArgumentPreProcessor keyPreProcessor = null;
  protected JsonObjectMapper jsonObjectMapper = null;
  protected int searchDialect = SearchProtocol.DEFAULT_DIALECT;

  /**
   * Returns the concrete builder instance for method chaining. This method must be implemented by
   * subclasses to return their own type.
   * @return the concrete builder instance
   */
  protected abstract T self();

  /**
   * Builds the Redis client instance. This method must be implemented by subclasses to create their
   * specific client type.
   * @return the configured Redis client instance
   */
  public abstract C build();

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
  @Experimental
  public T cache(Cache cache) {
    this.cache = cache;
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
   * Sets a key preprocessor for transforming Redis keys before sending commands.
   * <p>
   * The key preprocessor allows you to modify keys before they are sent to Redis, for example to
   * add prefixes, apply transformations, or implement key routing logic.
   * <p>
   * Example usage:
   *
   * <pre>{@code
   * CommandKeyArgumentPreProcessor keyProcessor = key -> "myapp:" + key;
   * builder.keyPreProcessor(keyProcessor);
   * }</pre>
   *
   * @param keyPreProcessor the key preprocessor
   * @return this builder
   */
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
   * <pre>{@code
   * JsonObjectMapper customMapper = new MyCustomJsonMapper();
   * builder.jsonObjectMapper(customMapper);
   * }</pre>
   *
   * @param jsonObjectMapper the JSON object mapper
   * @return this builder
   */
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
   */
  public T searchDialect(int searchDialect) {
    if (searchDialect == 0) {
      throw new IllegalArgumentException("DIALECT=0 cannot be set.");
    }
    this.searchDialect = searchDialect;
    return self();
  }

  /**
   * Applies common configuration to the CommandObjects instance.
   * <p>
   * This method is called by concrete builders to configure the CommandObjects with the common
   * settings like key preprocessor, JSON mapper, and search dialect.
   * @param commandObjects the CommandObjects instance to configure
   */
  protected void applyCommonConfiguration(CommandObjects commandObjects) {
    if (keyPreProcessor != null) {
      commandObjects.setKeyArgumentPreProcessor(keyPreProcessor);
    }

    if (jsonObjectMapper != null) {
      commandObjects.setJsonObjectMapper(jsonObjectMapper);
    }

    if (searchDialect != SearchProtocol.DEFAULT_DIALECT) {
      commandObjects.setDefaultSearchDialect(searchDialect);
    }
  }

  /**
   * Validates common configuration parameters.
   * <p>
   * This method can be called by concrete builders to validate the common configuration before
   * building the client.
   * @throws IllegalArgumentException if any common configuration is invalid
   */
  protected void validateCommonConfiguration() {
    if (poolConfig == null) {
      throw new IllegalArgumentException("Pool configuration cannot be null");
    }
  }
}
