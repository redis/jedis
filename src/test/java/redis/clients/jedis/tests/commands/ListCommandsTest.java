package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class ListCommandsTest extends Assert {
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
    public void rpush() throws JedisException {
	int size = jedis.rpush("foo", "bar");
	assertEquals(1, size);
	size = jedis.rpush("foo", "foo");
	assertEquals(2, size);
    }

    @Test
    public void lpush() throws JedisException {
	int size = jedis.lpush("foo", "bar");
	assertEquals(1, size);
	size = jedis.lpush("foo", "foo");
	assertEquals(2, size);
    }

    @Test
    public void llen() throws JedisException {
	assertEquals(0, jedis.llen("foo"));
	jedis.lpush("foo", "bar");
	jedis.lpush("foo", "car");
	assertEquals(2, jedis.llen("foo"));
    }

    @Test(expected = JedisException.class)
    public void llenNotOnList() throws JedisException {
	jedis.set("foo", "bar");
	jedis.llen("foo");
    }

    @Test
    public void lrange() throws JedisException {
	jedis.rpush("foo", "a");
	jedis.rpush("foo", "b");
	jedis.rpush("foo", "c");

	List<String> expected = new ArrayList<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");

	List<String> range = jedis.lrange("foo", 0, 2);
	assertEquals(expected, range);

	range = jedis.lrange("foo", 0, 20);
	assertEquals(expected, range);

	expected = new ArrayList<String>();
	expected.add("b");
	expected.add("c");

	range = jedis.lrange("foo", 1, 2);
	assertEquals(expected, range);

	expected = new ArrayList<String>();
	range = jedis.lrange("foo", 2, 1);
	assertEquals(expected, range);
    }

}
