package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ResponseListener;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

import java.util.concurrent.CountDownLatch;

public class AsyncPipeliningTest extends Assert {
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
    public void async() throws Exception {
        Pipeline pipeline = jedis.pipelined();

        Response<Long> response = pipeline.incr("number");

        pipeline.flushAsync();

        response.await();
    }

    @Test
    public void asyncThenRegular() throws Exception {
        Pipeline pipeline = jedis.pipelined();

        Response<Long> lastResponse = null;
        for (int i = 0; i < 1000; i++) {
            lastResponse = pipeline.incr("number");
        }

        pipeline.flushAsync();

        assertEquals(jedis.get("number"), "1000");

        lastResponse.await();
    }

    @Test
    public void disconnectWithPendingCalls() throws Exception {
        Pipeline pipeline = jedis.pipelined();

        Response<Long> lastResponse = null;
        for (int i = 0; i < 1000; i++) {
            lastResponse = pipeline.incr("number");
        }

        pipeline.flushAsync();

        jedis.disconnect();

        try {
            lastResponse.await();
            assertFalse("Should have thrown exception", true);
        } catch (JedisDataException da) {
            assertTrue(da.getMessage().contains("Disconnected"));
        }
    }

    @Test
    public void chainedCalls() throws Exception {
        Pipeline pipeline = jedis.pipelined();

        jedis.set("number", "1000");
        Response<Long> response = pipeline.decr("number");
        final CountDownLatch latch = new CountDownLatch(999);

        ResponseListener<Long> listener = new ResponseListener<Long>() {
            @Override
            public void onComplete(Response<Long> response) {
                assertEquals(response.get(), new Long(latch.getCount()));
                latch.countDown();
                if (response.get() > 0) {
                    Pipeline p = jedis.pipelined();
                    p.decr("number").setListener(this);
                    p.flushAsync();
                }
            }
        };

        response.setListener(listener);

        pipeline.flushAsync();

        latch.await();

        assertEquals(jedis.get("number"), "0");
    }
}
