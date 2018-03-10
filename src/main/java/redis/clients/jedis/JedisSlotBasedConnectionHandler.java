package redis.clients.jedis;

import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

public class JedisSlotBasedConnectionHandler extends JedisClusterConnectionHandler {

  public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig poolConfig, int timeout, ReadFrom readFrom) {
    this(nodes, poolConfig, timeout, timeout, readFrom);
  }

  public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
      final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
      ReadFrom readFrom) {
    super(nodes, poolConfig, connectionTimeout, soTimeout, null, readFrom);
  }

  public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password,
      ReadFrom readFrom) {
    super(nodes, poolConfig, connectionTimeout, soTimeout, password, readFrom);
  }

  public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
      GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password,
      String clientName, ReadFrom readFrom) {
    super(nodes, poolConfig, connectionTimeout, soTimeout, password, clientName, readFrom);
  }

  @Override
  public Jedis getConnection(ReadFrom readFrom) {
    // In antirez's redis-rb-cluster implementation,
    // getRandomConnection always return valid connection (able to
    // ping-pong)
    // or exception if all connections are invalid

    List<JedisPool> pools = cache.getShuffledNodesPool(readFrom);
    for (JedisPool pool : pools) {
      Jedis jedis = null;
      try {
        jedis = pool.getResource();

        if (jedis == null) {
          continue;
        }

        String result = jedis.ping();

        if (result.equalsIgnoreCase("pong")) return jedis;

        jedis.close();
      } catch (JedisException ex) {
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    throw new JedisNoReachableClusterNodeException("No reachable node in cluster");
  }

  @Override
  public Jedis getConnectionFromSlot(int slot, ReadFrom readFrom) {
    JedisPool connectionPool = cache.getSlotPool(slot, readFrom);
    if (connectionPool != null) {
      // It can't guaranteed to get valid connection because of node
      // assignment
      return connectionPool.getResource();
    } else {
      renewSlotCache(); // It's abnormal situation for cluster mode, that we have just nothing for
                        // slot, try to rediscover state
      connectionPool = cache.getSlotPool(slot, readFrom);
      if (connectionPool != null) {
        return connectionPool.getResource();
      } else {
        // no choice, fallback to new connection to random node
        return getConnection(readFrom);
      }
    }
  }
}
