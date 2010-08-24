package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.Protocol;

public class HashesCommandsTest extends JedisCommandTestBase {
    @Test
    public void hset() {
	int status = jedis.hset("foo", "bar", "car");
	assertEquals(1, status);
	status = jedis.hset("foo", "bar", "foo");
	assertEquals(0, status);
    }

    @Test
    public void hget() {
	jedis.hset("foo", "bar", "car");
	assertEquals(null, jedis.hget("bar", "foo"));
	assertEquals(null, jedis.hget("foo", "car"));
	assertEquals("car", jedis.hget("foo", "bar"));
    }

    @Test
    public void hsetnx() {
	int status = jedis.hsetnx("foo", "bar", "car");
	assertEquals(1, status);
	assertEquals("car", jedis.hget("foo", "bar"));

	status = jedis.hsetnx("foo", "bar", "foo");
	assertEquals(0, status);
	assertEquals("car", jedis.hget("foo", "bar"));

	status = jedis.hsetnx("foo", "car", "bar");
	assertEquals(1, status);
	assertEquals("bar", jedis.hget("foo", "car"));
    }

    @Test
    public void hmset() {
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	String status = jedis.hmset("foo", hash);
	assertEquals("OK", status);
	assertEquals("car", jedis.hget("foo", "bar"));
	assertEquals("bar", jedis.hget("foo", "car"));
    }

    @Test
    public void hmget() {
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	List<String> values = jedis.hmget("foo", "bar", "car", "foo");
	List<String> expected = new ArrayList<String>();
	expected.add("car");
	expected.add("bar");
	expected.add(null);

	assertEquals(expected, values);
    }

    @Test
    public void hincrBy() {
	int value = jedis.hincrBy("foo", "bar", 1);
	assertEquals(1, value);
	value = jedis.hincrBy("foo", "bar", -1);
	assertEquals(0, value);
	value = jedis.hincrBy("foo", "bar", -10);
	assertEquals(-10, value);
    }

    @Test
    public void hexists() {
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	assertEquals(0, jedis.hexists("bar", "foo"));
	assertEquals(0, jedis.hexists("foo", "foo"));
	assertEquals(1, jedis.hexists("foo", "bar"));
    }

    @Test
    public void hdel() {
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	assertEquals(0, jedis.hdel("bar", "foo"));
	assertEquals(0, jedis.hdel("foo", "foo"));
	assertEquals(1, jedis.hdel("foo", "bar"));
	assertEquals(null, jedis.hget("foo", "bar"));
    }

    @Test
    public void hlen() {
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	assertEquals(0, jedis.hlen("bar"));
	assertEquals(2, jedis.hlen("foo"));
    }

    @Test
    public void hkeys() {
	Map<String, String> hash = new LinkedHashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	List<String> keys = jedis.hkeys("foo");
	List<String> expected = new ArrayList<String>();
	expected.add("bar");
	expected.add("car");
	assertEquals(expected, keys);
    }

    @Test
    public void hvals() {
	Map<String, String> hash = new LinkedHashMap<String, String>();
	hash.put("bar", "car");
	hash.put("car", "bar");
	jedis.hmset("foo", hash);

	List<String> vals = jedis.hvals("foo");
	List<String> expected = new ArrayList<String>();
	expected.add("car");
	expected.add("bar");
	assertEquals(expected, vals);
    }

    @Test
    public void hgetAll() {
	Map<String, String> h = new HashMap<String, String>();
	h.put("bar", "car");
	h.put("car", "bar");
	jedis.hmset("foo", h);

	Map<String, String> hash = jedis.hgetAll("foo");
	Map<String, String> expected = new HashMap<String, String>();
	expected.put("bar", "car");
	expected.put("car", "bar");
	assertEquals(expected, hash);
    }
}
