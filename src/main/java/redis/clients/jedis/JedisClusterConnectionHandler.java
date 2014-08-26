package redis.clients.jedis;

import java.util.*;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;

public abstract class JedisClusterConnectionHandler {
    public static ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();

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
	    ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(
		    nodeInfo, new HostAndPort(jedis.getClient().getHost(),
			    jedis.getClient().getPort()));

	    HostAndPort targetNode = clusterNodeInfo.getNode();
	    setNodeIfNotExist(targetNode);
	    assignSlotsToNode(clusterNodeInfo.getAvailableSlots(), targetNode);
	}
    }

    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	JedisPool targetPool = nodes.get(getNodeKey(targetNode));

	if (targetPool == null) {
	    setNodeIfNotExist(targetNode);
	    targetPool = nodes.get(getNodeKey(targetNode));
	}
	slots.put(slot, targetPool);
    }

    public void assignSlotsToNode(List<Integer> targetSlots,
	    HostAndPort targetNode) {
	JedisPool targetPool = nodes.get(getNodeKey(targetNode));

	if (targetPool == null) {
	    setNodeIfNotExist(targetNode);
	    targetPool = nodes.get(getNodeKey(targetNode));
	}

	for (Integer slot : targetSlots) {
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

    private void setNodeIfNotExist(HostAndPort node) {
	String nodeKey = getNodeKey(node);
	if (nodes.containsKey(nodeKey))
	    return;

	JedisPool nodePool = new JedisPool(node.getHost(), node.getPort());
	nodes.put(nodeKey, nodePool);
    }
}
