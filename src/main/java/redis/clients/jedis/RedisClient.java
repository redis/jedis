package redis.clients.jedis;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.IOUtils;

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
 * try (RedisClient client = RedisClient.builder()
 *         .host("localhost")
 *         .port(6379)
 *         .password("secret")
 *         .database(1)
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

    /**
     * Creates a Redis client with default configuration connecting to localhost:6379.
     */
    public RedisClient() {
        this("localhost", Protocol.DEFAULT_PORT);
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
        this(hostAndPort, DefaultJedisClientConfig.builder().build());
    }

    /**
     * Creates a Redis client with the specified client configuration.
     *
     * @param clientConfig the client configuration
     */
    public RedisClient(JedisClientConfig clientConfig) {
        this(new HostAndPort(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT), clientConfig);
    }

    /**
     * Creates a Redis client connecting to the specified host and port with the given configuration.
     *
     * @param hostAndPort the Redis server host and port
     * @param clientConfig the client configuration
     */
    public RedisClient(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
        this(hostAndPort, clientConfig, new ConnectionPoolConfig());
    }

    /**
     * Creates a Redis client with full configuration.
     *
     * @param hostAndPort the Redis server host and port
     * @param clientConfig the client configuration
     * @param poolConfig the connection pool configuration
     */
    public RedisClient(HostAndPort hostAndPort, JedisClientConfig clientConfig,
                      GenericObjectPoolConfig<Connection> poolConfig) {
        this.provider = new PooledConnectionProvider(hostAndPort, clientConfig, poolConfig);
        this.executor = new DefaultCommandExecutor(provider);
        this.commandObjects = new CommandObjects();

        // Set protocol if specified
        if (clientConfig.getRedisProtocol() != null) {
            this.commandObjects.setProtocol(clientConfig.getRedisProtocol());
        }
    }

    /**
     * Creates a Redis client from a URI.
     *
     * @param uri the Redis URI (e.g., "redis://localhost:6379")
     */
    public RedisClient(URI uri) {
        this(uri, DefaultJedisClientConfig.builder().build());
    }

    /**
     * Creates a Redis client from a URI with the specified configuration.
     *
     * @param uri the Redis URI
     * @param clientConfig the client configuration
     */
    public RedisClient(URI uri, JedisClientConfig clientConfig) {
        this(new HostAndPort(uri.getHost(), uri.getPort() == -1 ? Protocol.DEFAULT_PORT : uri.getPort()),
             mergeUriConfig(uri, clientConfig));
    }

    /**
     * Package-private constructor for builder.
     */
    RedisClient(Builder builder) {
        this.provider = new PooledConnectionProvider(
            new HostAndPort(builder.host, builder.port),
            builder.clientConfig,
            builder.poolConfig
        );
        this.executor = new DefaultCommandExecutor(provider);
        this.commandObjects = new CommandObjects();

        if (builder.clientConfig.getRedisProtocol() != null) {
            this.commandObjects.setProtocol(builder.clientConfig.getRedisProtocol());
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
     * Merges URI configuration with client configuration.
     */
    private static JedisClientConfig mergeUriConfig(URI uri, JedisClientConfig clientConfig) {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder().from(clientConfig);
        
        // Extract database from URI path
        if (uri.getPath() != null && uri.getPath().length() > 1) {
            try {
                int database = Integer.parseInt(uri.getPath().substring(1));
                builder.database(database);
            } catch (NumberFormatException e) {
                // Ignore invalid database in URI
            }
        }
        
        // Extract credentials from URI
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":", 2);
            if (userInfo.length == 1) {
                builder.password(userInfo[0]);
            } else if (userInfo.length == 2) {
                builder.user(userInfo[0]).password(userInfo[1]);
            }
        }
        
        // Set SSL if scheme is rediss
        if ("rediss".equals(uri.getScheme())) {
            builder.ssl(true);
        }
        
        return builder.build();
    }

    /**
     * Builder for configuring RedisClient instances.
     *
     * <p>Provides a fluent API for setting connection parameters, authentication,
     * SSL configuration, and connection pooling options.
     *
     * <p>Example usage:
     * <pre>{@code
     * RedisClient client = RedisClient.builder()
     *     .host("redis.example.com")
     *     .port(6379)
     *     .password("secret")
     *     .database(1)
     *     .ssl(true)
     *     .connectionTimeout(Duration.ofSeconds(5))
     *     .socketTimeout(Duration.ofSeconds(10))
     *     .maxPoolSize(20)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String host = "localhost";
        private int port = Protocol.DEFAULT_PORT;
        private JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();
        private GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

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
         * @param clientConfig the client configuration
         * @return this builder
         */
        public Builder clientConfig(JedisClientConfig clientConfig) {
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
         * Sets the Redis password for authentication.
         *
         * @param password the password
         * @return this builder
         */
        public Builder password(String password) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .password(password)
                .build();
            return this;
        }

        /**
         * Sets the Redis username and password for ACL authentication.
         *
         * @param user the username
         * @param password the password
         * @return this builder
         */
        public Builder auth(String user, String password) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .user(user)
                .password(password)
                .build();
            return this;
        }

        /**
         * Sets the Redis database number.
         *
         * @param database the database number (0-15 typically)
         * @return this builder
         */
        public Builder database(int database) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .database(database)
                .build();
            return this;
        }

        /**
         * Sets the client name for this connection.
         *
         * @param clientName the client name
         * @return this builder
         */
        public Builder clientName(String clientName) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .clientName(clientName)
                .build();
            return this;
        }

        /**
         * Enables or disables SSL/TLS encryption.
         *
         * @param ssl true to enable SSL, false to disable
         * @return this builder
         */
        public Builder ssl(boolean ssl) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .ssl(ssl)
                .build();
            return this;
        }

        /**
         * Sets the SSL socket factory for custom SSL configuration.
         *
         * @param sslSocketFactory the SSL socket factory
         * @return this builder
         */
        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .sslSocketFactory(sslSocketFactory)
                .build();
            return this;
        }

        /**
         * Sets the SSL parameters for SSL connections.
         *
         * @param sslParameters the SSL parameters
         * @return this builder
         */
        public Builder sslParameters(SSLParameters sslParameters) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .sslParameters(sslParameters)
                .build();
            return this;
        }

        /**
         * Sets the hostname verifier for SSL connections.
         *
         * @param hostnameVerifier the hostname verifier
         * @return this builder
         */
        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .hostnameVerifier(hostnameVerifier)
                .build();
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout the connection timeout
         * @return this builder
         */
        public Builder connectionTimeout(Duration timeout) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .connectionTimeoutMillis((int) timeout.toMillis())
                .build();
            return this;
        }

        /**
         * Sets the socket timeout for Redis operations.
         *
         * @param timeout the socket timeout
         * @return this builder
         */
        public Builder socketTimeout(Duration timeout) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .socketTimeoutMillis((int) timeout.toMillis())
                .build();
            return this;
        }

        /**
         * Sets the Redis protocol version.
         *
         * @param protocol the Redis protocol (RESP2 or RESP3)
         * @return this builder
         */
        public Builder protocol(RedisProtocol protocol) {
            this.clientConfig = DefaultJedisClientConfig.builder()
                .from(this.clientConfig)
                .protocol(protocol)
                .build();
            return this;
        }

        /**
         * Shortcut to set RESP3 protocol.
         *
         * @return this builder
         */
        public Builder resp3() {
            return protocol(RedisProtocol.RESP3);
        }

        /**
         * Sets the maximum number of connections in the pool.
         *
         * @param maxTotal the maximum total connections
         * @return this builder
         */
        public Builder maxPoolSize(int maxTotal) {
            this.poolConfig.setMaxTotal(maxTotal);
            return this;
        }

        /**
         * Sets the maximum number of idle connections in the pool.
         *
         * @param maxIdle the maximum idle connections
         * @return this builder
         */
        public Builder maxIdleConnections(int maxIdle) {
            this.poolConfig.setMaxIdle(maxIdle);
            return this;
        }

        /**
         * Sets the minimum number of idle connections in the pool.
         *
         * @param minIdle the minimum idle connections
         * @return this builder
         */
        public Builder minIdleConnections(int minIdle) {
            this.poolConfig.setMinIdle(minIdle);
            return this;
        }

        /**
         * Sets the maximum time to wait for a connection from the pool.
         *
         * @param maxWait the maximum wait time
         * @return this builder
         */
        public Builder maxWaitTime(Duration maxWait) {
            this.poolConfig.setMaxWait(maxWait);
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
    }
}
