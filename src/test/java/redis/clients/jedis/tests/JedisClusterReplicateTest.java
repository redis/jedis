package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.utils.JedisClusterTestUtil;

public class JedisClusterReplicateTest {
    private static Jedis node5;
    private static Jedis node6;

    private HostAndPort nodeInfo5 = HostAndPortUtil.getClusterServers().get(4);
    private HostAndPort nodeInfo6 = HostAndPortUtil.getClusterServers().get(5);
    
    private static int TIMEOUT = 15000; // cluster-node-timeout * 3

    @Before
    public void setUp() throws InterruptedException {
	node5 = new Jedis(nodeInfo5.getHost(), nodeInfo5.getPort(), TIMEOUT);
	node5.connect();
	node5.flushAll();

	node6 = new Jedis(nodeInfo6.getHost(), nodeInfo6.getPort(), TIMEOUT);
	node6.connect();
	// cannot flushall - it will be slave

	// ---- configure cluster

	// add nodes to cluster
	node5.clusterMeet("127.0.0.1", nodeInfo6.getPort());
	
	JedisClusterTestUtil.assertNodeIsKnown(node5, JedisClusterTestUtil.getNodeId(node6.clusterNodes()), 1000);
	JedisClusterTestUtil.assertNodeIsKnown(node6, JedisClusterTestUtil.getNodeId(node5.clusterNodes()), 1000);

	// split available slots across the three nodes
	int[] node5Slots = new int[JedisCluster.HASHSLOTS];
	for (int i = 0 ; i < JedisCluster.HASHSLOTS; i++) {
	    node5Slots[i] = i;
	}

	node5.clusterAddSlots(node5Slots);

	JedisClusterTestUtil.waitForClusterReady(node5);

	// replicate full 1on1
	node6.clusterReplicate(JedisClusterTestUtil.getNodeId(node5
		.clusterNodes()));

	Map<Jedis, Jedis> replMap = new HashMap<Jedis, Jedis>();
	replMap.put(node5, node6);

	waitForReplicateReady(replMap, TIMEOUT);
	JedisClusterTestUtil.waitForClusterReady(node5, node6);
    }

    private void waitForReplicateReady(Map<Jedis, Jedis> replMap, int timeoutMs) {
	int interval = 100;

	for (int timeout = 0; timeout <= timeoutMs; timeout += interval) {
	    for (Entry<Jedis, Jedis> entry : replMap.entrySet()) {
		Jedis master = entry.getKey();
		Jedis slave = entry.getValue();

		String masterNodeId = JedisClusterTestUtil.getNodeId(master
			.clusterNodes());
		String slaveNodeId = JedisClusterTestUtil.getNodeId(slave
			.clusterNodes());

		try {
    		List<String> slaves = master.clusterSlaves(masterNodeId);
    
    		if (slaves.size() > 0 && slaves.get(0).contains(slaveNodeId)) {
    		    return;
    		}
		} catch (JedisDataException e) {
		    if (!e.getMessage().startsWith("ERR The specified node is not a master"))
			throw e;
		    
		    // retry...
		}
	    }

	    try {
		Thread.sleep(interval);
	    } catch (InterruptedException e) {
	    }
	}

	throw new JedisException("there seems to replication error");
    }

    @After
    public void tearDown() throws InterruptedException {
	// clear all slots
	int[] slotsToDelete = new int[JedisCluster.HASHSLOTS];
	for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
	    slotsToDelete[i] = i;
	}

	node5.clusterDelSlots(slotsToDelete);
    }

    @Test
    public void testClusterReplicate() {
	// we're already replicate 1on1
	List<String> slaveInfos = node5.clusterSlaves(JedisClusterTestUtil
		.getNodeId(node5.clusterNodes()));
	assertEquals(1, slaveInfos.size());
	assertTrue(slaveInfos.get(0).contains(
		JedisClusterTestUtil.getNodeId(node6.clusterNodes())));
    }

    @Test
    public void testClusterFailover() throws InterruptedException {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort(nodeInfo5.getHost(), nodeInfo5.getPort()));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	
	jc.set("51", "foo");
	// node5 is responsible of taking care of slot for key "51" (7186)
	
	node6.clusterFailover();

	try {
	    // wait for failover
	    Map<Jedis, Jedis> replMap = new HashMap<Jedis, Jedis>();
	    replMap.put(node6, node5);
	    waitForReplicateReady(replMap, TIMEOUT);
	    JedisClusterTestUtil.waitForClusterReady(node5, node6);

	    List<String> slaveInfos = node6.clusterSlaves(JedisClusterTestUtil
		    .getNodeId(node6.clusterNodes()));
	    assertEquals(1, slaveInfos.size());
	    assertTrue(slaveInfos.get(0).contains(
		    JedisClusterTestUtil.getNodeId(node5.clusterNodes())));
	} finally {
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
	    }
	    
	    // rollback
	    node5.clusterFailover();
	    
	    Map<Jedis, Jedis> replMap = new HashMap<Jedis, Jedis>();
	    replMap.put(node5, node6);
	    waitForReplicateReady(replMap, TIMEOUT);
	    JedisClusterTestUtil.waitForClusterReady(node5, node6);
	}
    }
}
