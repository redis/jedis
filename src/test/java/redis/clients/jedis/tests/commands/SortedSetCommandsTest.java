package redis.clients.jedis.tests.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.JedisException;
import redis.clients.jedis.Tuple;

public class SortedSetCommandsTest extends JedisCommandTestBase {
    @Test
    public void zadd() throws JedisException {
	int status = jedis.zadd("foo", 1d, "a");
	assertEquals(1, status);

	status = jedis.zadd("foo", 10d, "b");
	assertEquals(1, status);

	status = jedis.zadd("foo", 0.1d, "c");
	assertEquals(1, status);

	status = jedis.zadd("foo", 2d, "a");
	assertEquals(0, status);
    }

    @Test
    public void zrange() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("c");
	expected.add("a");

	Set<String> range = jedis.zrange("foo", 0, 1);
	assertEquals(expected, range);

	expected.add("b");
	range = jedis.zrange("foo", 0, 100);
	assertEquals(expected, range);
    }

    @Test
    public void zrevrange() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");
	expected.add("a");

	Set<String> range = jedis.zrevrange("foo", 0, 1);
	assertEquals(expected, range);

	expected.add("c");
	range = jedis.zrevrange("foo", 0, 100);
	assertEquals(expected, range);
    }

    @Test
    public void zrem() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 2d, "b");

	int status = jedis.zrem("foo", "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	assertEquals(1, status);
	assertEquals(expected, jedis.zrange("foo", 0, 100));

	status = jedis.zrem("foo", "bar");

	assertEquals(0, status);
    }

    @Test
    public void zincrby() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 2d, "b");

	double score = jedis.zincrby("foo", 2d, "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");

	assertEquals(3d, score);
	assertEquals(expected, jedis.zrange("foo", 0, 100));
    }

    @Test
    public void zrank() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 2d, "b");

	int rank = jedis.zrank("foo", "a");
	assertEquals(0, rank);

	rank = jedis.zrank("foo", "b");
	assertEquals(1, rank);
    }

    @Test
    public void zrevrank() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 2d, "b");

	int rank = jedis.zrevrank("foo", "a");
	assertEquals(1, rank);

	rank = jedis.zrevrank("foo", "b");
	assertEquals(0, rank);
    }

    @Test
    public void zrangeWithScores() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	Set<Tuple> expected = new LinkedHashSet<Tuple>();
	expected.add(new Tuple("c", 0.1d));
	expected.add(new Tuple("a", 2d));

	Set<Tuple> range = jedis.zrangeWithScores("foo", 0, 1);
	assertEquals(expected, range);

	expected.add(new Tuple("b", 10d));
	range = jedis.zrangeWithScores("foo", 0, 100);
	assertEquals(expected, range);
    }

    @Test
    public void zrevrangeWithScores() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	Set<Tuple> expected = new LinkedHashSet<Tuple>();
	expected.add(new Tuple("b", 10d));
	expected.add(new Tuple("a", 2d));

	Set<Tuple> range = jedis.zrevrangeWithScores("foo", 0, 1);
	assertEquals(expected, range);

	expected.add(new Tuple("c", 0.1d));
	range = jedis.zrevrangeWithScores("foo", 0, 100);
	assertEquals(expected, range);
    }

    @Test
    public void zcard() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	int size = jedis.zcard("foo");
	assertEquals(3, size);
    }

    @Test
    public void zscore() throws JedisException {
	jedis.zadd("foo", 1d, "a");
	jedis.zadd("foo", 10d, "b");
	jedis.zadd("foo", 0.1d, "c");
	jedis.zadd("foo", 2d, "a");

	double score = jedis.zscore("foo", "b");
	assertEquals(10d, score);

	score = jedis.zscore("foo", "c");
	assertEquals(0.1d, score);
    }
}