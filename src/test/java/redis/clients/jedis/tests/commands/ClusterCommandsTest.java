package redis.clients.jedis.tests.commands;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.JedisTestBase;

public class ClusterCommandsTest extends JedisTestBase {
    private Jedis node1;
    private Jedis node2;

    private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
    private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);

    @Before
    public void setUp() throws Exception {

	node1 = new Jedis(nodeInfo1.getHost(), nodeInfo1.getPort());
	node1.connect();
	node1.flushAll();

	node2 = new Jedis(nodeInfo2.getHost(), nodeInfo2.getPort());
	node2.connect();
	node2.flushAll();
    }

    @After
    public void tearDown() {
	node1.disconnect();
	node2.disconnect();
    }

    @Test
    public void clusterNodes() {
	String nodes = node1.clusterNodes();
	assertTrue(nodes.split("\n").length > 0);
    }

    @Test
    public void clusterMeet() {
	String status = node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
	assertEquals("OK", status);
    }

    @Test
    public void clusterAddSlots() {
	String status = node1.clusterAddSlots(1, 2, 3, 4, 5);
	assertEquals("OK", status);
    }

    @Test
    public void clusterDelSlots() {
	node1.clusterAddSlots(900);
	String status = node1.clusterDelSlots(900);
	assertEquals("OK", status);
    }

    @Test
    public void clusterInfo() {
	String info = node1.clusterInfo();
	assertNotNull(info);
    }

    @Test
    public void clusterGetKeysInSlot() {
	node1.clusterAddSlots(500);
	List<String> keys = node1.clusterGetKeysInSlot(500, 1);
	assertEquals(0, keys.size());
    }

    @Test
    public void clusterSetSlotNode() {
	String[] nodes = node1.clusterNodes().split("\n");
	String nodeId = nodes[0].split(" ")[0];
	String status = node1.clusterSetSlotNode(10000, nodeId);
	assertEquals("OK", status);
    }

    @Test
    public void clusterSetSlotMigrating() {
	node1.clusterAddSlots(5000);
	String[] nodes = node1.clusterNodes().split("\n");
	String nodeId = nodes[0].split(" ")[0];
	String status = node1.clusterSetSlotMigrating(5000, nodeId);
	assertEquals("OK", status);
    }

    @Test
    public void clusterSetSlotImporting() {
	node2.clusterAddSlots(6000);
	String[] nodes = node1.clusterNodes().split("\n");
	String nodeId = nodes[0].split(" ")[0];
	String status = node1.clusterSetSlotImporting(6000, nodeId);
	assertEquals("OK", status);
    }
}