package redis.clients.jedis.tests;

import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class JedisPoolTest extends Assert {
    @Test
    public void checkConnections() throws TimeoutException {
	JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT, 2000);
	pool.setResourcesNumber(10);
	pool.init();

	Jedis jedis = pool.getResource(200);
	jedis.auth("foobared");
	jedis.set("foo", "bar");
	assertEquals("bar", jedis.get("foo"));
	pool.returnResource(jedis);
    }
}