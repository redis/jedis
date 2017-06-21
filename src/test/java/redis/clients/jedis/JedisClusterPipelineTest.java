package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.util.JedisClusterCRC16;

/**
 * Created by saurabh.yagnik on 6/21/17.
 */
public class JedisClusterPipelineTest {

	JedisCluster cluster = null;

	@Before
	public void setUp() throws InterruptedException {
		Set<HostAndPort> hostAndPorts = new HashSet<>();
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(0));
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(1));
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(2));
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(3));
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(4));
		hostAndPorts.add(HostAndPortUtil.getClusterServers().get(5));
		cluster = new JedisCluster(hostAndPorts);
		cluster.set("Test", "Test");
		cluster.set("Test1", "Test1");
		cluster.set("Test2", "Test2");
		cluster.set("Test3", "Test3");
		cluster.set("Test4", "Test4");
		cluster.set("Test5", "Test5");
		cluster.set("Test6", "Test6");
		cluster.set("Test7", "Test7");
	}

	@After
	public void clean() throws InterruptedException {
		cluster.del("Test");
		cluster.del("Test1");
		cluster.del("Test2");
		cluster.del("Test3");
		cluster.del("Test4");
		cluster.del("Test5");
		cluster.del("Test6");
		cluster.del("Test7");

	}

	/**
	 * This test class will test check the Jedis Connection From Slot
	 */
	@Test
	public void testGetConnectionFromSlot() {
		int slot = JedisClusterCRC16.getSlot("Test");
		Jedis jedis = cluster.getConnectionFromSlot(slot);
		assertNotNull(jedis);
	}

	/**
	 * This test class will perform the pipeline on JedisCluster by taking Jedis connection from slot
	 * Also checks the pipeline the of key not present Redis Cluster
	 */
	@Test
	public void testPipelineOnConection() {
		int slot = JedisClusterCRC16.getSlot("Test5");  // Key present in cluster
		Jedis jedis = cluster.getConnectionFromSlot(slot);
		Pipeline pipeline = jedis.pipelined();
		Response<String> response = pipeline.get("Test5");
		pipeline.sync();
		assertEquals(response.get(), "Test5");

		slot = JedisClusterCRC16.getSlot("AAA"); // Key not present in cluster
		jedis = cluster.getConnectionFromSlot(slot);
		pipeline = jedis.pipelined();
		response = pipeline.get("AAA");
		pipeline.sync();
		assertNull(response.get());
	}
}
