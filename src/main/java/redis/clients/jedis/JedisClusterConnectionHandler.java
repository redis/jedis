package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler {
  protected final JedisClusterInfoCache cache;

  public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                       final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout) {
    this.cache = new JedisClusterInfoCache(poolConfig, connectionTimeout, soTimeout);
    initializeSlotsCache(nodes, poolConfig);
  }

  abstract Jedis getConnection();

  abstract Jedis getConnectionFromSlot(int slot);

  public Jedis getConnectionFromNode(HostAndPort node) {
    cache.setNodeIfNotExist(node);
    return cache.getNode(JedisClusterInfoCache.getNodeKey(node)).getResource();
  }
  
  public Map<String, JedisPool> getNodes() {
    return cache.getNodes();
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, GenericObjectPoolConfig poolConfig) {
    for (HostAndPort hostAndPort : startNodes) {
      Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort());
      try {
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
    for (JedisPool jp : getShuffledNodesPool()) {
      Jedis jedis = null;
      try {
        jedis = jp.getResource();
        cache.discoverClusterSlots(jedis);
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

  public void renewSlotCache(Jedis jedis) {
    try {
      cache.discoverClusterSlots(jedis);
    } catch (JedisConnectionException e) {
      renewSlotCache();
    }
  }

  protected List<JedisPool> getShuffledNodesPool() {
    List<JedisPool> pools = new ArrayList<JedisPool>();
    pools.addAll(cache.getNodes().values());
    Collections.shuffle(pools);
    return pools;
  }
}
