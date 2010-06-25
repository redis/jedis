package redis.clients.jedis.tests.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class SetCommandsTest extends Assert {
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
    public void sadd() throws JedisException {
	int status = jedis.sadd("foo", "a");
	assertEquals(1, status);

	status = jedis.sadd("foo", "a");
	assertEquals(0, status);
    }

    @Test
    public void smembers() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");

	Set<String> members = jedis.smembers("foo");

	assertEquals(expected, members);
    }

}
