package redis.clients.jedis;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * Redis client that provides a clean, developer-friendly API for Redis operations.
 *
 * <p>This client extends {@link BaseRedisClient} to inherit all Redis command implementations
 * and provides a Builder pattern for configuration instead of multiple constructors.
 *
 * <p>Key features:
 * <ul>
 *   <li>Clean API with minimal constructors</li>
 *   <li>Builder pattern for complex configuration</li>
 *   <li>Automatic connection pooling and resource management</li>
 *   <li>Full Redis command support including modules</li>
 *   <li>Pipeline and transaction support</li>
 *   <li>No deprecated features (Sharding/Sharded)</li>
 * </ul>
 *
 * <p>Basic usage:
 * <pre>{@code
 * // Simple connection
 * try (RedisClient client = new RedisClient("localhost", 6379)) {
 *     client.set("key", "value");
 *     String value = client.get("key");
 * }
 *
 * // With configuration
 * JedisClientConfig config = DefaultJedisClientConfig.builder()
 *         .password("secret")
 *         .database(1)
 *         .build();
 * try (RedisClient client = RedisClient.builder()
 *         .host("localhost")
 *         .port(6379)
 *         .config(config)
 *         .build()) {
 *     client.set("key", "value");
 * }
 * }</pre>
 *
 * @see BaseRedisClient
 * @see Builder
 */
public class RedisClient extends BaseRedisClient implements AutoCloseable {

    private final CommandExecutor executor;
    private final ConnectionProvider provider;
    private final CommandObjects commandObjects;
    private final Cache cache;

    /**
     * Creates a Redis client with default configuration connecting to localhost:6379.
     */
    public RedisClient() {
        this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
    }

    /**
     * Creates a Redis client connecting to the specified host and port.
     *
     * @param host the Redis server host
     * @param port the Redis server port
     */
    public RedisClient(String host, int port) {
        this(new HostAndPort(host, port));
    }

    /**
     * Creates a Redis client connecting to the specified host and port.
     *
     * @param hostAndPort the Redis server host and port
     */
    public RedisClient(HostAndPort hostAndPort) {
        this(builder().host(hostAndPort.getHost()).port(hostAndPort.getPort()));
    }

    /**
     * Creates a Redis client from a URI.
     *
     * @param uri the Redis URI (e.g., "redis://localhost:6379")
     */
    public RedisClient(final URI uri) {
        this(builder().fromURI(uri));
    }

    /**
     * Package-private constructor for builder.
     */
    RedisClient(Builder builder) {
        // Use custom connection provider if provided, otherwise create default pooled provider
        if (builder.connectionProvider != null) {
            this.provider = builder.connectionProvider;
        } else {
            this.provider = new PooledConnectionProvider(
                new HostAndPort(builder.host, builder.port),
                builder.clientConfig,
                builder.poolConfig
            );
        }

        this.executor = new DefaultCommandExecutor(provider);
        this.commandObjects = new CommandObjects();

        if (builder.clientConfig.getRedisProtocol() != null) {
            this.commandObjects.setProtocol(builder.clientConfig.getRedisProtocol());
        }

        if (builder.cache != null) {
            this.cache = builder.cache;
        } else {
            this.cache = null;
        }

        // Apply builder configuration to CommandObjects
        if (builder.keyPreProcessor != null) {
            this.commandObjects.setKeyArgumentPreProcessor(builder.keyPreProcessor);
        }

        if (builder.jsonObjectMapper != null) {
            this.commandObjects.setJsonObjectMapper(builder.jsonObjectMapper);
        }

        if (builder.searchDialect != SearchProtocol.DEFAULT_DIALECT) {
            this.commandObjects.setDefaultSearchDialect(builder.searchDialect);
        }
    }

    // Abstract method implementations from BaseRedisClient

    @Override
    protected CommandObjects getCommandObjects() {
        return commandObjects;
    }

    @Override
    protected ConnectionProvider getConnectionProvider() {
        return provider;
    }

    public Cache getCache() {
        return cache;
    }

