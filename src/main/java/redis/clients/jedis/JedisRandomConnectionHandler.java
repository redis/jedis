package redis.clients.jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import redis.clients.util.Pool;

public class JedisRandomConnectionHandler implements JedisClusterConnectionHandler {

	private Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
	
	public JedisRandomConnectionHandler(Set<HostAndPort> nodes) {
		initializeSlotsCache(nodes);
	}

	private void initializeSlotsCache(Set<HostAndPort> nodes) {
		for (HostAndPort hostAndPort : nodes) {
			JedisPool jp = new JedisPool(hostAndPort.getHost(),
					hostAndPort.getPort());
			this.nodes.put(hostAndPort.getHost() + hostAndPort.getPort(), jp);
		}

	}
	
	@SuppressWarnings("unchecked")
	public Jedis getConnection() {
		Object[] nodeArray =  nodes.values().toArray();
		return ((Pool<Jedis>) nodeArray[new Random().nextInt(nodeArray.length)]).getResource();
	}

}
