package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.JedisException;

public class StringValuesCommandsTest extends JedisCommandTestBase {
    @Test
    public void setAndGet() throws JedisException {
	String status = jedis.set("foo", "bar");
	assertEquals("OK", status);

	String value = jedis.get("foo");
	assertEquals("bar", value);

	assertEquals(null, jedis.get("bar"));
    }

    @Test
    public void getSet() throws JedisException {
	String value = jedis.getSet("foo", "bar");
	assertEquals(null, value);
	value = jedis.get("foo");
	assertEquals("bar", value);
    }

    @Test
    public void mget() throws JedisException {
	List<String> values = jedis.mget("foo", "bar");
	List<String> expected = new ArrayList<String>();
	expected.add(null);
	expected.add(null);

	assertEquals(expected, values);

	jedis.set("foo", "bar");

	expected = new ArrayList<String>();
	expected.add("bar");
	expected.add(null);
	values = jedis.mget("foo", "bar");

	assertEquals(expected, values);

	jedis.set("bar", "foo");

	expected = new ArrayList<String>();
	expected.add("bar");
	expected.add("foo");
	values = jedis.mget("foo", "bar");

	assertEquals(expected, values);
    }

    @Test
    public void setnx() throws JedisException {
	int status = jedis.setnx("foo", "bar");
	assertEquals(1, status);
	assertEquals("bar", jedis.get("foo"));

	status = jedis.setnx("foo", "bar2");
	assertEquals(0, status);
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void setex() throws JedisException {
	String status = jedis.setex("foo", 20, "bar");
	assertEquals("OK", status);
	int ttl = jedis.ttl("foo");
	assertTrue(ttl > 0 && ttl <= 20);
    }

    @Test
    public void mset() throws JedisException {
	String status = jedis.mset("foo", "bar", "bar", "foo");
	assertEquals("OK", status);
	assertEquals("bar", jedis.get("foo"));
	assertEquals("foo", jedis.get("bar"));
    }

    @Test
    public void msetnx() throws JedisException {
	int status = jedis.msetnx("foo", "bar", "bar", "foo");
	assertEquals(1, status);
	assertEquals("bar", jedis.get("foo"));
	assertEquals("foo", jedis.get("bar"));

	status = jedis.msetnx("foo", "bar1", "bar2", "foo2");
	assertEquals(0, status);
	assertEquals("bar", jedis.get("foo"));
	assertEquals("foo", jedis.get("bar"));
    }

    @Test(expected = JedisException.class)
    public void incrWrongValue() throws JedisException {
	jedis.set("foo", "bar");
	jedis.incr("foo");
    }

    @Test
    public void incr() throws JedisException {
	int value = jedis.incr("foo");
	assertEquals(1, value);
	value = jedis.incr("foo");
	assertEquals(2, value);
    }

    @Test(expected = JedisException.class)
    public void incrByWrongValue() throws JedisException {
	jedis.set("foo", "bar");
	jedis.incrBy("foo", 2);
    }

    @Test
    public void incrBy() throws JedisException {
	int value = jedis.incrBy("foo", 2);
	assertEquals(2, value);
	value = jedis.incrBy("foo", 2);
	assertEquals(4, value);
    }

    @Test(expected = JedisException.class)
    public void decrWrongValue() throws JedisException {
	jedis.set("foo", "bar");
	jedis.decr("foo");
    }

    @Test
    public void decr() throws JedisException {
	int value = jedis.decr("foo");
	assertEquals(-1, value);
	value = jedis.decr("foo");
	assertEquals(-2, value);
    }

    @Test(expected = JedisException.class)
    public void decrByWrongValue() throws JedisException {
	jedis.set("foo", "bar");
	jedis.decrBy("foo", 2);
    }

    @Test
    public void decrBy() throws JedisException {
	int value = jedis.decrBy("foo", 2);
	assertEquals(-2, value);
	value = jedis.decrBy("foo", 2);
	assertEquals(-4, value);
    }

    @Test
    public void append() throws JedisException {
	int value = jedis.append("foo", "bar");
	assertEquals(3, value);
	assertEquals("bar", jedis.get("foo"));
	value = jedis.append("foo", "bar");
	assertEquals(6, value);
	assertEquals("barbar", jedis.get("foo"));
    }

    @Test
    public void substr() throws JedisException {
	jedis.set("s", "This is a string");
	assertEquals("This", jedis.substr("s", 0, 3));
	assertEquals("ing", jedis.substr("s", -3, -1));
	assertEquals("This is a string", jedis.substr("s", 0, -1));
	assertEquals(" string", jedis.substr("s", 9, 100000));
    }
}