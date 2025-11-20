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

  public RedisClient() {
    this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  /**
   * WARNING: This constructor only accepts a uri string as {@code url}.
   * {@link JedisURIHelper#isValid(java.net.URI)} can be used before this.
   * <p>
   * To use a host string, {@link #RedisClient(java.lang.String, int)} can be used with
   * {@link Protocol#DEFAULT_PORT}.
   * @param url redis url
   */
  public RedisClient(final String url) {
    super(url);
  }

  public RedisClient(final String host, final int port) {
    this(new HostAndPort(host, port));
  }

  public RedisClient(final HostAndPort hostAndPort) {
    super(hostAndPort);
  }

  public RedisClient(final String host, final int port, final String user, final String password) {
    super(new HostAndPort(host, port),
        DefaultJedisClientConfig.builder().user(user).password(password).build());
  }

  public RedisClient(final URI uri) {
    super(uri);
  }

  private RedisClient(CommandExecutor commandExecutor, ConnectionProvider connectionProvider,
      CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
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
