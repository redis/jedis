package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.FailSafeJedisCluster;
import redis.clients.util.FailSafeJedisFactory;
import redis.clients.jedis.JedisServerInfo;

public class FailSafeJedisTest extends Assert {
	private final HostAndPort hnp0 = HostAndPortUtil.getRedisServers().get(0);
	private final HostAndPort hnp1 = HostAndPortUtil.getRedisServers().get(1);
	
	private Jedis jedis0, jedis1;
	
	@Before
	public void setUp() {
		jedis0 = new Jedis(hnp0.host, hnp0.port);
		jedis0.auth("foobared");
		jedis1 = new Jedis(hnp1.host, hnp1.port);
		jedis1.auth("foobared");
	}

	@After
	public void tearDown() {
		jedis0.slaveofNoOne();
		jedis1.slaveofNoOne();

		jedis0.disconnect();
		jedis1.disconnect();
	}

	@Test
	public void masterInstanceIsElected() {
		FailSafeJedisCluster cluster = new FailSafeJedisCluster(
				new JedisServerInfo(hnp0.host, hnp0.port, "foobared"), 
				new JedisServerInfo(hnp1.host, hnp1.port, "foobared"));

		jedis0.slaveof(hnp1.host, hnp1.port);
		cluster.electNewMaster();
		assertEquals(hnp1.host, cluster.getMaster().getHost());

		jedis1.slaveof(hnp0.host, hnp0.port);
		jedis0.slaveofNoOne();
		cluster.electNewMaster();
		assertEquals(hnp0.host, cluster.getMaster().getHost());
	}
	
	@Test
	public void liveInstanceIsElected() {
		FailSafeJedisCluster cluster = new FailSafeJedisCluster(
				new JedisServerInfo(hnp0.host, hnp0.port, "foobared"), 
				new JedisServerInfo("localhost", 9999, null));
		assertEquals(hnp0.host, cluster.getMaster().getHost());
	}
	 
	@Test
	public void aCommandIsExecutedAgainstElectedMaster() throws Exception {
		FailSafeJedisCluster cluster = new FailSafeJedisCluster(
				new JedisServerInfo(hnp0.host, hnp0.port, "foobared"), 
				new JedisServerInfo(hnp1.host, hnp1.port, "foobared"));

		FailSafeJedisFactory factory = new FailSafeJedisFactory(cluster);
		JedisCommands jedis = (JedisCommands) factory.makeObject();
		
		jedis0.slaveof(hnp1.host, hnp1.port);
		
		assertEquals("OK", jedis.set("foo", "foo"));
		assertEquals(hnp1.host, cluster.getMaster().getHost());

		jedis0.slaveofNoOne();
		jedis1.slaveof(hnp0.host, hnp0.port);
		assertEquals("OK", jedis.set("bar", "bar"));
		assertEquals(hnp0.host, cluster.getMaster().getHost());
		
	}

}
