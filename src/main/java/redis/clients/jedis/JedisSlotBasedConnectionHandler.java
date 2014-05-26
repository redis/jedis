package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisSlotBasedConnectionHandler extends
	JedisClusterConnectionHandler {

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes) {
	super(nodes);
    }

    public Jedis getConnection() {
	// In antirez's redis-rb-cluster implementation,
	// getRandomConnection always return valid connection (able to ping-pong)
	// or exception if all connections are invalid
	
	List<JedisPool> pools = getShuffledNodesPool();
	
	for (JedisPool pool : pools) {
	    Jedis jedis = null;
	    try {
		jedis = pool.getResource();
		
		if (jedis == null) {
		    continue;
		}
		    
		String result = jedis.ping();
		
		if (result.equalsIgnoreCase("pong"))
		    return jedis;

		pool.returnBrokenResource(jedis);
	    } catch (JedisConnectionException ex) {
		if (jedis != null) {
		    pool.returnBrokenResource(jedis);
		}
	    }
	}
	
	throw new JedisConnectionException("no reachable node in cluster");
    }

    @Override
    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	super.assignSlotToNode(slot, targetNode);
    }

    @Override
    public Jedis getConnectionFromSlot(int slot) {
	JedisPool connectionPool = slots.get(slot);
	if (connectionPool != null) {
	    // It can't guaranteed to get valid connection because of node assignment
	    return connectionPool.getResource();
	} else {
	    return getConnection();
	}
    }
    
    private List<JedisPool> getShuffledNodesPool() {
	List<JedisPool> pools = new ArrayList<JedisPool>();
	pools.addAll(nodes.values());
	Collections.shuffle(pools);
	return pools;
    }

}
