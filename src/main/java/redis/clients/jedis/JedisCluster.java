package redis.clients.jedis;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.ClusterConnectionProvider;

public class JedisCluster extends UnifiedJedis {

  /**
   * Default timeout in milliseconds.
   */
  public static final int DEFAULT_TIMEOUT = 2000;
  /**
   * Default amount of attemps for connecting.
   */
  public static final int DEFAULT_MAX_ATTEMPTS = 5;

  /**
   * Creates a Jedis-Cluster Instance where only a single Host is being used as a "Cluster".<br>
   * Here, the default Timeout of {@value #DEFAULT_TIMEOUT} ms is being used with {@value  #DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param node Host to connect to.
   */
  public JedisCluster(HostAndPort node) {
    this(Collections.singleton(node));
  }

  /**
   * Creates a Jedis-Cluster Instance where only a single Host is being used as a "Cluster".<br>
   * Here, the default of {@value  #DEFAULT_MAX_ATTEMPTS} maximum attempts is being used.
   * @param node Host to connect to.
   * @param timeout Timeout in milliseconds.
   */
  public JedisCluster(HostAndPort node, int timeout) {
    this(Collections.singleton(node), timeout);
  }

  /**
   * Creates a Jedis-Cluster Instance where only a single Host is being used as a "Cluster".<br>
   * You can specify the timeout and the maximum attempts.
   * @param node Host to connect to.
   * @param timeout Timeout in milliseconds.
   * @param maxAttempts Maximum Attempts to use.
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
   * Creates a Jedis-Cluster with multiple Nodes/Instances.
   * Here, the default Timeout of {@value #DEFAULT_TIMEOUT} ms is being used with {@value  #DEFAULT_MAX_ATTEMPTS} maximum attempts.
   * @param nodes Hosts to connect to.
   */
  public JedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  /**
   * Creates a Jedis-Cluster with multiple Nodes/Instances.
   * Here, the default of {@value  #DEFAULT_MAX_ATTEMPTS} maximum attempts is being used.
   * @param nodes Hosts to connect to.
   * @param timeout Timeout in milliseconds.
   */
  public JedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, DefaultJedisClientConfig.builder().timeoutMillis(timeout).build());
  }

  /**
   * Creates a Jedis-Cluster with multiple Nodes/Instances. <br>
   * You can specify the timeout and the maximum attempts.
   * @param nodes Hosts to connect to.
   * @param timeout Timeout in milliseconds.
   * @param maxAttempts Maximum Attempts to use.
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

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      int maxAttempts, GenericObjectPoolConfig<Connection> poolConfig) {
    this(clusterNodes, clientConfig, maxAttempts,
        Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts), poolConfig);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      int maxAttempts, Duration maxTotalRetriesDuration,
      GenericObjectPoolConfig<Connection> poolConfig) {
    super(clusterNodes, clientConfig, poolConfig, maxAttempts, maxTotalRetriesDuration);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig) {
    this(clusterNodes, clientConfig, DEFAULT_MAX_ATTEMPTS);
  }

  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts) {
    super(clusterNodes, clientConfig, maxAttempts);
  }

  /**
   * Creates a Jedis-Cluster with multiple Nodes/Instances. <br>
   * You can specify the timeout and the maximum attempts. <br><br>
   *
   * Additionally, you are free to provide a {@link JedisClientConfig} instance. <br>
   * You can use the {@link DefaultJedisClientConfig#builder()} Builder-Pattern to customize your configuration, includings socketTimeouts, Username & Passwords aswell as SSL and hostnames.
   *
   * @param clusterNodes Hosts to connect to.
   * @param clientConfig Timeout in milliseconds.
   * @param maxAttempts Maximum Attempts to use.
   * @param maxTotalRetriesDuration Maximum time used for reconnecting.
   */
  public JedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    super(clusterNodes, clientConfig, maxAttempts, maxTotalRetriesDuration);
  }

  public JedisCluster(ClusterConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    super(provider, maxAttempts, maxTotalRetriesDuration);
  }

  /**
   * Returns all nodes that were configured to connect to in a KEY-VALUE pair (Map).
   * Key is the HOST:PORT and the value is the connection.
   * @return Map of all connections.
   */
  public Map<String, ConnectionPool> getClusterNodes() {
    return ((ClusterConnectionProvider) provider).getNodes();
  }

  /**
   * Returns the connection for one of the 16,384 slots.<br><br>
   * If there is no connection for the given slot, either cause the node died or didn't connect, it may not return the correct node.
   * It will try to resolve connection-based issues using reconnection.
   * @param slot Slot to retrieve the Connection for
   * @return connection of the provided slot. {@code close()} of this connection must be called after use.
   */
  public Connection getConnectionFromSlot(int slot) {
    return ((ClusterConnectionProvider) provider).getConnectionFromSlot(slot);
  }

  @Override
  public ClusterPipeline pipelined() {
    return new ClusterPipeline((ClusterConnectionProvider) provider);
  }
}
