package redis.clients.jedis;

import java.net.URI;

import redis.clients.jedis.builders.StandaloneClientBuilder;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.Pool;

/**
 * {@code RedisClient} is the recommended client for connecting to standalone Redis deployments.
 * <p>
 * This class provides a modern, unified interface for interacting with Redis, supporting connection
 * pooling, authentication, and configuration via a fluent builder API.
 * </p>
 * <p>
 * {@code RedisClient} supersedes the deprecated {@link JedisPooled} and {@link UnifiedJedis}
 * classes, offering improved usability and extensibility. For new applications, use
 * {@code RedisClient} instead of the older classes.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *   &#64;code
 *   RedisClient client = RedisClient.builder().host("localhost").port(6379).build();
 * }
 * </pre>
 * <p>
 * For advanced configuration, see the {@link RedisClient.Builder} class.
 * </p>
 */
public class RedisClient extends UnifiedJedis {

  private RedisClient(CommandExecutor commandExecutor, ConnectionProvider connectionProvider,
      CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Creates a RedisClient with default host and port (localhost:6379).
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @return a new {@link RedisClient} instance
   */
  public static RedisClient create() {
    return builder().build();
  }

  /**
   * Creates a RedisClient from a Redis URI.
   * <p>
   * The URI must be in the format: {@code redis[s]://[[user][:password]@]host[:port][/database]}
   * <p>
   * Examples:
   * <ul>
   * <li>{@code redis://localhost:6379}</li>
   * <li>{@code redis://user:password@localhost:6379/0}</li>
   * <li>{@code rediss://localhost:6380} (SSL)</li>
   * </ul>
   * <p>
   * <b>Note:</b> To connect using just a hostname and port without a URI, use
   * {@link #create(String, int)} instead.
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @param url Redis URI string (not just a hostname)
   * @return a new {@link RedisClient} instance
   * @throws IllegalArgumentException if the URI format is invalid
   * @see JedisURIHelper#isValid(java.net.URI)
   */
  public static RedisClient create(final String url) {
    return builder().fromURI(url).build();
  }

  /**
   * Creates a RedisClient with the specified host and port.
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @param host the Redis server hostname
   * @param port the Redis server port
   * @return a new {@link RedisClient} instance
   */
  public static RedisClient create(final String host, final int port) {
    return builder().hostAndPort(host, port).build();
  }

  /**
   * Creates a RedisClient with the specified host and port.
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @param hostAndPort the Redis server host and port
   * @return a new {@link RedisClient} instance
   */
  public static RedisClient create(final HostAndPort hostAndPort) {
    return builder().hostAndPort(hostAndPort).build();
  }

  /**
   * Creates a RedisClient with the specified host, port, user, and password.
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @param host the Redis server hostname
   * @param port the Redis server port
   * @param user the username for authentication
   * @param password the password for authentication
   * @return a new {@link RedisClient} instance
   */
  public static RedisClient create(final String host, final int port, final String user,
      final String password) {
    return builder().hostAndPort(host, port)
        .clientConfig(DefaultJedisClientConfig.builder().user(user).password(password).build())
        .build();
  }

  /**
   * Creates a RedisClient from a Redis URI.
   * <p>
   * This is a convenience factory method that uses the builder pattern internally.
   * @param uri the Redis server URI
   * @return a new {@link RedisClient} instance
   */
  public static RedisClient create(final URI uri) {
    return builder().fromURI(uri).build();
  }

  /**
   * Fluent builder for {@link RedisClient} (standalone).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  public static class Builder extends StandaloneClientBuilder<RedisClient> {

    @Override
    protected RedisClient createClient() {
      return new RedisClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache);
    }
  }

  /**
   * Create a new builder for configuring RedisClient instances.
   * @return a new {@link RedisClient.Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public final Pool<Connection> getPool() {
    return ((PooledConnectionProvider) provider).getPool();
  }

  @Override
  public Pipeline pipelined() {
    return (Pipeline) super.pipelined();
  }
}
