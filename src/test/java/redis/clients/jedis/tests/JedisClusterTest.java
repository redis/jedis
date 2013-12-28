package redis.clients.jedis.tests;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.tests.utils.JedisClusterCRC16;

public class JedisClusterTest extends Assert {
    private Jedis node1;
    private Jedis node2;
    private Jedis node3;

    private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
    private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
    private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);

    @Before
    public void setUp() throws InterruptedException {
		node1 = new Jedis(nodeInfo1.getHost(), nodeInfo1.getPort());
		node1.connect();
		node1.flushAll();
	
		node2 = new Jedis(nodeInfo2.getHost(), nodeInfo2.getPort());
		node2.connect();
		node2.flushAll();
	
		node3 = new Jedis(nodeInfo3.getHost(), nodeInfo3.getPort());
		node3.connect();
		node3.flushAll();
	
		// ---- configure cluster
	
		// add nodes to cluster
		node1.clusterMeet("127.0.0.1", nodeInfo1.getPort());
		node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
		node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());
	
		// split available slots across the three nodes
		int slotsPerNode = JedisCluster.HASHSLOTS / 3;
		Pipeline pipeline1 = node1.pipelined();
		Pipeline pipeline2 = node2.pipelined();
		Pipeline pipeline3 = node3.pipelined();
		for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
			if (i < slotsPerNode) {
				pipeline1.clusterAddSlots(i);
			} else if (i > slotsPerNode * 2) {
				pipeline3.clusterAddSlots(i);
			} else {
				pipeline2.clusterAddSlots(i);
			}
		}
		pipeline1.sync();
		pipeline2.sync();
		pipeline3.sync();
		
		
		boolean clusterOk = false;
		while (!clusterOk) {
			if (node1.clusterInfo().split("\n")[0].contains("ok") &&
				node2.clusterInfo().split("\n")[0].contains("ok") &&
				node3.clusterInfo().split("\n")[0].contains("ok") ) {
				clusterOk = true;
			}
			Thread.sleep(100);
		}
    }

    @After
    public void tearDown() {
		// clear all slots of node1
		Pipeline pipelined = node1.pipelined();
		for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
		    pipelined.clusterDelSlots(i);
		}
		pipelined.sync();
    }

    @Test(expected=JedisMovedDataException.class)
    public void testThrowMovedException() {
    	node1.set("foo", "bar");
    }

    @Test(expected=JedisAskDataException.class)
    public void testThrowAskException() {
    	int keySlot = JedisClusterCRC16.getSlot("test");
    	String node3Id = getNodeId(node3.clusterNodes());
    	node2.clusterSetSlotMigrating(keySlot, node3Id);
    	node2.get("test");
    }
    
    @Test
    public void testDiscoverNodesAutomatically() {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	assertEquals(jc.getClusterNodes().size(), 3);
    }
    
    @Test
    public void testCalculateConnectionPerSlot() {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	jc.set("foo", "bar");
    	jc.set("test", "test");
    	assertEquals("bar",node3.get("foo"));
    	assertEquals("test",node2.get("test"));
    	
    }
    
    private String getNodeId(String infoOutput) {
    	for (String infoLine : infoOutput.split("\n")) {
			if (infoLine.contains("myself")) {
				return infoLine.split(" ")[0];
			}
		}
    	return "";
    }
}