package redis.clients.jedis.tests;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.PipelineBlock;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class PipeliningTest extends Assert {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
        jedis = new Jedis(hnp.host, hnp.port, 500);
        jedis.connect();
        jedis.auth("foobared");
        jedis.flushAll();
    }

    @Test
    public void pipeline() throws UnsupportedEncodingException {
        List<Object> results = jedis.pipelined(new PipelineBlock() {
            public void execute() {
                set("foo", "bar");
                get("foo");
            }
        });

        assertEquals(2, results.size());
        assertEquals("OK", results.get(0));
        assertEquals("bar", results.get(1));

        Pipeline p = jedis.pipelined();
        p.set("foo", "bar");
        p.get("foo");
        results = p.sync();

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

        Pipeline p = jedis.pipelined();
        Response<String> string = p.get("string");
        Response<String> list = p.lpop("list");
        Response<String> hash = p.hget("hash", "foo");
        Response<Set<String>> zset = p.zrange("zset", 0, -1);
        Response<String> set = p.spop("set");
        p.sync();

        assertEquals("foo", string.get());
        assertEquals("foo", list.get());
        assertEquals("bar", hash.get());
        assertEquals("foo", zset.get().iterator().next());
        assertEquals("foo", set.get());
    }

    @Test(expected = JedisDataException.class)
    public void pipelineResponseWithinPipeline() {
        jedis.set("string", "foo");

        Pipeline p = jedis.pipelined();
        Response<String> string = p.get("string");
        string.get();
        p.sync();
    }
}
