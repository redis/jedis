package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.utils.JedisClusterTestUtil;
import redis.clients.util.JedisClusterCRC16;

public class JedisClusterReplicateTest {
    private Jedis node1;
    private static Jedis node2;
    private static Jedis node3;
    private static Jedis node4;
    private static Jedis node5;
    private static Jedis node6;

    private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
    private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
    private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
    private HostAndPort nodeInfo4 = HostAndPortUtil.getClusterServers().get(3);
    private HostAndPort nodeInfo5 = HostAndPortUtil.getClusterServers().get(4);
    private HostAndPort nodeInfo6 = HostAndPortUtil.getClusterServers().get(5);

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

	node4 = new Jedis(nodeInfo4.getHost(), nodeInfo4.getPort());
	node4.connect();
	// cannot flushall - it will be slave

	node5 = new Jedis(nodeInfo5.getHost(), nodeInfo5.getPort());
	node5.connect();
	// cannot flushall - it will be slave

	node6 = new Jedis(nodeInfo6.getHost(), nodeInfo6.getPort());
	node6.connect();
	// cannot flushall - it will be slave

	// ---- configure cluster

	// add nodes to cluster
	node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
	node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());
	node1.clusterMeet("127.0.0.1", nodeInfo4.getPort());
	node1.clusterMeet("127.0.0.1", nodeInfo5.getPort());
	node1.clusterMeet("127.0.0.1", nodeInfo6.getPort());

	// split available slots across the three nodes
	int slotsPerNode = JedisCluster.HASHSLOTS / 3;
	int[] node1Slots = new int[slotsPerNode];
	int[] node2Slots = new int[slotsPerNode + 1];
	int[] node3Slots = new int[slotsPerNode];
	for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < JedisCluster.HASHSLOTS; i++) {
	    if (i < slotsPerNode) {
		node1Slots[slot1++] = i;
	    } else if (i > slotsPerNode * 2) {
		node3Slots[slot3++] = i;
	    } else {
		node2Slots[slot2++] = i;
	    }
	}

	node1.clusterAddSlots(node1Slots);
	node2.clusterAddSlots(node2Slots);
	node3.clusterAddSlots(node3Slots);

	JedisClusterTestUtil.waitForClusterReady(node1, node2, node3);

	// replicate full 1on1
	node4.clusterReplicate(JedisClusterTestUtil.getNodeId(node1
		.clusterNodes()));
	node5.clusterReplicate(JedisClusterTestUtil.getNodeId(node2
		.clusterNodes()));
	node6.clusterReplicate(JedisClusterTestUtil.getNodeId(node3
		.clusterNodes()));

	Map<Jedis, Jedis> replMap = new HashMap<Jedis, Jedis>();
	replMap.put(node1, node4);
	replMap.put(node2, node5);
	replMap.put(node3, node6);

	waitForReplicateReady(replMap, 2000);
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

		List<String> slaves = master.clusterSlaves(masterNodeId);

		if (slaves.size() > 0 && slaves.get(0).contains(slaveNodeId)) {
		    return;
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

	node1.clusterDelSlots(slotsToDelete);
	node2.clusterDelSlots(slotsToDelete);
	node3.clusterDelSlots(slotsToDelete);

	List<Jedis> masters = new ArrayList<Jedis>();
	masters.add(node1);
	masters.add(node2);
	masters.add(node3);

	List<Jedis> slaves = new ArrayList<Jedis>();
	slaves.add(node4);
	slaves.add(node5);
	slaves.add(node6);

	for (Jedis master : masters) {
	    for (Jedis slave : slaves) {
		master.clusterForget(JedisClusterTestUtil.getNodeId(slave
			.clusterNodes()));
	    }
	}
    }

    @Test
    public void testClusterReplicate() {
	// we're already replicate 1on1
	List<String> slaveInfos = node1.clusterSlaves(JedisClusterTestUtil
		.getNodeId(node1.clusterNodes()));
	assertEquals(1, slaveInfos.size());
	assertTrue(slaveInfos.get(0).contains(
		JedisClusterTestUtil.getNodeId(node4.clusterNodes())));

	slaveInfos = node2.clusterSlaves(JedisClusterTestUtil.getNodeId(node2
		.clusterNodes()));
	assertEquals(1, slaveInfos.size());
	assertTrue(slaveInfos.get(0).contains(
		JedisClusterTestUtil.getNodeId(node5.clusterNodes())));

	slaveInfos = node3.clusterSlaves(JedisClusterTestUtil.getNodeId(node3
		.clusterNodes()));
	assertEquals(1, slaveInfos.size());
	assertTrue(slaveInfos.get(0).contains(
		JedisClusterTestUtil.getNodeId(node6.clusterNodes())));
    }

    @Test
    @Ignore
    // FIXME: There is some error from cluster manual failover
    // by Redis unstable, commit 0f2597092fdcd17547282bd434e46d3dc8412e23
    public void testClusterFailover() {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	
	jc.set("51", "foo");
	// node2 is responsible of taking care of slot for key "51" (7186)
	
	node5.clusterFailover();

	// wait for failover
	Map<Jedis, Jedis> replMap = new HashMap<Jedis, Jedis>();
	replMap.put(node4, node1);
	waitForReplicateReady(replMap, 2000);

	try {
	    List<String> slaveInfos = node5.clusterSlaves(JedisClusterTestUtil
		    .getNodeId(node5.clusterNodes()));
	    assertEquals(1, slaveInfos.size());
	    assertTrue(slaveInfos.get(0).contains(
		    JedisClusterTestUtil.getNodeId(node2.clusterNodes())));
	} finally {
	    // rollback
	    node2.clusterFailover();
	}
    }
}
