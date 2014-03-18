package redis.clients.jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisClusterConnectionHandler {

    protected Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
    protected Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>();

    abstract Jedis getConnection();

    protected void returnConnection(Jedis connection) {
	nodes.get(getNodeKey(connection.getClient()))
		.returnResource(connection);
    }

    public void returnBrokenConnection(Jedis connection) {
	nodes.get(getNodeKey(connection.getClient())).returnBrokenResource(
		connection);
    }

    abstract Jedis getConnectionFromSlot(int slot);

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes) {
	initializeSlotsCache(nodes);
    }

    public Map<String, JedisPool> getNodes() {
	return nodes;
    }

    private void initializeSlotsCache(Set<HostAndPort> startNodes) {
	for (HostAndPort hostAndPort : startNodes) {
	    JedisPool jp = new JedisPool(hostAndPort.getHost(),
		    hostAndPort.getPort());

	    this.nodes.clear();
	    this.slots.clear();

	    Jedis jedis = null;
	    try {
		jedis = jp.getResource();
		discoverClusterNodesAndSlots(jedis);
		break;
	    } catch (JedisConnectionException e) {
		if (jedis != null) {
		    jp.returnBrokenResource(jedis);
		    jedis = null;
		}

		// try next nodes
	    } finally {
		if (jedis != null) {
		    jp.returnResource(jedis);
		}
	    }
	}

	for (HostAndPort node : startNodes) {
	    setNodeIfNotExist(node);
	}
    }
    
    private void discoverClusterNodesAndSlots(Jedis jedis) {
	String localNodes = jedis.clusterNodes();
	for (String nodeInfo : localNodes.split("\n")) {
	    HostAndPort node = getHostAndPortFromNodeLine(nodeInfo, jedis);
	    setNodeIfNotExist(node);
	    
	    JedisPool nodePool = nodes.get(getNodeKey(node));
	    populateNodeSlots(nodeInfo, nodePool);
	}
    }
    
    private void setNodeIfNotExist(HostAndPort node) {
	String nodeKey = getNodeKey(node);
	if (nodes.containsKey(nodeKey))
	    return;
	
	JedisPool nodePool = new JedisPool(node.getHost(), node.getPort());
	nodes.put(nodeKey, nodePool);
    }

    private void populateNodeSlots(String nodeInfo, JedisPool nodePool) {
	String[] nodeInfoArray = nodeInfo.split(" ");
	if (nodeInfoArray.length > 7) {
	    for (int i = 8; i < nodeInfoArray.length; i++) {
		processSlot(nodeInfoArray[i], nodePool);
	    }
	}
    }

    private void processSlot(String slot, JedisPool nodePool) {
	if (slot.contains("-")) {
	    String[] slotRange = slot.split("-");
	    for (int i = Integer.valueOf(slotRange[0]); i <= Integer
		    .valueOf(slotRange[1]); i++) {
		slots.put(i, nodePool);
	    }
	} else {
	    slots.put(Integer.valueOf(slot), nodePool);
	}
    }

    private HostAndPort getHostAndPortFromNodeLine(String nodeInfo,
	    Jedis currentConnection) {
	String stringHostAndPort = nodeInfo.split(" ", 3)[1];
	if (":0".equals(stringHostAndPort)) {
	    return new HostAndPort(currentConnection.getClient().getHost(),
		    currentConnection.getClient().getPort());
	}
	String[] arrayHostAndPort = stringHostAndPort.split(":");
	return new HostAndPort(arrayHostAndPort[0],
		Integer.valueOf(arrayHostAndPort[1]));
    }

    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	JedisPool targetPool = nodes.get(getNodeKey(targetNode));

	if (targetPool != null) {
	    slots.put(slot, targetPool);
	} else {
	    setNodeIfNotExist(targetNode);
	    
	    targetPool = nodes.get(getNodeKey(targetNode));
	    slots.put(slot, targetPool);
	}
    }

    protected JedisPool getRandomConnection() {
	Object[] nodeArray = nodes.values().toArray();
	return (JedisPool) (nodeArray[new Random().nextInt(nodeArray.length)]);
    }

    protected String getNodeKey(HostAndPort hnp) {
	return hnp.getHost() + ":" + hnp.getPort();
    }

    protected String getNodeKey(Client client) {
	return client.getHost() + ":" + client.getPort();
    }
}
