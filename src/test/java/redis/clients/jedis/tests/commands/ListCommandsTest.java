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

    @Test
    public void ltrim() throws JedisException {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");
	String status = jedis.ltrim("foo", 0, 1);

	List<String> expected = new ArrayList<String>();
	expected.add("3");
	expected.add("2");

	assertEquals("OK", status);
	assertEquals(2, jedis.llen("foo"));
	assertEquals(expected, jedis.lrange("foo", 0, 100));
    }

    @Test
    public void lindex() throws JedisException {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	List<String> expected = new ArrayList<String>();
	expected.add("3");
	expected.add("bar");
	expected.add("1");

	String status = jedis.lset("foo", 1, "bar");

	assertEquals("OK", status);
	assertEquals(expected, jedis.lrange("foo", 0, 100));
    }

    @Test
    public void lset() throws JedisException {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	assertEquals("3", jedis.lindex("foo", 0));
	assertEquals(null, jedis.lindex("foo", 100));
    }

    @Test
    public void lrem() throws JedisException {
	jedis.lpush("foo", "hello");
	jedis.lpush("foo", "hello");
	jedis.lpush("foo", "x");
	jedis.lpush("foo", "hello");
	jedis.lpush("foo", "c");
	jedis.lpush("foo", "b");
	jedis.lpush("foo", "a");

	int count = jedis.lrem("foo", -2, "hello");

	List<String> expected = new ArrayList<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");
	expected.add("hello");
	expected.add("x");

	assertEquals(2, count);
	assertEquals(expected, jedis.lrange("foo", 0, 1000));
	assertEquals(0, jedis.lrem("bar", 100, "foo"));
    }

    @Test
    public void lpop() throws JedisException {
	jedis.rpush("foo", "a");
	jedis.rpush("foo", "b");
	jedis.rpush("foo", "c");

	String element = jedis.lpop("foo");
	assertEquals("a", element);

	List<String> expected = new ArrayList<String>();
	expected.add("b");
	expected.add("c");

	assertEquals(expected, jedis.lrange("foo", 0, 1000));
	jedis.lpop("foo");
	jedis.lpop("foo");

	element = jedis.lpop("foo");
	assertEquals(null, element);
    }

    @Test
    public void rpop() throws JedisException {
	jedis.rpush("foo", "a");
	jedis.rpush("foo", "b");
	jedis.rpush("foo", "c");

	String element = jedis.rpop("foo");
	assertEquals("c", element);

	List<String> expected = new ArrayList<String>();
	expected.add("a");
	expected.add("b");

	assertEquals(expected, jedis.lrange("foo", 0, 1000));
	jedis.rpop("foo");
	jedis.rpop("foo");

	element = jedis.rpop("foo");
	assertEquals(null, element);
    }

    @Test
    public void rpoplpush() throws JedisException {
	jedis.rpush("foo", "a");
	jedis.rpush("foo", "b");
	jedis.rpush("foo", "c");

	jedis.rpush("dst", "foo");
	jedis.rpush("dst", "bar");

	String element = jedis.rpoplpush("foo", "dst");

	assertEquals("c", element);

	List<String> srcExpected = new ArrayList<String>();
	srcExpected.add("a");
	srcExpected.add("b");

	List<String> dstExpected = new ArrayList<String>();
	dstExpected.add("c");
	dstExpected.add("foo");
	dstExpected.add("bar");

	assertEquals(srcExpected, jedis.lrange("foo", 0, 1000));
	assertEquals(dstExpected, jedis.lrange("dst", 0, 1000));
    }
}
