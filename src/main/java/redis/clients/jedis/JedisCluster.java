package redis.clients.jedis;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.util.JedisClusterCRC16;

public class JedisCluster extends UnifiedJedis {

  public static final String INIT_NO_ERROR_PROPERTY = "jedis.cluster.initNoError";

  /**
   * Default timeout in milliseconds.
   */
  public static final int DEFAULT_TIMEOUT = 2000;
  public static final int DEFAULT_MAX_ATTEMPTS = 5;

  public JedisCluster(HostAndPort node) {
    this(Collections.singleton(node));
  }

  public JedisCluster(HostAndPort node, int timeout) {
    this(Collections.singleton(node), timeout);
  }

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

  public JedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, DefaultJedisClientConfig.builder().timeoutMillis(timeout).build());
  }

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

  private JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol) {
    super(provider, maxAttempts, maxTotalRetriesDuration, protocol);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, ClientSideCache clientSideCache) {
    this(clusterNodes, clientConfig, clientSideCache, DEFAULT_MAX_ATTEMPTS,
        Duration.ofMillis(DEFAULT_MAX_ATTEMPTS * clientConfig.getSocketTimeoutMillis()));
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache), maxAttempts, maxTotalRetriesDuration,
        clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      int maxAttempts, Duration maxTotalRetriesDuration, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig),
        maxAttempts, maxTotalRetriesDuration, clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig),
        DEFAULT_MAX_ATTEMPTS, Duration.ofMillis(DEFAULT_MAX_ATTEMPTS * clientConfig.getSocketTimeoutMillis()),
        clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, clientSideCache, poolConfig, topologyRefreshPeriod),
        maxAttempts, maxTotalRetriesDuration, clientConfig.getRedisProtocol(), clientSideCache);
  }

  @Experimental
  private JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol, ClientSideCache clientSideCache) {
    super(provider, maxAttempts, maxTotalRetriesDuration, protocol, clientSideCache);
  }

  // Uses a fetched connection to process protocol. Should be avoided if possible.
  public JedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    super(provider, maxAttempts, maxTotalRetriesDuration);
  }

  public Map<String, ConnectionPool> getClusterNodes() {
    return ((ClusterConnectionProvider) provider).getNodes();
  }

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
   * @return nothing
   * @throws UnsupportedOperationException
   */
  @Override
  public Transaction multi() {
    throw new UnsupportedOperationException();
  }
}
