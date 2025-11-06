package redis.clients.jedis;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.builders.ClusterClientBuilder;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;

/**
 * RedisClusterClient provides a high-level, unified interface for interacting with a Redis Cluster.
 * <p>
 * This class is intended as a modern replacement for the deprecated {@code JedisCluster} class. It
 * supports all cluster operations and is designed to work seamlessly with the {@link UnifiedJedis}
 * API, allowing for consistent usage patterns across standalone, sentinel, and cluster deployments.
 * <p>
 * <b>Usage:</b>
 * 
 * <pre>
 * {
 *   &#64;code
 *   Set<HostAndPort> clusterNodes = new HashSet<>();
 *   clusterNodes.add(new HostAndPort("127.0.0.1", 7000));
 *   RedisClusterClient client = new RedisClusterClient(clusterNodes);
 *   client.set("key", "value");
 *   String value = client.get("key");
 * }
 * </pre>
 * <p>
 * <b>Migration:</b> Users of {@code JedisCluster} are encouraged to migrate to this class for
 * improved API consistency, better resource management, and enhanced support for future Redis
 * features.
 * <p>
 * <b>Thread-safety:</b> This client is thread-safe and can be shared across multiple threads.
 * <p>
 * <b>Configuration:</b> Various constructors allow for flexible configuration, including
 * authentication and custom timeouts.
 */
public class RedisClusterClient extends UnifiedJedis {

  public static final String INIT_NO_ERROR_PROPERTY = "jedis.cluster.initNoError";

  /**
   * Default timeout in milliseconds.
   */
  public static final int DEFAULT_TIMEOUT = 2000;

  /**
   * Default amount of attempts for executing a command
   */
  public static final int DEFAULT_MAX_ATTEMPTS = 5;

  /**
   * Creates a RedisClusterClient instance. The provided node is used to make the first contact with
   * the cluster.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.RedisClusterClient#DEFAULT_TIMEOUT} ms
   * is being used with {@value redis.clients.jedis.RedisClusterClient#DEFAULT_MAX_ATTEMPTS} maximum
   * attempts.
   * @param node Node to first connect to.
   */
  public RedisClusterClient(HostAndPort node) {
    super(
        new ClusterConnectionProvider(Collections.singleton(node),
            DefaultJedisClientConfig.builder().timeoutMillis(DEFAULT_TIMEOUT).build()),
        DEFAULT_MAX_ATTEMPTS, Duration.ofMillis((long) DEFAULT_TIMEOUT * DEFAULT_MAX_ATTEMPTS));
  }

  /**
   * Creates a RedisClusterClient with multiple entry points.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.RedisClusterClient#DEFAULT_TIMEOUT} ms
   * is being used with {@value redis.clients.jedis.RedisClusterClient#DEFAULT_MAX_ATTEMPTS} maximum
   * attempts.
   * @param nodes Nodes to connect to.
   */
  public RedisClusterClient(Set<HostAndPort> nodes) {
    super(
        new ClusterConnectionProvider(nodes,
            DefaultJedisClientConfig.builder().timeoutMillis(DEFAULT_TIMEOUT).build()),
        DEFAULT_MAX_ATTEMPTS, Duration.ofMillis((long) DEFAULT_TIMEOUT * DEFAULT_MAX_ATTEMPTS));
  }

  public RedisClusterClient(Set<HostAndPort> nodes, String user, String password) {
    super(
        new ClusterConnectionProvider(nodes,
            DefaultJedisClientConfig.builder().user(user).password(password).build()),
        DEFAULT_MAX_ATTEMPTS,
        Duration.ofMillis((long) Protocol.DEFAULT_TIMEOUT * DEFAULT_MAX_ATTEMPTS));
  }

  private RedisClusterClient(CommandExecutor commandExecutor, ConnectionProvider connectionProvider,
      CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Fluent builder for {@link RedisClusterClient} (Redis Cluster).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  static public class Builder extends ClusterClientBuilder<RedisClusterClient> {

    @Override
    protected RedisClusterClient createClient() {
      return new RedisClusterClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache);
    }
  }

  /**
   * Create a new builder for configuring RedisClusterClient instances.
   * @return a new {@link RedisClusterClient.Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns all nodes that were configured to connect to in key-value pairs ({@link Map}).<br>
   * Key is the HOST:PORT and the value is the connection pool.
   * @return the map of all connections.
   */
  public Map<String, ConnectionPool> getClusterNodes() {
    return ((ClusterConnectionProvider) provider).getNodes();
  }

  /**
   * Returns the connection for one of the 16,384 slots.
   * @param slot the slot to retrieve the connection for.
   * @return connection of the provided slot. {@code close()} of this connection must be called
   *         after use.
   */
  public Connection getConnectionFromSlot(int slot) {
    return ((ClusterConnectionProvider) provider).getConnectionFromSlot(slot);
  }

  // commands
  public long spublish(String channel, String message) {
    return executeCommand(commandObjects.spublish(channel, message));
  }

  public long spublish(byte[] channel, byte[] message) {
    return executeCommand(commandObjects.spublish(channel, message));
  }

  public void ssubscribe(final JedisShardedPubSub jedisPubSub, final String... channels) {
    try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  public void ssubscribe(BinaryJedisShardedPubSub jedisPubSub, final byte[]... channels) {
    try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
      jedisPubSub.proceed(connection, channels);
    }
  }
  // commands

  @Override
  public ClusterPipeline pipelined() {
    return new ClusterPipeline((ClusterConnectionProvider) provider,
        (ClusterCommandObjects) commandObjects);
  }

  /**
   * @param doMulti param
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public AbstractTransaction transaction(boolean doMulti) {
    throw new UnsupportedOperationException();
  }

  public final <T> T executeCommandToReplica(CommandObject<T> commandObject) {
    if (!(executor instanceof ClusterCommandExecutor)) {
      throw new UnsupportedOperationException(
          "Support only execute to replica in ClusterCommandExecutor");
    }
    return ((ClusterCommandExecutor) executor).executeCommandToReplica(commandObject);
  }
}
