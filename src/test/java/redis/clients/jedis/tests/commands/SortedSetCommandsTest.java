package redis.clients.jedis.tests.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;

public class SortedSetCommandsTest extends JedisCommandTestBase {
    @Test
    public void zadd() {
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
    public void zrange() {
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
    public void zrevrange() {
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
    public void zrem() {
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
    public void zincrby() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 2d, "b");

        double score = jedis.zincrby("foo", 2d, "a");

        Set<String> expected = new LinkedHashSet<String>();
        expected.add("a");
        expected.add("b");

        assertEquals(3d, score, 0);
        assertEquals(expected, jedis.zrange("foo", 0, 100));
    }

    @Test
    public void zrank() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 2d, "b");

        Integer rank = jedis.zrank("foo", "a");
        assertEquals(0, rank.intValue());

        rank = jedis.zrank("foo", "b");
        assertEquals(1, rank.intValue());

        rank = jedis.zrank("car", "b");
        assertNull(rank);
    }

    @Test
    public void zrevrank() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 2d, "b");

        int rank = jedis.zrevrank("foo", "a");
        assertEquals(1, rank);

        rank = jedis.zrevrank("foo", "b");
        assertEquals(0, rank);
    }

    @Test
    public void zrangeWithScores() {
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
    public void zrevrangeWithScores() {
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
    public void zcard() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        int size = jedis.zcard("foo");
        assertEquals(3, size);
    }

    @Test
    public void zscore() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        Double score = jedis.zscore("foo", "b");
        assertEquals((Double)10d, score);

        score = jedis.zscore("foo", "c");
        assertEquals((Double)0.1d, score);

        score = jedis.zscore("foo", "s");
        assertNull(score);
    }

    @Test
    public void zcount() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        int result = jedis.zcount("foo", 0.01d, 2.1d);

        assertEquals(2, result);
    }

    @Test
    public void zrangebyscore() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        Set<String> range = jedis.zrangeByScore("foo", 0d, 2d);

        Set<String> expected = new LinkedHashSet<String>();
        expected.add("c");
        expected.add("a");

        assertEquals(expected, range);

        range = jedis.zrangeByScore("foo", 0d, 2d, 0, 1);

        expected = new LinkedHashSet<String>();
        expected.add("c");

        assertEquals(expected, range);

        range = jedis.zrangeByScore("foo", 0d, 2d, 1, 1);
        Set<String> range2 = jedis.zrangeByScore("foo", "-inf", "(2");
        assertEquals(expected, range2);

        expected = new LinkedHashSet<String>();
        expected.add("a");

        assertEquals(expected, range);
    }

    @Test
    public void zrangebyscoreWithScores() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        Set<Tuple> range = jedis.zrangeByScoreWithScores("foo", 0d, 2d);

        Set<Tuple> expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("c", 0.1d));
        expected.add(new Tuple("a", 2d));

        assertEquals(expected, range);

        range = jedis.zrangeByScoreWithScores("foo", 0d, 2d, 0, 1);

        expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("c", 0.1d));

        assertEquals(expected, range);

        range = jedis.zrangeByScoreWithScores("foo", 0d, 2d, 1, 1);

        expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("a", 2d));

        assertEquals(expected, range);
    }

    @Test
    public void zremrangeByRank() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        int result = jedis.zremrangeByRank("foo", 0, 0);

        assertEquals(1, result);

        Set<String> expected = new LinkedHashSet<String>();
        expected.add("a");
        expected.add("b");

        assertEquals(expected, jedis.zrange("foo", 0, 100));
    }

    @Test
    public void zremrangeByScore() {
        jedis.zadd("foo", 1d, "a");
        jedis.zadd("foo", 10d, "b");
        jedis.zadd("foo", 0.1d, "c");
        jedis.zadd("foo", 2d, "a");

        int result = jedis.zremrangeByScore("foo", 0, 2);

        assertEquals(2, result);

        Set<String> expected = new LinkedHashSet<String>();
        expected.add("b");

        assertEquals(expected, jedis.zrange("foo", 0, 100));
    }

    @Test
    public void zunionstore() {
        jedis.zadd("foo", 1, "a");
        jedis.zadd("foo", 2, "b");
        jedis.zadd("bar", 2, "a");
        jedis.zadd("bar", 2, "b");

        int result = jedis.zunionstore("dst", "foo", "bar");

        assertEquals(2, result);

        Set<Tuple> expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("b", new Double(4)));
        expected.add(new Tuple("a", new Double(3)));

        assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));
    }

    @Test
    public void zunionstoreParams() {
        jedis.zadd("foo", 1, "a");
        jedis.zadd("foo", 2, "b");
        jedis.zadd("bar", 2, "a");
        jedis.zadd("bar", 2, "b");

        ZParams params = new ZParams();
        params.weights(2, 2);
        params.aggregate(ZParams.Aggregate.SUM);
        int result = jedis.zunionstore("dst", params, "foo", "bar");

        assertEquals(2, result);

        Set<Tuple> expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("b", new Double(8)));
        expected.add(new Tuple("a", new Double(6)));

        assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));
    }

    @Test
    public void zinterstore() {
        jedis.zadd("foo", 1, "a");
        jedis.zadd("foo", 2, "b");
        jedis.zadd("bar", 2, "a");

        int result = jedis.zinterstore("dst", "foo", "bar");

        assertEquals(1, result);

        Set<Tuple> expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("a", new Double(3)));

        assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));
    }

    @Test
    public void zintertoreParams() {
        jedis.zadd("foo", 1, "a");
        jedis.zadd("foo", 2, "b");
        jedis.zadd("bar", 2, "a");

        ZParams params = new ZParams();
        params.weights(2, 2);
        params.aggregate(ZParams.Aggregate.SUM);
        int result = jedis.zinterstore("dst", params, "foo", "bar");

        assertEquals(1, result);

        Set<Tuple> expected = new LinkedHashSet<Tuple>();
        expected.add(new Tuple("a", new Double(6)));

        assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));
    }
}