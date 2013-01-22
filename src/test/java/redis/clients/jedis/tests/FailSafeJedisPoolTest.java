package redis.clients.jedis.tests;

import junit.framework.Assert;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.FailSafeJedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisFacade;
import redis.clients.jedis.JedisServerInfo;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.FailSafeJedisCluster;

public class FailSafeJedisPoolTest extends Assert {
	private final HostAndPort hnp0 = HostAndPortUtil.getRedisServers().get(0);
	private final HostAndPort hnp1 = HostAndPortUtil.getRedisServers().get(1);

	private Jedis jedis0, jedis1;
	private FailSafeJedisCluster cluster;

	@Before
	public void setUp() {
		jedis0 = new Jedis(hnp0.host, hnp0.port);
		jedis0.auth("foobared");
		jedis1 = new Jedis(hnp1.host, hnp1.port);
		jedis1.auth("foobared");

		cluster = new FailSafeJedisCluster(new JedisServerInfo(hnp0.host,
				hnp0.port, "foobared"), new JedisServerInfo(hnp1.host,
				hnp1.port, "foobared"));
	}

	@After
	public void tearDown() {
		jedis0.slaveofNoOne();
		jedis1.slaveofNoOne();

		jedis0.disconnect();
		jedis1.disconnect();
	}

	@Test
	public void connectionFromPoolShouldWork() {
		FailSafeJedisPool pool = new FailSafeJedisPool(new Config(), cluster);
		JedisFacade jedis = pool.getResource();

		jedis.set("foo", "bar");
		assertEquals("bar", jedis.get("foo"));
		pool.returnResource(jedis);
		pool.destroy();
	}

	@Test
	public void connectionIsReusedWhenReturned() {
		Config config = new Config();
		config.maxActive = 1;
		FailSafeJedisPool pool = new FailSafeJedisPool(config, cluster);
		JedisFacade jedisA, jedisB;
		
		jedisA = pool.getResource();
		jedisA.set("foo", "0");
		pool.returnResource(jedisA);

		jedisB = pool.getResource();
		jedisB.incr("foo");
		pool.returnResource(jedisB);
		
		assertTrue(jedisA == jedisB);
		pool.destroy();
	}
	
	@Test
	public void shouldWorkIfOneServerFails() {
		Config config = new Config();
		FailSafeJedisPool pool = new FailSafeJedisPool(config, cluster);		
		JedisFacade jedis;
		
		jedis0.slaveof(hnp1.host, hnp1.port);
		jedis = pool.getResource();
		assertEquals("OK", jedis.set("foo", "0"));
		pool.returnResource(jedis);
		
		jedis1.slaveof(hnp0.host, hnp0.port);
		jedis0.slaveofNoOne();
		
		jedis = pool.getResource();
		assertEquals("OK", jedis.set("foo", "1"));
		pool.returnResource(jedis);

		pool.destroy();	
	}

}
