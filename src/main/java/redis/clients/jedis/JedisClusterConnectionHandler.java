package redis.clients.jedis;

import java.util.List;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;

import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler {
  protected final JedisClusterInfoCache cache;

  abstract Jedis getConnection();

  public void returnConnection(Jedis connection) {
    cache.getNode(getNodeKey(connection.getClient())).returnResource(connection);
  }

  public void returnBrokenConnection(Jedis connection) {
    cache.getNode(getNodeKey(connection.getClient())).returnBrokenResource(connection);
  }

  abstract Jedis getConnectionFromSlot(int slot);

  public Jedis getConnectionFromNode(HostAndPort node) {
    cache.setNodeIfNotExist(node);
    return cache.getNode(JedisClusterInfoCache.getNodeKey(node)).getResource();
  }

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig poolConfig) {
    this.cache = new JedisClusterInfoCache(poolConfig);
    initializeSlotsCache(nodes, poolConfig);
  }

  public Map<String, JedisPool> getNodes() {
    return cache.getNodes();
  }

  public List<JedisPool> getMasterNodes() {
    return cache.getMasterNodes();
  }

  public void assignSlotToNode(int slot, HostAndPort targetNode) {
    cache.assignSlotToNode(slot, targetNode);
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, GenericObjectPoolConfig poolConfig) {
    for (HostAndPort hostAndPort : startNodes) {
      JedisPool jp = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());

      Jedis jedis = null;
      try {
        jedis = jp.getResource();
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

    for (HostAndPort node : startNodes) {
      cache.setNodeIfNotExist(node);
    }
  }

  public void renewSlotCache() {
    for (JedisPool jp : cache.getNodes().values()) {
      Jedis jedis = null;
      try {
        jedis = jp.getResource();
        cache.discoverClusterSlots(jedis);
        break;
      } finally {
        if (jedis != null) {
          jedis.close();
        }
      }
    }
  }

}
