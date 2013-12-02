package redis.clients.jedis.tests.commands;

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
}