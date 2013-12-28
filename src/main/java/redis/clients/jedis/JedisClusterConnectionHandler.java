package redis.clients.jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class JedisClusterConnectionHandler {
	
	protected Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
	
	abstract Jedis getConnection();
	
	public JedisClusterConnectionHandler(Set<HostAndPort> nodes) {
		initializeSlotsCache(nodes);
	}
	
	public Map<String, JedisPool> getNodes() {
		return nodes;
	}
	
	private void initializeSlotsCache(Set<HostAndPort> nodes) {
		for (HostAndPort hostAndPort : nodes) {
			JedisPool jp = new JedisPool(hostAndPort.getHost(),	hostAndPort.getPort());
			this.nodes.put(hostAndPort.getHost() + hostAndPort.getPort(), jp);
			this.nodes.putAll(discoverClusterNodes(jp));
		}

	}

	private Map<? extends String, ? extends JedisPool> discoverClusterNodes(JedisPool jp) {
		Map<String, JedisPool> discoveredNodes = new HashMap<String, JedisPool>();
		String localNodes = jp.getResource().clusterNodes();
		for (String nodeInfo : localNodes.split("\n")) {
			HostAndPort node = getHostAndPortFromNodeLine(nodeInfo);
			JedisPool nodePool = new JedisPool(node.getHost(), node.getPort());
			discoveredNodes.put(node.getHost() + node.getPort(), nodePool);
		}
		return discoveredNodes;
	}

	private HostAndPort getHostAndPortFromNodeLine(String nodeInfo) {
		String stringHostAndPort = nodeInfo.split(" ",3)[1];
		String[] arrayHostAndPort = stringHostAndPort.split(":");
		return new HostAndPort(arrayHostAndPort[0], Integer.valueOf(arrayHostAndPort[1]));
	}
}
