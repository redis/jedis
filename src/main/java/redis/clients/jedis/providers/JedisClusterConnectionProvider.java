package redis.clients.jedis.providers;

import redis.clients.jedis.ClusterCommandArguments;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class JedisClusterConnectionProvider implements JedisConnectionProvider {

  protected final JedisClusterInfoCache cache;

  public JedisClusterConnectionProvider(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig) {
    this.cache = new JedisClusterInfoCache(clientConfig);
    initializeSlotsCache(jedisClusterNodes, clientConfig);
  }

  public JedisClusterConnectionProvider(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this.cache = new JedisClusterInfoCache(clientConfig, poolConfig);
    initializeSlotsCache(jedisClusterNodes, clientConfig);
  }

  private void initializeSlotsCache(Set<HostAndPort> startNodes, JedisClientConfig clientConfig) {
    ArrayList<HostAndPort> startNodeList = new ArrayList<>(startNodes);
    Collections.shuffle(startNodeList);

    for (HostAndPort hostAndPort : startNodeList) {
      try (Connection jedis = new Connection(hostAndPort, clientConfig)) {
        cache.discoverClusterNodesAndSlots(jedis);
        return;
      } catch (JedisConnectionException e) {
        // try next nodes
      }
    }
  }

  @Override
  public void close() {
    cache.reset();
  }

  public void renewSlotCache() {
    cache.renewClusterSlots(null);
  }

  public void renewSlotCache(Connection jedis) {
    cache.renewClusterSlots(jedis);
  }

  public Map<String, Pool<Connection>> getNodes() {
    return cache.getNodes();
  }

  public HostAndPort getNode(String key) {
    return cache.getSlotNode(JedisClusterCRC16.getSlot(key));
  }

  public Connection getConnection(HostAndPort node) {
    return cache.setupNodeIfNotExist(node).getResource();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    final int slot = ((ClusterCommandArguments) args).getCommandHashSlot();
    return slot >= 0 ? getConnectionFromSlot(slot) : getConnection();
  }

  public Connection getConnection() {
    // In antirez's redis-rb-cluster implementation, getRandomConnection always
    // return valid connection (able to ping-pong) or exception if all
    // connections are invalid

    List<Pool<Connection>> pools = cache.getShuffledNodesPool();

    JedisException suppressed = null;
    for (Pool<Connection> pool : pools) {
      Connection jedis = null;
      try {
        jedis = pool.getResource();
        if (jedis == null) {
          continue;
        }

        jedis.ping();
        return jedis;

      } catch (JedisException ex) {
        if (suppressed == null) { // remembering first suppressed exception
          suppressed = ex;
        }
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    JedisClusterOperationException noReachableNode = new JedisClusterOperationException("No reachable node in cluster.");
    if (suppressed != null) {
      noReachableNode.addSuppressed(suppressed);
    }
    throw noReachableNode;
  }

  public Connection getConnectionFromSlot(int slot) {
    Pool<Connection> connectionPool = cache.getSlotPool(slot);
    if (connectionPool != null) {
      // It can't guaranteed to get valid connection because of node assignment
      return connectionPool.getResource();
    } else {
      // It's abnormal situation for cluster mode that we have just nothing for slot.
      // Try to rediscover state
      renewSlotCache();
      connectionPool = cache.getSlotPool(slot);
      if (connectionPool != null) {
        return connectionPool.getResource();
      } else {
        // no choice, fallback to new connection to random node
        return getConnection();
      }
    }
  }
}
