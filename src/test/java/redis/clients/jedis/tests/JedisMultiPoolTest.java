package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMultiPool;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.Pool;

public class JedisMultiPoolTest extends Assert {
    private static HostAndPort redis1 = HostAndPortUtil.getRedisServers()
            .get(0);
    private static HostAndPort redis2 = HostAndPortUtil.getRedisServers()
            .get(1);

    private List<JedisShardInfo> shards;

    @Before
    public void startUp() {
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
    public void checkConnections() {
    	JedisMultiPool masterPool = new JedisMultiPool(new Config(), shards);
        Pool<Jedis> jedisPool = masterPool.getPool("foo");
        Jedis j = jedisPool.getResource();
        j.set("foo", "bar");
        jedisPool.returnResource(j);
        
        j = jedisPool.getResource();
        assertEquals("bar", j.get("foo"));
        jedisPool.returnResource(j);
        
        jedisPool.returnResource(j);
        masterPool.destroy();
    }

    @Test
    public void checkJedisIsReusedWhenReturned() {
    	JedisMultiPool masterPool = new JedisMultiPool(new Config(), shards);
        Pool<Jedis> jedisPool = masterPool.getPool("foo");
        Jedis j = jedisPool.getResource();
        
        j.set("foo", "0");
        jedisPool.returnResource(j);

        j = jedisPool.getResource();
        j.incr("foo");
        jedisPool.returnResource(j);
        
        masterPool.destroy();
    }

    @Test
    public void checkPoolRepairedWhenJedisIsBroken() {
    	JedisMultiPool masterPool = new JedisMultiPool(new Config(), shards);
        Pool<Jedis> jedisPool = masterPool.getPool("foo");
        Jedis j = jedisPool.getResource();
        
        j.disconnect();
        jedisPool.returnBrokenResource(j);

        j = jedisPool.getResource();
        j.incr("foo");
        jedisPool.returnResource(j);
        masterPool.destroy();
    }

}