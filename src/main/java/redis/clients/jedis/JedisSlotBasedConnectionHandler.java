package redis.clients.jedis;

import java.util.Random;
import java.util.Set;

import redis.clients.jedis.tests.utils.JedisClusterCRC16;

public class JedisSlotBasedConnectionHandler extends JedisClusterConnectionHandler {

	
	public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes) {
		super(nodes);
	}

	
	public Jedis getConnection(String key) {
		int keySlot = JedisClusterCRC16.getSlot(key);
		JedisPool connectionPool = slots.get(keySlot);
		if (connectionPool == null) {
			connectionPool = getRandomConnection();
		}
		return connectionPool.getResource();
	}
	
	private JedisPool getRandomConnection() {
		Object[] nodeArray =  nodes.values().toArray();
		return (JedisPool) (nodeArray[new Random().nextInt(nodeArray.length)]);
	}

}
