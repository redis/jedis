package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;

public abstract class JedisClusterConnectionHandler {
    protected JedisClusterInfoCache cache = new JedisClusterInfoCache();

    abstract Jedis getConnection();

    public void returnConnection(Jedis connection) {
	cache.getNode(getNodeKey(connection.getClient())).returnResource(
		connection);
    }

    public void returnBrokenConnection(Jedis connection) {
	cache.getNode(getNodeKey(connection.getClient())).returnBrokenResource(
		connection);
    }

    abstract Jedis getConnectionFromSlot(int slot);

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes) {
	initializeSlotsCache(nodes);
    }

    public Map<String, JedisPool> getNodes() {
	return cache.getNodes();
    }

    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	cache.assignSlotToNode(slot, targetNode);
    }

    private void initializeSlotsCache(Set<HostAndPort> startNodes) {
	for (HostAndPort hostAndPort : startNodes) {
	    JedisPool jp = new JedisPool(hostAndPort.getHost(),
		    hostAndPort.getPort());

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

    protected JedisPool getRandomConnection() {
	Object[] nodeArray = cache.getNodes().values().toArray();
	return (JedisPool) (nodeArray[new Random().nextInt(nodeArray.length)]);
    }

}
