package redis.clients.jedis.tests;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SharedJedisPipelineTest {
    private static HostAndPortUtil.HostAndPort redis1 = HostAndPortUtil.getRedisServers()
            .get(0);
    private static HostAndPortUtil.HostAndPort redis2 = HostAndPortUtil.getRedisServers()
            .get(1);

    private ShardedJedis jedis;

    @Before
    public void setUp() throws Exception {
        Jedis jedis = new Jedis(redis1.host, redis1.port);
        jedis.flushAll();
        jedis.disconnect();
        jedis = new Jedis(redis2.host, redis2.port);
        jedis.flushAll();
        jedis.disconnect();

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port));
        shards.add(new JedisShardInfo(redis2.host, redis2.port));
        this.jedis = new ShardedJedis(shards);
    }

    @Test
    public void pipeline() throws UnsupportedEncodingException {
        ShardedJedisPipeline p = jedis.pipelined();
        p.set("foo", "bar");
        p.get("foo");
        List<Object> results = p.syncAndReturnAll();

        assertEquals(2, results.size());
        assertEquals("OK", results.get(0));
        assertEquals("bar", results.get(1));

    }

    @Test
    public void pipelineResponse() {
        jedis.set("string", "foo");
        jedis.lpush("list", "foo");
        jedis.hset("hash", "foo", "bar");
        jedis.zadd("zset", 1, "foo");
        jedis.sadd("set", "foo");

        ShardedJedisPipeline p = jedis.pipelined();
        Response<String> string = p.get("string");
        Response<String> list = p.lpop("list");
        Response<String> hash = p.hget("hash", "foo");
        Response<Set<String>> zset = p.zrange("zset", 0, -1);
        Response<String> set = p.spop("set");
        Response<Boolean> blist = p.exists("list");
        Response<Double> zincrby = p.zincrby("zset", 1, "foo");
        Response<Long> zcard = p.zcard("zset");
        p.lpush("list", "bar");
        Response<List<String>> lrange = p.lrange("list", 0, -1);
        Response<Map<String, String>> hgetAll = p.hgetAll("hash");
        p.sadd("set", "foo");
        Response<Set<String>> smembers = p.smembers("set");
        Response<Set<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0,
                -1);
        p.sync();

        assertEquals("foo", string.get());
        assertEquals("foo", list.get());
        assertEquals("bar", hash.get());
        assertEquals("foo", zset.get().iterator().next());
        assertEquals("foo", set.get());
        assertFalse(blist.get());
        assertEquals(new Double(2), zincrby.get());
        assertEquals(new Long(1), zcard.get());
        assertEquals(1, lrange.get().size());
        assertNotNull(hgetAll.get().get("foo"));
        assertEquals(1, smembers.get().size());
        assertEquals(1, zrangeWithScores.get().size());
    }

    @Test(expected = JedisDataException.class)
    public void pipelineResponseWithinPipeline() {
        jedis.set("string", "foo");

        ShardedJedisPipeline p = jedis.pipelined();
        Response<String> string = p.get("string");
        string.get();
        p.sync();
    }

    @Test
    public void canRetrieveUnsetKey() {
        ShardedJedisPipeline p = jedis.pipelined();
        Response<String> shouldNotExist = p.get(UUID.randomUUID().toString());
        p.sync();
        assertNull(shouldNotExist.get());
    }
}
