package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.PipelineBlock;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;
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
    public void pipeline() throws UnknownHostException, IOException {
        List<Object> results = jedis.pipelined(new PipelineBlock() {
            public void execute() {
                set("foo", "bar");
                get("foo");
            }
        });

        assertEquals(2, results.size());
        assertArrayEquals("OK".getBytes(Protocol.CHARSET), (byte[]) results
                .get(0));
        assertArrayEquals("bar".getBytes(Protocol.CHARSET), (byte[]) results
                .get(1));

        Pipeline p = jedis.pipelined();
        p.set("foo", "bar");
        p.get("foo");
        results = p.execute();

        assertEquals(2, results.size());
        assertArrayEquals("OK".getBytes(Protocol.CHARSET), (byte[]) results
                .get(0));
        assertArrayEquals("bar".getBytes(Protocol.CHARSET), (byte[]) results
                .get(1));

    }
}
