package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Set;

public class JedisSlotBasedConnectionHandler extends
	JedisClusterConnectionHandler {

    private Jedis currentConnection;

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
        final GenericObjectPoolConfig poolConfig) {
	super(nodes, poolConfig);
    }

    public Jedis getConnection() {
	return currentConnection != null ? currentConnection
		: getRandomConnection().getResource();
    }

    private void returnCurrentConnection() {
	if (currentConnection != null) {
	    nodes.get(
		    currentConnection.getClient().getHost()
			    + currentConnection.getClient().getPort())
		    .returnResource(currentConnection);
	}

    }

    @Override
    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	super.assignSlotToNode(slot, targetNode);
	getConnectionFromSlot(slot);
    }

    @Override
    public Jedis getConnectionFromSlot(int slot) {
	returnCurrentConnection();
	JedisPool connectionPool = slots.get(slot);
	if (connectionPool == null) {
	    connectionPool = getRandomConnection();
	}
	currentConnection = connectionPool.getResource();
	return currentConnection;
    }

}
