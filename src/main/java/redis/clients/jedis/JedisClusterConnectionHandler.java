package redis.clients.jedis;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler implements Closeable {
  protected final JedisClusterInfoCache cache;

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                       final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password) {
    this(nodes, poolConfig, connectionTimeout, soTimeout, password, null);
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
          final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, String clientName) {
    this.cache = new JedisClusterInfoCache(poolConfig, connectionTimeout, soTimeout, password, clientName);
    initializeSlotsCache(nodes, poolConfig, password, clientName);
}

  abstract Jedis getConnection();

  abstract Jedis getConnectionFromSlot(int slot);

  public Jedis getConnectionFromNode(HostAndPort node) {
    return cache.setupNodeIfNotExist(node).getResource();
  }
  
  public Map<String, JedisPool> getNodes() {
    return cache.getNodes();
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, GenericObjectPoolConfig poolConfig, String password, String clientName) {
    for (HostAndPort hostAndPort : startNodes) {
      Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort());
      try {
        if (password != null) {
          jedis.auth(password);
        }
        if (clientName != null) {
          jedis.clientSetname(clientName);
        }
        cache.discoverClusterNodesAndSlots(jedis);
        break;
      } catch (JedisConnectionException e) {
        // try next nodes
      } finally {
        if (jedis != null) {
          jedis.close();
        }
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
