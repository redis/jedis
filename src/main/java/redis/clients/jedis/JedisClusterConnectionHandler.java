package redis.clients.jedis;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler implements Closeable {
  protected final JedisClusterInfoCache cache;

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, String password) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, password, null);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, null, password, clientName);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, String user, String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, 0, user, password, clientName);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, int infiniteSoTimeout, String user, String password, String clientName) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, clientName, false, null, null, null, null);
  }

  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, String password, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, null, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
  }

  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, String user, String password, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, 0, user, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
  }

  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
      int connectionTimeout, int soTimeout, int infiniteSoTimeout, String user, String password, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
    this(nodes,
        DefaultJedisSocketConfig.builder().withConnectionTimeout(connectionTimeout).withSoTimeout(soTimeout)
            .withSsl(ssl).withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
            .withHostnameVerifier(hostnameVerifier).build(),
        poolConfig,
        DefaultJedisSocketConfig.builder().withConnectionTimeout(connectionTimeout).withSoTimeout(soTimeout)
            .withSsl(ssl).withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
            .withHostnameVerifier(hostnameVerifier).withHostAndPortMapper(portMap).build(),
        infiniteSoTimeout, user, password, clientName);
  }

  @Deprecated
  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final JedisSocketConfig seedNodesSocketConfig,
      final GenericObjectPoolConfig poolConfig, final JedisSocketConfig clusterNodesSocketConfig,
      int infiniteSoTimeout, String user, String password, String clientName) {
    final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().withInfiniteSoTimeout(infiniteSoTimeout)
        .withUser(user).withPassword(password).withClinetName(clientName).build();
    this.cache = new JedisClusterInfoCache(poolConfig, clusterNodesSocketConfig, clientConfig);
    initializeSlotsCache(nodes, seedNodesSocketConfig, clientConfig);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
      final JedisSocketConfig socketConfig, final JedisClientConfig clientConfig) {
    this.cache = new JedisClusterInfoCache(poolConfig, socketConfig, clientConfig);
    initializeSlotsCache(nodes, socketConfig, clientConfig);
  }

  protected abstract Jedis getConnection();

  protected abstract Jedis getConnectionFromSlot(int slot);

  public Jedis getConnectionFromNode(HostAndPort node) {
    return cache.setupNodeIfNotExist(node).getResource();
  }
  
  public Map<String, JedisPool> getNodes() {
    return cache.getNodes();
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, JedisSocketConfig socketConfig,
      JedisClientConfig clientConfig) {

    for (HostAndPort hostAndPort : startNodes) {
      try (Jedis jedis = new Jedis(hostAndPort, socketConfig, clientConfig.getInfiniteSoTimeout())) { 
        if (clientConfig.getUser() != null) {
          jedis.auth(clientConfig.getUser(), clientConfig.getPassword());
        } else if (clientConfig.getPassword() != null) {
          jedis.auth(clientConfig.getPassword());
        }
        if (clientConfig.getClientName() != null) {
          jedis.clientSetname(clientConfig.getClientName());
        }
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
