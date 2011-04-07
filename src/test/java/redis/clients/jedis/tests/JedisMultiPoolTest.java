package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMultiPool;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;

public class JedisMultiPoolTest extends Assert {
    private static HostAndPort redis1 = HostAndPortUtil.getRedisServers().get(0);
    private static HostAndPort redis2 = HostAndPortUtil.getRedisServers().get(1);

    private List<JedisShardInfo> shards;

    @Before
    public void startUp() {
        shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port, "foobared"));
        shards.add(new JedisShardInfo(redis2.host, redis2.port, "foobared"));
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
    	Config config = new Config();
        config.maxActive = 1;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
    	JedisMultiPool masterPool = new JedisMultiPool(config, shards);
        Jedis j = masterPool.getResource("foo");
        
        j.set("foo", "0");
        masterPool.returnResource(j);

        j = masterPool.getResource("foo");
        j.incr("foo");
        masterPool.returnResource(j);
        
        masterPool.destroy();
    }

    @Test
    public void checkPoolRepairedWhenJedisIsBroken() {
    	Config config = new Config();
        config.maxActive = 1;
    	JedisMultiPool masterPool = new JedisMultiPool(config, shards);
        Pool<Jedis> jedisPool = masterPool.getPool("foo");
        Jedis j = jedisPool.getResource();
        
        j.disconnect();
        jedisPool.returnBrokenResource(j);

        j = jedisPool.getResource();
        j.incr("foo");
        jedisPool.returnResource(j);
        masterPool.destroy();
    }
    
    @Test
    public void shouldNotShareInstances() {
        Config config = new Config();
        config.maxActive = 4;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;

        JedisMultiPool pool = new JedisMultiPool(config, shards);

        Jedis j1 = pool.getResource("foo");
        Jedis j2 = pool.getResource("foo");

        assertNotSame(j1, j2);
        
        Pool<Jedis> jp1 = pool.getPool("foo");
        Pool<Jedis> jp2 = pool.getPool("foo");

        assertSame(jp1, jp2);
        
        assertNotSame(jp1.getResource(), jp2.getResource());
        pool.destroy();
    }
    
    @Test
    public void shouldBasicOps() {
        JedisMultiPool pool = new JedisMultiPool(new Config(), shards);
        
        Iterator<JedisPool> iterPool = pool.getAllPools().iterator();
        
        for(JedisShardInfo shardInfo : pool.getShardsInfo()){
        	assertSame(pool.getPool(shardInfo), iterPool.next());
        }

        assertSame(pool.getPool("foo"), pool.getPool(SafeEncoder.encode("foo")));
    	
    }
}