    @Override
    protected <T> T executeCommand(CommandObject<T> commandObject) {
        return executor.executeCommand(commandObject);
    }

    @Override
    protected <T> T broadcastCommand(CommandObject<T> commandObject) {
        return executor.broadcastCommand(commandObject);
    }

    @Override
    protected <T> T checkAndBroadcastCommand(CommandObject<T> commandObject) {
        // For standalone Redis, broadcast and execute are the same
        return executeCommand(commandObject);
    }

    // Client-specific methods

    /**
     * Creates a new pipeline for batching commands.
     *
     * @return a new Pipeline instance
     */
    public Pipeline pipelined() {
        return new Pipeline(provider.getConnection(), true, commandObjects);
    }

    /**
     * Creates a new transaction (MULTI/EXEC block).
     *
     * @return a new Transaction instance
     */
    public Transaction multi() {
        return new Transaction(provider.getConnection(), true, true, commandObjects);
    }

    /**
     * Creates a new transaction with optional MULTI command.
     *
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
     * Creates a new builder for configuring a RedisClient.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring RedisClient instances.
     *
     * <p>Provides a fluent API for setting connection parameters and advanced Redis client features.
     * For detailed client configuration (authentication, SSL, timeouts, etc.), use
     * {@link DefaultJedisClientConfig.Builder} and pass the result to {@link #config(JedisClientConfig)}.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Simple configuration
     * RedisClient client = RedisClient.builder()
     *     .host("redis.example.com")
     *     .port(6379)
     *     .build();
     *
     * // Advanced configuration with custom client config
     * JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
     *     .password("secret")
     *     .database(1)
     *     .ssl(true)
     *     .connectionTimeoutMillis(5000)
     *     .socketTimeoutMillis(10000)
     *     .build();
     *
     * // Advanced configuration with custom pool config
     * ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
     * poolConfig.setMaxTotal(20);
     * poolConfig.setMaxIdle(10);
     *
     * RedisClient client = RedisClient.builder()
     *     .host("redis.example.com")
     *     .port(6379)
     *     .config(clientConfig)
     *     .poolConfig(poolConfig)
     *     .cache(cache)
     *     .build();
     *
     * // Custom connection provider
     * ConnectionProvider customProvider = new MyCustomConnectionProvider();
     * RedisClient client = RedisClient.builder()
     *     .connectionProvider(customProvider)
     *     .build();
     *
     * // Advanced configuration with custom processors and mappers
     * CommandKeyArgumentPreProcessor keyProcessor = new PrefixedKeyArgumentPreProcessor("myapp:");
     * JsonObjectMapper jsonMapper = new MyCustomJsonMapper();
     * RedisClient client = RedisClient.builder()
     *     .host("redis.example.com")
     *     .port(6379)
     *     .keyPreProcessor(keyProcessor)
     *     .jsonObjectMapper(jsonMapper)
     *     .searchDialect(3)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        // Connection details (not part of JedisClientConfig)
        private String host = "localhost";
        private int port = Protocol.DEFAULT_PORT;

        // Client configuration
        private JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();

        // Additional configuration not part of JedisClientConfig
        private GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
        private Cache cache = null;
        private ConnectionProvider connectionProvider = null;

        private CommandKeyArgumentPreProcessor keyPreProcessor = null;
        private JsonObjectMapper jsonObjectMapper = null;
        private int searchDialect = SearchProtocol.DEFAULT_DIALECT;

        private Builder() {
        }

        /**
         * Sets the Redis server host.
         *
         * @param host the host name or IP address
         * @return this builder
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the Redis server port.
         *
         * @param port the port number
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the complete client configuration.
         *
         * <p>Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations
         * with authentication, SSL, timeouts, and other Redis client settings.
         *
         * @param clientConfig the client configuration
         * @return this builder
         */
        public Builder config(JedisClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        /**
         * Sets the connection pool configuration.
         *
         * @param poolConfig the pool configuration
         * @return this builder
         */
        public Builder poolConfig(GenericObjectPoolConfig<Connection> poolConfig) {
            this.poolConfig = poolConfig;
            return this;
        }

        /**
         * Sets the client-side cache for caching Redis responses.
         *
         * @param cache the cache instance
         * @return this builder
         */
        public Builder cache(Cache cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets a custom connection provider.
         *
         * <p>When a custom connection provider is set, the host, port, clientConfig,
         * and poolConfig settings will be ignored as the provider is responsible
         * for managing connections.
         *
         * @param connectionProvider the connection provider
         * @return this builder
         */
        public Builder connectionProvider(ConnectionProvider connectionProvider) {
            this.connectionProvider = connectionProvider;
            return this;
        }

        /**
         * Sets a key preprocessor for transforming Redis keys before sending commands.
         *
         * <p>The key preprocessor allows you to modify keys before they are sent to Redis,
         * for example to add prefixes or perform other transformations.
         *
         * @param keyPreProcessor the key preprocessor
         * @return this builder
         */
        public Builder keyPreProcessor(CommandKeyArgumentPreProcessor keyPreProcessor) {
            this.keyPreProcessor = keyPreProcessor;
            return this;
        }

        /**
         * Sets a custom JSON object mapper for JSON operations.
         *
         * <p>The JSON object mapper is used for serializing and deserializing objects
         * in JSON commands. If not set, a default Gson-based mapper will be used.
         *
         * @param jsonObjectMapper the JSON object mapper
         * @return this builder
         */
        public Builder jsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
            this.jsonObjectMapper = jsonObjectMapper;
            return this;
        }

        /**
         * Sets the default search dialect for RediSearch operations.
         *
         * <p>The search dialect determines the query syntax and features available
         * for RediSearch commands. Default is {@value redis.clients.jedis.search.SearchProtocol#DEFAULT_DIALECT}.
         *
         * @param searchDialect the search dialect version
         * @return this builder
         * @throws IllegalArgumentException if dialect is 0 (not allowed)
         */
        public Builder searchDialect(int searchDialect) {
            if (searchDialect == 0) {
                throw new IllegalArgumentException("DIALECT=0 cannot be set.");
            }
            this.searchDialect = searchDialect;
            return this;
        }

        /**
         * Builds the RedisClient with the configured parameters.
         *
         * @return a new RedisClient instance
         * @throws IllegalArgumentException if the configuration is invalid
         */
        public RedisClient build() {
            validateConfiguration();
            return new RedisClient(this);
        }

        /**
         * Validates the builder configuration.
         */
        private void validateConfiguration() {
            // If custom connection provider is set, skip host/port/config validation
            if (connectionProvider != null) {
                return;
            }

            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be null or empty");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (clientConfig == null) {
                throw new IllegalArgumentException("Client configuration cannot be null");
            }
            if (poolConfig == null) {
                throw new IllegalArgumentException("Pool configuration cannot be null");
            }
        }

        /**
         * Configures the builder from a Redis URI.
         *
         * <p>Parses the URI to extract connection parameters including host, port,
         * authentication credentials, database, protocol, and SSL settings.
         *
         * <p>Supported URI formats:
         * <ul>
         *   <li>{@code redis://[user:password@]host[:port][/database][?protocol=3]}</li>
         *   <li>{@code rediss://[user:password@]host[:port][/database][?protocol=3]} (SSL)</li>
         * </ul>
         *
         * @param uri the Redis URI to parse
         * @return this builder
         * @throws IllegalArgumentException if the URI is invalid
         */
        public Builder fromURI(URI uri) {
            HostAndPort hnp = JedisURIHelper.getHostAndPort(uri);
            this.host = hnp.getHost();
            this.port = hnp.getPort();

            this.clientConfig = DefaultJedisClientConfig.builder()
                .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
                .database(JedisURIHelper.getDBIndex(uri)).protocol(JedisURIHelper.getRedisProtocol(uri))
                .ssl(JedisURIHelper.isRedisSSLScheme(uri)).build();
            return this;
        }
    }
}
