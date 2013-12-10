package redis.clients.jedis.tests;

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
import redis.clients.jedis.tests.utils.RedisSlot;

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
	
		// add all slots to node1
		Pipeline pipelined = node1.pipelined();
		for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
		    pipelined.clusterAddSlots(i);
		}
		pipelined.sync();
		
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
    public void throwMovedExceptionTest() {
    	node1.set("foo", "bar");
    	node2.get("foo");
    }

//    @Test(expected=JedisAskDataException.class)
//    public void ask() {
//    	node1.set("foo", "bar");
//    	int keySlot = RedisSlot.getSlot("foo");
//    	String node2Id = getNodeId(node2.clusterNodes());
//    	node1.clusterSetSlotMigrating(keySlot, node2Id);
//    	node1.get("foo");
//    }
    
    private String getNodeId(String infoOutput) {
    	for (String infoLine : infoOutput.split("\n")) {
			if (infoLine.contains("myself")) {
				return infoLine.split(" ")[0];
			}
		}
    	return "";
    }
}