package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

public class JedisTest extends Assert {
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost");
	jedis.connect();
    }

    @After
    public void tearDown() throws Exception {
	jedis.disconnect();
    }

    @Test
    public void ping() throws UnknownHostException, IOException {
	String status = jedis.ping();
	assertEquals("PONG", status);
    }

    @Test
    public void setAndGet() throws UnknownHostException, IOException {
	String status = jedis.set("foo", "bar");
	assertEquals("OK", status);

	String value = jedis.get("foo");
	assertEquals("bar", value);
    }
}
