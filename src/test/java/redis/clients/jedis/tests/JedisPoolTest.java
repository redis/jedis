package redis.clients.jedis.tests;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class JedisPoolTest extends Assert {
	private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0); 
	
	@Test
	public void checkConnections() throws TimeoutException {
		JedisPool pool = new JedisPool(hnp.host, hnp.port, 2000);
		pool.setResourcesNumber(10);
		pool.init();

		Jedis jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.set("foo", "bar");
		assertEquals("bar", jedis.get("foo"));
		pool.returnResource(jedis);
		pool.destroy();
	}

	@Test
	public void checkConnectionWithDefaultPort() throws TimeoutException {
		JedisPool pool = new JedisPool(hnp.host, hnp.port);
		pool.setResourcesNumber(10);
		pool.init();

		Jedis jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.set("foo", "bar");
		assertEquals("bar", jedis.get("foo"));
		pool.returnResource(jedis);
		pool.destroy();
	}

	@Test
	public void checkJedisIsReusedWhenReturned() throws TimeoutException {
		JedisPool pool = new JedisPool(hnp.host, hnp.port);
		pool.setResourcesNumber(1);
		pool.init();

		Jedis jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.set("foo", "0");
		pool.returnResource(jedis);

		jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.incr("foo");
		pool.returnResource(jedis);
		pool.destroy();
	}

	@Test
	public void checkPoolRepairedWhenJedisIsBroken() throws TimeoutException,
			IOException {
		JedisPool pool = new JedisPool(hnp.host, hnp.port);
		pool.setResourcesNumber(1);
		pool.init();

		Jedis jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.quit();
		pool.returnBrokenResource(jedis);

		jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.incr("foo");
		pool.returnResource(jedis);
		pool.destroy();
	}

	@Test(expected = TimeoutException.class)
	public void checkPoolOverflow() throws TimeoutException {
		JedisPool pool = new JedisPool(hnp.host, hnp.port);
		pool.setResourcesNumber(1);
		pool.init();

		Jedis jedis = pool.getResource(200);
		jedis.auth("foobared");
		jedis.set("foo", "0");

		Jedis newJedis = pool.getResource(200);
		newJedis.auth("foobared");
		newJedis.incr("foo");
	}
}