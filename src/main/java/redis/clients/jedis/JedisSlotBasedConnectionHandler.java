package redis.clients.jedis;

import java.util.Set;

public class JedisSlotBasedConnectionHandler extends
	JedisClusterConnectionHandler {

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes) {
	super(nodes);
    }

    public Jedis getConnection() {
	return getRandomConnection().getResource();
    }

    @Override
    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	super.assignSlotToNode(slot, targetNode);
	getConnectionFromSlot(slot);
    }

    @Override
    public Jedis getConnectionFromSlot(int slot) {
	JedisPool connectionPool = slots.get(slot);
	if (connectionPool == null) {
	    connectionPool = getRandomConnection();
	}
	return  connectionPool.getResource();
    }

}
