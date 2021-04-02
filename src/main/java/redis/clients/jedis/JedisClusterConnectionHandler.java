package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler implements Closeable {
  protected final JedisClusterInfoCache cache;

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      String password) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, password, null);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, null, password, clientName);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      String user, String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, 0, user, password, clientName);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, String user, String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, infiniteSoTimeout, user, password,
        clientName, false, null, null, null, null);
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      String password, String clientName, boolean ssl, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, HostnameVerifier hostnameVerifier,
      JedisClusterHostAndPortMap portMap) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, null, password, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier, portMap);
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout, String user,
      String password, String clientName, boolean ssl, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, HostnameVerifier hostnameVerifier,
      JedisClusterHostAndPortMap portMap) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, 0, user, password, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier, portMap);
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig<Jedis> poolConfig, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, String user, String password, String clientName, boolean ssl,
      SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
    this(nodes, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout).user(user)
        .password(password).clientName(clientName).ssl(ssl).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build(), poolConfig,
        DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
            .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
            .user(user).password(password).clientName(clientName).ssl(ssl)
            .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build());
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final JedisClientConfig seedNodesClientConfig,
      final GenericObjectPoolConfig<Jedis> poolConfig,
      final JedisClientConfig clusterNodesClientConfig) {
    this.cache = new JedisClusterInfoCache(poolConfig, clusterNodesClientConfig);
    initializeSlotsCache(nodes, seedNodesClientConfig);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig<Jedis> poolConfig, final JedisClientConfig clientConfig) {
    this.cache = new JedisClusterInfoCache(poolConfig, clientConfig);
    initializeSlotsCache(nodes, clientConfig);
  }

  protected abstract Jedis getConnection();

  protected abstract Jedis getConnectionFromSlot(int slot);

  public Jedis getConnectionFromNode(HostAndPort node) {
    return cache.setupNodeIfNotExist(node).getResource();
  }

  public Map<String, JedisPool> getNodes() {
    return cache.getNodes();
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, JedisClientConfig clientConfig) {
    ArrayList<HostAndPort> startNodeList = new ArrayList<>(startNodes);
    Collections.shuffle(startNodeList);

    for (HostAndPort hostAndPort : startNodeList) {
      try (Jedis jedis = new Jedis(hostAndPort, clientConfig)) {
        cache.discoverClusterNodesAndSlots(jedis);
        return;
      } catch (JedisConnectionException e) {
        // try next nodes
      }
    }
  }

  public void renewSlotCache() {
    cache.renewClusterSlots(null);
  }

  public void renewSlotCache(Jedis jedis) {
    cache.renewClusterSlots(jedis);
  }

  @Override
  public void close() {
    cache.reset();
  }
}
