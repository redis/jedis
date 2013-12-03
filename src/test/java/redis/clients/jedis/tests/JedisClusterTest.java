package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

public class JedisClusterTest extends Assert {
    private Jedis node1;
    private Jedis node2;
    private Jedis node3;

    private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
    private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
    private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);

    @Before
    public void setUp() {
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

	// add all slots to node1
	Pipeline pipelined = node1.pipelined();
	for (int i = 0; i < JedisCluster.HASH_SLOTS; i++) {
	    pipelined.clusterAddSlots(i);
	}
	pipelined.sync();
    }

    @After
    public void tearDown() {
	// clear all slots of node1
	Pipeline pipelined = node1.pipelined();
	for (int i = 0; i < JedisCluster.HASH_SLOTS; i++) {
	    pipelined.clusterDelSlots(i);
	}
	pipelined.sync();
    }

    @Test
    public void moved() {
	//TODO: needs to implement
    }

    @Test
    public void ask() {
	//TODO: needs to implement
    }
}