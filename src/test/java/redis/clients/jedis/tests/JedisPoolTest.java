package redis.clients.jedis.tests;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class JedisPoolTest extends Assert {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    @Test
    public void checkConnections() throws Exception {
        JedisPool pool = new JedisPool(new Config(), hnp.host, hnp.port, 2000);
        Jedis jedis = pool.getResource();
        jedis.auth("foobared");
        jedis.set("foo", "bar");
        assertEquals("bar", jedis.get("foo"));
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test
    public void checkConnectionWithDefaultPort() throws Exception {
        JedisPool pool = new JedisPool(new Config(), hnp.host, hnp.port);
        Jedis jedis = pool.getResource();
        jedis.auth("foobared");
        jedis.set("foo", "bar");
        assertEquals("bar", jedis.get("foo"));
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test
    public void checkJedisIsReusedWhenReturned() throws Exception {
        JedisPool pool = new JedisPool(new Config(), hnp.host, hnp.port);
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
    public void checkPoolRepairedWhenJedisIsBroken() throws Exception {
        JedisPool pool = new JedisPool(new Config(), hnp.host, hnp.port);
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

    @Test(expected = JedisException.class)
    public void checkPoolOverflow() {
        Config config = new Config();
        config.maxActive = 1;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
        JedisPool pool = new JedisPool(config, hnp.host, hnp.port);
        Jedis jedis = pool.getResource();
        jedis.auth("foobared");
        jedis.set("foo", "0");

        Jedis newJedis = pool.getResource();
        newJedis.auth("foobared");
        newJedis.incr("foo");
    }
}