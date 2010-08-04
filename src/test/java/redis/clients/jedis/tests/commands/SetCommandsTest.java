package redis.clients.jedis.tests.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.JedisException;

public class SetCommandsTest extends JedisCommandTestBase {
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

    @Test
    public void srem() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int status = jedis.srem("foo", "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	assertEquals(1, status);
	assertEquals(expected, jedis.smembers("foo"));

	status = jedis.srem("foo", "bar");

	assertEquals(0, status);
    }

    @Test
    public void spop() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	String member = jedis.spop("foo");

	assertTrue("a".equals(member) || "b".equals(member));
	assertEquals(1, jedis.smembers("foo").size());

	member = jedis.spop("bar");
	assertNull(member);
    }

    @Test
    public void smove() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "c");

	int status = jedis.smove("foo", "bar", "a");

	Set<String> expectedSrc = new LinkedHashSet<String>();
	expectedSrc.add("b");

	Set<String> expectedDst = new LinkedHashSet<String>();
	expectedDst.add("c");
	expectedDst.add("a");

	assertEquals(status, 1);
	assertEquals(expectedSrc, jedis.smembers("foo"));
	assertEquals(expectedDst, jedis.smembers("bar"));

	status = jedis.smove("foo", "bar", "a");
	assertEquals(status, 0);
    }

    @Test
    public void scard() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int card = jedis.scard("foo");

	assertEquals(2, card);

	card = jedis.scard("bar");
	assertEquals(0, card);
    }

    @Test
    public void sismember() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int status = jedis.sismember("foo", "a");
	assertEquals(1, status);

	status = jedis.sismember("foo", "c");
	assertEquals(0, status);
    }

    @Test
    public void sinter() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	Set<String> intersection = jedis.sinter("foo", "bar");
	assertEquals(expected, intersection);
    }

    @Test
    public void sinterstore() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	int status = jedis.sinterstore("car", "foo", "bar");
	assertEquals(1, status);

	assertEquals(expected, jedis.smembers("car"));
    }

    @Test
    public void sunion() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");

	Set<String> union = jedis.sunion("foo", "bar");
	assertEquals(expected, union);
    }

    @Test
    public void sunionstore() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");

	int status = jedis.sunionstore("car", "foo", "bar");
	assertEquals(3, status);

	assertEquals(expected, jedis.smembers("car"));
    }

    @Test
    public void sdiff() throws JedisException {
	jedis.sadd("foo", "x");
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");
	jedis.sadd("foo", "c");

	jedis.sadd("bar", "c");

	jedis.sadd("car", "a");
	jedis.sadd("car", "d");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("x");
	expected.add("b");

	Set<String> diff = jedis.sdiff("foo", "bar", "car");
	assertEquals(expected, diff);
    }

    @Test
    public void sdiffstore() throws JedisException {
	jedis.sadd("foo", "x");
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");
	jedis.sadd("foo", "c");

	jedis.sadd("bar", "c");

	jedis.sadd("car", "a");
	jedis.sadd("car", "d");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("d");
	expected.add("a");

	int status = jedis.sdiffstore("tar", "foo", "bar", "car");
	assertEquals(2, status);
	assertEquals(expected, jedis.smembers("car"));
    }

    @Test
    public void srandmember() throws JedisException {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	String member = jedis.srandmember("foo");

	assertTrue("a".equals(member) || "b".equals(member));
	assertEquals(2, jedis.smembers("foo").size());

	member = jedis.srandmember("bar");
	assertNull(member);
    }

}