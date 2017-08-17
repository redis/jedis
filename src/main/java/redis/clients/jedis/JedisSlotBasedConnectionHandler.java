package redis.clients.jedis;

import java.util.List;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Set;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

public class JedisSlotBasedConnectionHandler extends JedisClusterConnectionHandler {

	public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
			int timeout) {
		this(nodes, poolConfig, timeout, timeout);
	}

	public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
			int connectionTimeout, int soTimeout) {
		super(nodes, poolConfig, connectionTimeout, soTimeout, null);
	}

	public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
			int connectionTimeout, int soTimeout, String password) {
		super(nodes, poolConfig, connectionTimeout, soTimeout, password);
	}

	public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig,
			int connectionTimeout, int soTimeout, String password, String clientName) {
		super(nodes, poolConfig, connectionTimeout, soTimeout, password, clientName);
	}

	@Override
	public Jedis getConnection() {

		return getJedisPool().getResource();

	}

	@Override
	public Jedis getConnectionFromSlot(int slot) {
		return getJedisPoolFromSlot(slot).getResource();
	}

	@Override
	public JedisPool getJedisPoolFromSlot(int slot) {
		JedisPool connectionPool = cache.getSlotPool(slot);
		if (connectionPool != null) {
			// It can't guaranteed to get valid connection because of node
			// assignment
			return connectionPool;
		} else {
			renewSlotCache(); // It's abnormal situation for cluster mode, that we have just nothing for slot,
								// try to rediscover state
			connectionPool = cache.getSlotPool(slot);
			if (connectionPool != null) {
				return connectionPool;
			} else {
				// no choice, fallback to new connection to random node
				return getJedisPool();
			}
		}
	}

	@Override
	public JedisPool getJedisPool() {
		// In antirez's redis-rb-cluster implementation,
		// getRandomConnection always return valid connection (able to
		// ping-pong)
		// or exception if all connections are invalid
		List<JedisPool> pools = cache.getShuffledNodesPool();

		for (JedisPool pool : pools) {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();

				if (jedis == null) {
					continue;
				}

				String result = jedis.ping();
				if (result.equalsIgnoreCase("pong"))
					return pool;

				jedis.close();
			} catch (JedisException ex) {
				if (jedis != null) {
					jedis.close();
				}
			}
		}
		throw new JedisNoReachableClusterNodeException("No reachable node in cluster");
	}
}
