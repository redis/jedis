package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class ListCommandsTest extends JedisCommandTestBase {
    @Test
    public void rpush() {
	int size = jedis.rpush("foo", "bar");
	assertEquals(1, size);
	size = jedis.rpush("foo", "foo");
	assertEquals(2, size);
    }

    @Test
    public void lpush() {
	int size = jedis.lpush("foo", "bar");
	assertEquals(1, size);
	size = jedis.lpush("foo", "foo");
	assertEquals(2, size);
    }

    @Test
    public void llen() {
	assertEquals(0, jedis.llen("foo").intValue());
	jedis.lpush("foo", "bar");
	jedis.lpush("foo", "car");
	assertEquals(2, jedis.llen("foo").intValue());
    }

    @Test(expected = JedisException.class)
    public void llenNotOnList() {
	jedis.set("foo", "bar");
	jedis.llen("foo");
    }

    @Test
    public void lrange() {
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
    public void ltrim() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");
	String status = jedis.ltrim("foo", 0, 1);

	List<String> expected = new ArrayList<String>();
	expected.add("3");
	expected.add("2");

	assertEquals("OK", status);
	assertEquals(2, jedis.llen("foo").intValue());
	assertEquals(expected, jedis.lrange("foo", 0, 100));
    }

    @Test
    public void lindex() {
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
    public void lset() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	assertEquals("3", jedis.lindex("foo", 0));
	assertEquals(null, jedis.lindex("foo", 100));
    }

    @Test
    public void lrem() {
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
	assertEquals(0, jedis.lrem("bar", 100, "foo").intValue());
    }

    @Test
    public void lpop() {
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
    public void rpop() {
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
    public void rpoplpush() {
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

    @Test
    public void blpop() throws InterruptedException {
	List<String> result = jedis.blpop(1, "foo");
	assertNull(result);

	new Thread(new Runnable() {
	    public void run() {
		try {
		    Jedis j = createJedis();
		    j.lpush("foo", "bar");
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	result = jedis.blpop(1, "foo");
	assertNotNull(result);
	assertEquals(2, result.size());
	assertEquals("foo", result.get(0));
	assertEquals("bar", result.get(1));
    }

    @Test
    public void brpop() throws InterruptedException {
	List<String> result = jedis.brpop(1, "foo");
	assertNull(result);

	new Thread(new Runnable() {
	    public void run() {
		try {
		    Jedis j = createJedis();
		    j.lpush("foo", "bar");
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	result = jedis.brpop(1, "foo");
	assertNotNull(result);
	assertEquals(2, result.size());
	assertEquals("foo", result.get(0));
	assertEquals("bar", result.get(1));
    }

    @Test
    public void lpushx() {
	int status = jedis.lpushx("foo", "bar");
	assertEquals(0, status);

	jedis.lpush("foo", "a");
	status = jedis.lpushx("foo", "b");
	assertEquals(2, status);
    }

    @Test
    public void rpushx() {
	int status = jedis.rpushx("foo", "bar");
	assertEquals(0, status);

	jedis.lpush("foo", "a");
	status = jedis.rpushx("foo", "b");
	assertEquals(2, status);
    }
}