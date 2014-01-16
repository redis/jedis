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
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.util.JedisClusterCRC16;

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
		
		
		waitForClusterReady();
    }


	@After
    public void tearDown() {
		// clear all slots
    	int[] slotsToDelete = new int[JedisCluster.HASHSLOTS];
    	for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
			slotsToDelete[i] = i;
		}
		node1.clusterDelSlots(slotsToDelete);
		node2.clusterDelSlots(slotsToDelete);
		node3.clusterDelSlots(slotsToDelete);
    }

    @Test(expected=JedisMovedDataException.class)
    public void testThrowMovedException() {
    	node1.set("foo", "bar");
    }
    
    @Test
    public void testMovedExceptionParameters() {
    	try {
    		node1.set("foo", "bar");
    	} catch (JedisMovedDataException jme) {
    		assertEquals(12182, jme.getSlot());
    		assertEquals(new HostAndPort("127.0.0.1", 7381), jme.getTargetNode());
    	}
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
    
    @Test
    public void testRecalculateSlotsWhenMoved() throws InterruptedException {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	int slot51 = JedisClusterCRC16.getSlot("51");
		node2.clusterDelSlots(slot51);
    	node3.clusterDelSlots(slot51);
    	node3.clusterAddSlots(slot51);
    	
    	waitForClusterReady();
    	jc.set("51", "foo");
    	assertEquals("foo", jc.get("51"));
    }
    
    @Test
    public void testAskResponse() throws InterruptedException {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	int slot51 = JedisClusterCRC16.getSlot("51");
		node3.clusterSetSlotImporting(slot51, getNodeId(node2.clusterNodes()));
    	node2.clusterSetSlotMigrating(slot51, getNodeId(node3.clusterNodes()));
    	jc.set("51", "foo");
    	assertEquals("foo", jc.get("51"));
    }
    
    @Test(expected=JedisClusterException.class)
    public void testThrowExceptionWithoutKey() {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	jc.ping();
    }
    
    @Test(expected=JedisClusterMaxRedirectionsException.class)
    public void testRedisClusterMaxRedirections() {
    	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    	JedisCluster jc = new JedisCluster(jedisClusterNode);
    	int slot51 = JedisClusterCRC16.getSlot("51");
    	//This will cause an infinite redirection loop
		node2.clusterSetSlotMigrating(slot51, getNodeId(node3.clusterNodes()));
    	jc.set("51", "foo");
    }
    
    private String getNodeId(String infoOutput) {
    	for (String infoLine : infoOutput.split("\n")) {
			if (infoLine.contains("myself")) {
				return infoLine.split(" ")[0];
			}
		}
    	return "";
    }
    
    private void waitForClusterReady() throws InterruptedException {
		boolean clusterOk = false;
		while (!clusterOk) {
			if (node1.clusterInfo().split("\n")[0].contains("ok") &&
				node2.clusterInfo().split("\n")[0].contains("ok") &&
				node3.clusterInfo().split("\n")[0].contains("ok") ) {
				clusterOk = true;
			}
			Thread.sleep(50);
		}
	}
    
}