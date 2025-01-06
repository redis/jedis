package redis.clients.jedis;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.util.JedisClusterCRC16;

public class JedisCluster extends UnifiedJedis {

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
   * Creates a JedisCluster instance. The provided node is used to make the first contact with the cluster.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.JedisCluster#DEFAULT_TIMEOUT} ms is being used with
   * {@value redis.clients.jedis.JedisCluster#DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param node Node to first connect to.
   */
  public JedisCluster(HostAndPort node) {
    this(Collections.singleton(node));
  }

  /**
   * Creates a JedisCluster instance. The provided node is used to make the first contact with the cluster.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.JedisCluster#DEFAULT_TIMEOUT} ms is being used with
   * {@value redis.clients.jedis.JedisCluster#DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param node Node to first connect to.
   * @param timeout connection and socket timeout in milliseconds.
   */
  public JedisCluster(HostAndPort node, int timeout) {
    this(Collections.singleton(node), timeout);
  }

  /**
   * Creates a JedisCluster instance. The provided node is used to make the first contact with the cluster.<br>
   * You can specify the timeout and the maximum attempts.
   * @param node Node to first connect to.
   * @param timeout connection and socket timeout in milliseconds.
   * @param maxAttempts maximum attempts for executing a command.
   */
  public JedisCluster(HostAndPort node, int timeout, int maxAttempts) {
    this(Collections.singleton(node), timeout, maxAttempts);
  }

  public JedisCluster(HostAndPort node, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), poolConfig);
  }

  public JedisCluster(HostAndPort node, int timeout, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), timeout, poolConfig);
  }

  public JedisCluster(HostAndPort node, int timeout, int maxAttempts,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), timeout, maxAttempts, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String password, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password,
        poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String password, String clientName, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password,
        clientName, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String user, String password, String clientName,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, user, password,
        clientName, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String password, String clientName, final GenericObjectPoolConfig<Connection> poolConfig,
      boolean ssl) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password,
        clientName, poolConfig, ssl);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String user, String password, String clientName,
      final GenericObjectPoolConfig<Connection> poolConfig, boolean ssl) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, user, password,
        clientName, poolConfig, ssl);
  }

  public JedisCluster(HostAndPort node, final JedisClientConfig clientConfig, int maxAttempts,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(Collections.singleton(node), clientConfig, maxAttempts, poolConfig);
  }

  /**
   * Creates a JedisCluster with multiple entry points.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.JedisCluster#DEFAULT_TIMEOUT} ms is being used with
   * {@value redis.clients.jedis.JedisCluster#DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param nodes Nodes to connect to.
   */
  public JedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  /**
   * Creates a JedisCluster with multiple entry points.
   * <p>
   * Here, the default timeout of {@value redis.clients.jedis.JedisCluster#DEFAULT_TIMEOUT} ms is being used with
   * {@value redis.clients.jedis.JedisCluster#DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param nodes Nodes to connect to.
   * @param timeout connection and socket timeout in milliseconds.
   */
  public JedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, DefaultJedisClientConfig.builder().timeoutMillis(timeout).build());
  }

  /**
   * Creates a JedisCluster with multiple entry points.<br>
   * You can specify the timeout and the maximum attempts.
   * @param nodes Nodes to connect to.
   * @param timeout connection and socket timeout in milliseconds.
   * @param maxAttempts maximum attempts for executing a command.
   */
  public JedisCluster(Set<HostAndPort> nodes, int timeout, int maxAttempts) {
    this(nodes, DefaultJedisClientConfig.builder().timeoutMillis(timeout).build(), maxAttempts);
  }

  public JedisCluster(Set<HostAndPort> nodes, String user, String password) {
    this(nodes, DefaultJedisClientConfig.builder().user(user).password(password).build());
  }

  public JedisCluster(Set<HostAndPort> nodes, String user, String password,
      HostAndPortMapper hostAndPortMap) {
    this(nodes, DefaultJedisClientConfig.builder().user(user).password(password)
        .hostAndPortMapper(hostAndPortMap).build());
  }

  public JedisCluster(Set<HostAndPort> nodes, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(nodes, DEFAULT_TIMEOUT, DEFAULT_MAX_ATTEMPTS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(nodes, timeout, DEFAULT_MAX_ATTEMPTS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int timeout, int maxAttempts,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, timeout, timeout, maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, final GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, connectionTimeout, soTimeout, maxAttempts, null, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, connectionTimeout, soTimeout, maxAttempts, password, null, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, connectionTimeout, soTimeout, maxAttempts, null, password, clientName,
        poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).user(user).password(password).clientName(clientName).build(),
        maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, int maxAttempts, String user, String password, String clientName,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
        .user(user).password(password).clientName(clientName).build(), maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName,
      GenericObjectPoolConfig<Connection> poolConfig, boolean ssl) {
    this(clusterNodes, connectionTimeout, soTimeout, maxAttempts, null, password, clientName,
        poolConfig, ssl);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName,
      GenericObjectPoolConfig<Connection> poolConfig, boolean ssl) {
    this(clusterNodes, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).user(user).password(password).clientName(clientName).ssl(ssl).build(),
        maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig) {
    this(clusterNodes, clientConfig, DEFAULT_MAX_ATTEMPTS);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts) {
    this(clusterNodes, clientConfig, maxAttempts,
        Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts));
  }

  /**
   * Creates a JedisCluster with multiple entry points.<br>
   * You can specify the timeout and the maximum attempts.<br>
   *
   * Additionally, you are free to provide a {@link JedisClientConfig} instance.<br>
   * You can use the {@link DefaultJedisClientConfig#builder()} builder pattern to customize your configuration, including socket timeouts,
   * username and passwords as well as SSL related parameters.
   *
   * @param clusterNodes Nodes to connect to.
   * @param clientConfig Client configuration parameters.
   * @param maxAttempts maximum attempts for executing a command.
   * @param maxTotalRetriesDuration Maximum time used for reconnecting.
   */
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig), maxAttempts, maxTotalRetriesDuration,
        clientConfig.getRedisProtocol());
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, clientConfig, DEFAULT_MAX_ATTEMPTS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, clientConfig, maxAttempts,
        Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts), poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts,
      Duration maxTotalRetriesDuration, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig), maxAttempts, maxTotalRetriesDuration,
        clientConfig.getRedisProtocol());
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig, topologyRefreshPeriod),
        maxAttempts, maxTotalRetriesDuration, clientConfig.getRedisProtocol());
  }

  // Uses a fetched connection to process protocol. Should be avoided if possible.
  public JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    super(provider, maxAttempts, maxTotalRetriesDuration);
  }

  private JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol) {
    super(provider, maxAttempts, maxTotalRetriesDuration, protocol);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> hnp, JedisClientConfig jedisClientConfig, CacheConfig cacheConfig) {
    this(hnp, jedisClientConfig, CacheFactory.getCache(cacheConfig));
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, Cache clientSideCache) {
    this(clusterNodes, clientConfig, clientSideCache, DEFAULT_MAX_ATTEMPTS,
        Duration.ofMillis(DEFAULT_MAX_ATTEMPTS * clientConfig.getSocketTimeoutMillis()));
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, Cache clientSideCache,
      int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache), maxAttempts,
        maxTotalRetriesDuration, clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, Cache clientSideCache,
      int maxAttempts, Duration maxTotalRetriesDuration, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig),
        maxAttempts, maxTotalRetriesDuration, clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, Cache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig),
        DEFAULT_MAX_ATTEMPTS, Duration.ofMillis(DEFAULT_MAX_ATTEMPTS * clientConfig.getSocketTimeoutMillis()),
        clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, Cache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig, topologyRefreshPeriod),
        maxAttempts, maxTotalRetriesDuration, clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  private JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol, Cache clientSideCache) {
    super(provider, maxAttempts, maxTotalRetriesDuration, protocol, clientSideCache);
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
   * @return connection of the provided slot. {@code close()} of this connection must be called after use.
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
    return new ClusterPipeline((ClusterConnectionProvider) provider, (ClusterCommandObjects) commandObjects);
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
      throw new UnsupportedOperationException("Support only execute to replica in ClusterCommandExecutor");
    }
    return ((ClusterCommandExecutor) executor).executeCommandToReplica(commandObject);
  }
}
