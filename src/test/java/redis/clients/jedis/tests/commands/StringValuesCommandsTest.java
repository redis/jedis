package redis.clients.jedis.tests.commands;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class StringValuesCommandsTest extends Assert {
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost");
	jedis.connect();
    }

    @After
    public void tearDown() throws Exception {
	jedis.flushDB();
	jedis.disconnect();
    }

    @Test
    public void setAndGet() throws JedisException {
	String status = jedis.set("foo", "bar");
	assertEquals("OK", status);

	String value = jedis.get("foo");
	assertEquals("bar", value);
    }

}
