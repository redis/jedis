package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class ShardedJedisPoolTest extends Assert {
    private static HostAndPort redis1 = HostAndPortUtil.getRedisServers()
            .get(0);
    private static HostAndPort redis2 = HostAndPortUtil.getRedisServers()
            .get(1);

    private List<JedisShardInfo> shards;

    @Before
    public void startUp() throws UnknownHostException, IOException {
        shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port));
        shards.add(new JedisShardInfo(redis2.host, redis2.port));
        shards.get(0).setPassword("foobared");
        shards.get(1).setPassword("foobared");
        Jedis j = new Jedis(shards.get(0));
        j.connect();
        j.flushAll();
        j.disconnect();
        j = new Jedis(shards.get(1));
        j.connect();
        j.flushAll();
        j.disconnect();
    }

    @Test
    public void checkConnections() throws Exception {
        ShardedJedisPool pool = new ShardedJedisPool(new Config(), shards);
        ShardedJedis jedis = pool.getResource();
        jedis.set("foo", "bar");
        assertEquals("bar", jedis.get("foo"));
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test
    public void checkConnectionWithDefaultPort() throws Exception {
        ShardedJedisPool pool = new ShardedJedisPool(new Config(), shards);
        ShardedJedis jedis = pool.getResource();
        jedis.set("foo", "bar");
        assertEquals("bar", jedis.get("foo"));
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test
    public void checkJedisIsReusedWhenReturned() throws Exception {
        ShardedJedisPool pool = new ShardedJedisPool(new Config(), shards);
        ShardedJedis jedis = pool.getResource();
        jedis.set("foo", "0");
        pool.returnResource(jedis);

        jedis = pool.getResource();
        jedis.incr("foo");
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test
    public void checkPoolRepairedWhenJedisIsBroken() throws Exception {
        ShardedJedisPool pool = new ShardedJedisPool(new Config(), shards);
        ShardedJedis jedis = pool.getResource();
        jedis.disconnect();
        pool.returnBrokenResource(jedis);

        jedis = pool.getResource();
        jedis.incr("foo");
        pool.returnResource(jedis);
        pool.destroy();
    }

    @Test(expected = NoSuchElementException.class)
    public void checkPoolOverflow() throws Exception {
        Config config = new Config();
        config.maxActive = 1;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;

        ShardedJedisPool pool = new ShardedJedisPool(config, shards);

        ShardedJedis jedis = pool.getResource();
        jedis.set("foo", "0");

        ShardedJedis newJedis = pool.getResource();
        newJedis.incr("foo");
    }
}