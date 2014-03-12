package redis.clients.jedis.tests;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisPoolTest extends Assert {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    @Test
    public void checkConnections() {
	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000);
	Jedis jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.set("foo", "bar");
	assertEquals("bar", jedis.get("foo"));
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkConnectionWithDefaultPort() {
	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort());
	Jedis jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.set("foo", "bar");
	assertEquals("bar", jedis.get("foo"));
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkJedisIsReusedWhenReturned() {

	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort());
	Jedis jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.set("foo", "0");
	pool.returnResource(jedis);

	jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.incr("foo");
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void checkPoolRepairedWhenJedisIsBroken() {
	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort());
	Jedis jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.quit();
	pool.returnBrokenResource(jedis);

	jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.incr("foo");
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test(expected = JedisConnectionException.class)
    public void checkPoolOverflow() {
	GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	config.setMaxTotal(1);
	config.setBlockWhenExhausted(false);
	JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort());
	Jedis jedis = pool.getResource();
	jedis.auth("foobared");
	jedis.set("foo", "0");

	Jedis newJedis = pool.getResource();
	newJedis.auth("foobared");
	newJedis.incr("foo");
    }

    @Test
    public void securePool() {
	JedisPoolConfig config = new JedisPoolConfig();
	config.setTestOnBorrow(true);
	JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(),
		2000, "foobared");
	Jedis jedis = pool.getResource();
	jedis.set("foo", "bar");
	pool.returnResource(jedis);
	pool.destroy();
    }

    @Test
    public void nonDefaultDatabase() {
	JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000, "foobared");
	Jedis jedis0 = pool0.getResource();
	jedis0.set("foo", "bar");
	assertEquals("bar", jedis0.get("foo"));
	pool0.returnResource(jedis0);
	pool0.destroy();

	JedisPool pool1 = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000, "foobared", 1);
	Jedis jedis1 = pool1.getResource();
	assertNull(jedis1.get("foo"));
	pool1.returnResource(jedis1);
	pool1.destroy();
    }

    @Test
    public void startWithUrlString() {
	Jedis j = new Jedis("localhost", 6380);
	j.auth("foobared");
	j.select(2);
	j.set("foo", "bar");
	JedisPool pool = new JedisPool("redis://:foobared@localhost:6380/2");
	Jedis jedis = pool.getResource();
	assertEquals("PONG", jedis.ping());
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void startWithUrl() throws URISyntaxException {
	Jedis j = new Jedis("localhost", 6380);
	j.auth("foobared");
	j.select(2);
	j.set("foo", "bar");
	JedisPool pool = new JedisPool(new URI(
		"redis://:foobared@localhost:6380/2"));
	Jedis jedis = pool.getResource();
	assertEquals("PONG", jedis.ping());
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void selectDatabaseOnActivation() {
	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000, "foobared");

	Jedis jedis0 = pool.getResource();
	assertEquals(0L, jedis0.getDB().longValue());

	jedis0.select(1);
	assertEquals(1L, jedis0.getDB().longValue());

	pool.returnResource(jedis0);

	Jedis jedis1 = pool.getResource();
	assertTrue("Jedis instance was not reused", jedis1 == jedis0);
	assertEquals(0L, jedis1.getDB().longValue());

	pool.returnResource(jedis1);
	pool.destroy();
    }

    @Test
    public void customClientName() {
	JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000, "foobared", 0, "my_shiny_client_name");

	Jedis jedis = pool0.getResource();

	assertEquals("my_shiny_client_name", jedis.clientGetname());

	pool0.returnResource(jedis);
	pool0.destroy();
    }

    @Test
    public void returnResourceShouldResetState() {
	GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	config.setMaxTotal(1);
	config.setBlockWhenExhausted(false);
	JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(),
		2000, "foobared");

	Jedis jedis = pool.getResource();
	jedis.set("hello", "jedis");
	Transaction t = jedis.multi();
	t.set("hello", "world");
	pool.returnResource(jedis);

	Jedis jedis2 = pool.getResource();
	assertTrue(jedis == jedis2);
	assertEquals("jedis", jedis2.get("hello"));
	pool.returnResource(jedis2);
	pool.destroy();
    }
    
    @Test
    public void returnNullObjectShouldNotFail() {
	JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(),
		hnp.getPort(), 2000, "foobared", 0, "my_shiny_client_name");

	pool.returnBrokenResource(null);
	pool.returnResource(null);
	pool.returnResourceObject(null);
    }
}
