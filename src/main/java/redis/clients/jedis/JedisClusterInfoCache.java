package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.SafeEncoder;

public class JedisClusterInfoCache {
	private final Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
	private final Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>();
	private final Map<HostAndPort, RedisNodeInfo> nodeInfo = new HashMap<HostAndPort, RedisNodeInfo>();

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock r = rwl.readLock();
	private final Lock w = rwl.writeLock();
	private final ReentrantLock slotsLock = new ReentrantLock();
	private final GenericObjectPoolConfig poolConfig;

	private int connectionTimeout;
	private int soTimeout;

	private static final int MASTER_NODE_INDEX = 2;

	public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig, int timeout) {
		this(poolConfig, timeout, timeout);
	}

	public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig, final int connectionTimeout,
			final int soTimeout) {
		this.poolConfig = poolConfig;
		this.connectionTimeout = connectionTimeout;
		this.soTimeout = soTimeout;
	}

	public void discoverClusterNodesAndSlots(Jedis jedis) {
		w.lock();
		try {
			reset();
			assignSlots(retriveClusterSlots(jedis));
		} finally {
			w.unlock();
		}
	}

	public void renewClusterSlots(Jedis jedis) {
		if (slotsLock.tryLock()) {
			try {
				if (jedis != null) {
					try {
						Map<HostAndPort, RedisNodeInfo> _nodeInfo = retriveClusterSlots(jedis);
						if (hasChanged(_nodeInfo, nodeInfo)) {
							assignSlots(_nodeInfo);
						}
						return;
					} catch (JedisException e) {
						// try nodes from all pools
					}
				}

				for (JedisPool jp : getShuffledNodesPool()) {
					try {
						jedis = jp.getResource();
						Map<HostAndPort, RedisNodeInfo> _nodeInfo = retriveClusterSlots(jedis);
						if (hasChanged(_nodeInfo, nodeInfo)) {
							assignSlots(_nodeInfo);
						}
						return;
					} catch (JedisConnectionException e) {
						// try next nodes
					} finally {
						if (jedis != null) {
							jedis.close();
						}
					}
				}
			} finally {
				slotsLock.unlock();
			}
		}

	}

	public JedisPool setupNodeIfNotExist(HostAndPort node) {
		w.lock();
		try {
			String nodeKey = getNodeKey(node);
			JedisPool existingPool = nodes.get(nodeKey);
			if (existingPool != null)
				return existingPool;

			JedisPool nodePool = new JedisPool(poolConfig, node.getHost(), node.getPort(), connectionTimeout, soTimeout,
					null, 0, null);
			nodes.put(nodeKey, nodePool);
			return nodePool;
		} finally {
			w.unlock();
		}
	}

	public JedisPool getNode(String nodeKey) {
		r.lock();
		try {
			return nodes.get(nodeKey);
		} finally {
			r.unlock();
		}
	}

	public JedisPool getSlotPool(int slot) {
		r.lock();
		try {
			return slots.get(slot);
		} finally {
			r.unlock();
		}
	}

	public Map<String, JedisPool> getNodes() {
		r.lock();
		try {
			return new HashMap<String, JedisPool>(nodes);
		} finally {
			r.unlock();
		}
	}

	public List<JedisPool> getShuffledNodesPool() {
		r.lock();
		try {
			List<JedisPool> pools = new ArrayList<JedisPool>(nodes.values());
			Collections.shuffle(pools);
			return pools;
		} finally {
			r.unlock();
		}
	}

	/**
	 * Clear discovered nodes collections and gently release allocated resources
	 */
	public void reset() {
		w.lock();
		try {
			for (JedisPool pool : nodes.values()) {
				try {
					if (pool != null) {
						pool.destroy();
					}
				} catch (Exception e) {
					// pass
				}
			}
			nodes.clear();
			slots.clear();
			nodeInfo.clear();
		} finally {
			w.unlock();
		}
	}

	public static String getNodeKey(HostAndPort hnp) {
		return hnp.getHost() + ":" + hnp.getPort();
	}

	public static String getNodeKey(Client client) {
		return client.getHost() + ":" + client.getPort();
	}

	public static String getNodeKey(Jedis jedis) {
		return getNodeKey(jedis.getClient());
	}

	private Map<HostAndPort, RedisNodeInfo> retriveClusterSlots(Jedis jedis) {
		Map<HostAndPort, RedisNodeInfo> _nodeInfo = new HashMap<HostAndPort, RedisNodeInfo>();
		List<Object> slots = jedis.clusterSlots();

		for (Object slotInfoObj : slots) {
			List<Object> slotInfo = (List<Object>) slotInfoObj;

			if (slotInfo.size() <= MASTER_NODE_INDEX) {
				continue;
			}

			int size = slotInfo.size();
			for (int i = MASTER_NODE_INDEX; i < size; i++) {
				List<Object> hostInfos = (List<Object>) slotInfo.get(i);
				if (hostInfos.size() <= 0) {
					continue;
				}

				HostAndPort targetNode = generateHostAndPort(hostInfos);
				if (_nodeInfo.containsKey(targetNode)) {
					_nodeInfo.get(targetNode).slots.add(new RedisNodeInfo.SlotSegment(
							((Long) slotInfo.get(0)).intValue(), ((Long) slotInfo.get(1)).intValue()));
				} else {
					RedisNodeInfo redisNodeInfo = new RedisNodeInfo(targetNode, i == MASTER_NODE_INDEX);
					redisNodeInfo.slots.add(new RedisNodeInfo.SlotSegment(((Long) slotInfo.get(0)).intValue(),
							((Long) slotInfo.get(1)).intValue()));
					_nodeInfo.put(targetNode, redisNodeInfo);
				}
			}
		}
		return _nodeInfo;
	}

	private void assignSlots(Map<HostAndPort, RedisNodeInfo> _nodeInfo) {
		w.lock();
		try {
			this.slots.clear();
			for (RedisNodeInfo node : _nodeInfo.values()) {
				JedisPool pool = setupNodeIfNotExist(node.node);
				if (node.master) {
					for (RedisNodeInfo.SlotSegment ss : node.slots) {
						for (int i = ss.slotStart; i <= ss.slotEnd; i++) {
							slots.put(Integer.valueOf(i), pool);
						}
					}
				}
			}
			nodeInfo.clear();
			nodeInfo.putAll(_nodeInfo);
		} finally {
			w.unlock();
		}
	}

	private boolean hasChanged(Map<HostAndPort, RedisNodeInfo> newNodeInfo,
			Map<HostAndPort, RedisNodeInfo> oldNodeInfo) {
		if (newNodeInfo.size() != oldNodeInfo.size())
			return true;

		for (Map.Entry<HostAndPort, RedisNodeInfo> entry : newNodeInfo.entrySet()) {
			RedisNodeInfo node = oldNodeInfo.get(entry.getKey());
			if (node == null || !node.equals(entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	private HostAndPort generateHostAndPort(List<Object> hostInfos) {
		return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)), ((Long) hostInfos.get(1)).intValue());
	}

	private static class RedisNodeInfo {
		public RedisNodeInfo(HostAndPort targetNode, boolean master) {
			this.master = master;
			this.node = targetNode;
			this.slots = new HashSet<>();
		}

		final Set<SlotSegment> slots;
		final HostAndPort node;
		final boolean master;

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof RedisNodeInfo))
				return false;

			RedisNodeInfo _node = (RedisNodeInfo) obj;

			return _node.master == master && _node.node.equals(node) && _node.slots.equals(slots);
		}

		@Override
		public String toString() {
			return node + ",master:" + master + "slots:" + slots;
		}

		private static class SlotSegment {
			private final int slotStart;
			private final int slotEnd;

			public SlotSegment(int slotStart, int slotEnd) {
				this.slotStart = slotStart;
				this.slotEnd = slotEnd;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof SlotSegment))
					return false;

				SlotSegment slots = (SlotSegment) obj;
				return slots.slotStart == slotStart && slots.slotEnd == slotEnd;
			}

			@Override
			public String toString() {
				return "start:" + slotStart + ",end:" + slotEnd;
			}

			@Override
			public int hashCode() {
				int hashCode = 1;
				hashCode = 31 * hashCode + slotStart;
				return 31 * hashCode + slotEnd;
			}
		}
	}

}
