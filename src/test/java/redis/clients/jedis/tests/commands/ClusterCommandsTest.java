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

    @Before
    public void setUp() throws Exception {
	HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
	HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);

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
	assertEquals(1, nodes.split("\n").length);
    }
}