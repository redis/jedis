package redis.clients.jedis.tests;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class JedisPoolTest extends Assert {
    @Test
    public void checkConnections() throws TimeoutException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT,
		2000, "foobared");
	pool.setResourcesNumber(10);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.set("foo", "bar");
	assertEquals("bar", jedis.get("foo"));
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkConnectionWithDefaultPort() throws TimeoutException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT,
		2000, "foobared");
	pool.setResourcesNumber(10);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.set("foo", "bar");
	assertEquals("bar", jedis.get("foo"));
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkJedisIsReusedWhenReturned() throws TimeoutException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT,
		2000, "foobared");
	pool.setResourcesNumber(1);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.set("foo", "0");
	pool.returnResource(jedis);

	jedis = pool.getResource(200);
	jedis.incr("foo");
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkPoolRepairedWhenJedisIsBroken() throws TimeoutException,
	    IOException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT,
		2000, "foobared");
	pool.setResourcesNumber(1);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.quit();
	pool.returnBrokenResource(jedis);

	jedis = pool.getResource(200);
	jedis.incr("foo");
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test(expected = TimeoutException.class)
    public void checkPoolOverflow() throws TimeoutException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT,
		2000, "foobared");
	pool.setResourcesNumber(1);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.set("foo", "0");

	Jedis newJedis = pool.getResource(200);
	newJedis.incr("foo");
    }
}