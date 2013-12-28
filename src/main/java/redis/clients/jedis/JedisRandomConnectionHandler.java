package redis.clients.jedis;

import java.util.Random;
import java.util.Set;

import redis.clients.util.Pool;

public class JedisRandomConnectionHandler extends JedisClusterConnectionHandler {

	
	public JedisRandomConnectionHandler(Set<HostAndPort> nodes) {
		super(nodes);
	}

	
	@SuppressWarnings("unchecked")
	public Jedis getConnection() {
		Object[] nodeArray =  nodes.values().toArray();
		return ((Pool<Jedis>) nodeArray[new Random().nextInt(nodeArray.length)]).getResource();
	}

}
