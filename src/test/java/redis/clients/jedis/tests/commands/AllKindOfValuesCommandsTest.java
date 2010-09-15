package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.JedisException;

public class AllKindOfValuesCommandsTest extends JedisCommandTestBase {
    @Test
    public void ping() {
	String status = jedis.ping();
	assertEquals("PONG", status);
    }

    @Test
    public void exists() {
	String status = jedis.set("foo", "bar");
	assertEquals("OK", status);

	int reply = jedis.exists("foo");
	assertEquals(1, reply);

	reply = jedis.del("foo");
	assertEquals(1, reply);

	reply = jedis.exists("foo");
	assertEquals(0, reply);
    }

    @Test
    public void del() {
	jedis.set("foo1", "bar1");
	jedis.set("foo2", "bar2");
	jedis.set("foo3", "bar3");

	int reply = jedis.del("foo1", "foo2", "foo3");
	assertEquals(3, reply);

	reply = jedis.exists("foo1");
	assertEquals(0, reply);
	reply = jedis.exists("foo2");
	assertEquals(0, reply);
	reply = jedis.exists("foo3");
	assertEquals(0, reply);

	jedis.set("foo1", "bar1");

	reply = jedis.del("foo1", "foo2");
	assertEquals(1, reply);

	reply = jedis.del("foo1", "foo2");
	assertEquals(0, reply);
    }

    @Test
    public void type() {
	jedis.set("foo", "bar");
	String status = jedis.type("foo");
	assertEquals("string", status);
    }

    @Test
    public void keys() {
	jedis.set("foo", "bar");
	jedis.set("foobar", "bar");

	List<String> keys = jedis.keys("foo*");
	List<String> expected = new ArrayList<String>();
	expected.add("foo");
	expected.add("foobar");
	assertEquals(expected, keys);

	expected = new ArrayList<String>();
	keys = jedis.keys("bar*");

	assertEquals(expected, keys);
    }

    @Test
    public void randomKey() {
	assertEquals(null, jedis.randomKey());

	jedis.set("foo", "bar");

	assertEquals("foo", jedis.randomKey());

	jedis.set("bar", "foo");

	String randomkey = jedis.randomKey();
	assertTrue(randomkey.equals("foo") || randomkey.equals("bar"));
    }

    @Test
    public void rename() {
	jedis.set("foo", "bar");
	String status = jedis.rename("foo", "bar");
	assertEquals("OK", status);

	String value = jedis.get("foo");
	assertEquals(null, value);

	value = jedis.get("bar");
	assertEquals("bar", value);
    }

    @Test(expected = JedisException.class)
    public void renameOldAndNewAreTheSame() {
	jedis.set("foo", "bar");
	jedis.rename("foo", "foo");
    }

    @Test
    public void renamenx() {
	jedis.set("foo", "bar");
	int status = jedis.renamenx("foo", "bar");
	assertEquals(1, status);

	jedis.set("foo", "bar");
	status = jedis.renamenx("foo", "bar");
	assertEquals(0, status);
    }

    @Test
    public void dbSize() {
	int size = jedis.dbSize();
	assertEquals(0, size);

	jedis.set("foo", "bar");
	size = jedis.dbSize();
	assertEquals(1, size);
    }

    @Test
    public void expire() {
	int status = jedis.expire("foo", 20);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.expire("foo", 20);
	assertEquals(1, status);
    }

    @Test
    public void expireAt() {
	long unixTime = (System.currentTimeMillis() / 1000L) + 20;

	int status = jedis.expireAt("foo", unixTime);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	unixTime = (System.currentTimeMillis() / 1000L) + 20;
	status = jedis.expireAt("foo", unixTime);
	assertEquals(1, status);
    }

    @Test
    public void ttl() {
	int ttl = jedis.ttl("foo");
	assertEquals(-1, ttl);

	jedis.set("foo", "bar");
	ttl = jedis.ttl("foo");
	assertEquals(-1, ttl);

	jedis.expire("foo", 20);
	ttl = jedis.ttl("foo");
	assertTrue(ttl >= 0 && ttl <= 20);
    }

    @Test
    public void select() {
	jedis.set("foo", "bar");
	String status = jedis.select(1);
	assertEquals("OK", status);
	assertEquals(null, jedis.get("foo"));
	status = jedis.select(0);
	assertEquals("OK", status);
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void move() {
	int status = jedis.move("foo", 1);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.move("foo", 1);
	assertEquals(1, status);
	assertEquals(null, jedis.get("foo"));

	jedis.select(1);
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void flushDB() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	String status = jedis.flushDB();
	assertEquals("OK", status);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(1, jedis.dbSize().intValue());
    }

    @Test
    public void flushAll() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	String status = jedis.flushAll();
	assertEquals("OK", status);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(0, jedis.dbSize().intValue());
    }

    @Test
    public void persist() {
	jedis.setex("foo", 60 * 60, "bar");
	assertTrue(jedis.ttl("foo") > 0);
	int status = jedis.persist("foo");
	assertEquals(1, status);
	assertEquals(-1, jedis.ttl("foo").intValue());
    }

    @Test
    public void echo() {
	String result = jedis.echo("hello world");
	assertEquals("hello world", result);
    }

}