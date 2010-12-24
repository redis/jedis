package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static redis.clients.jedis.PairImpl.newPair;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class SortedSetCommandsTest extends JedisTestBase {
    private final String foo = "foo";
    private final String bar = "bar";
    private final String car = "car";
    private final String a = "a";
    private final String b = "b";
    private final String c = "c";
    private final String d = "d";
    private final String e = "e";

    @Test
    public void zadd() {
	assertThat(jedis.zadd(foo, 1., bar), is(true));
	assertThat(jedis.zadd(foo, 2., bar), is(false));
	assertThat(jedis.zadd(foo, 2., foo), is(true));

	assertThat(jedis.zadd(bar, newPair(bar, 1.)), is(true));
	assertThat(jedis.zadd(bar, newPair(bar, 2.)), is(false));
	assertThat(jedis.zadd(bar, newPair(foo, 2.)), is(true));
    }

    @Test
    public void zcard() {
	assertThat(jedis.zcard(foo), is(0L));
	jedis.zadd(foo, 1L, bar);
	assertThat(jedis.zcard(foo), is(1L));
    }

    @Test
    public void zcount() {
	jedis.zadd(foo, 0, a);
	assertThat(jedis.zcount(foo, "0", "1"), is(1L));
	assertThat(jedis.zcount(foo, "(0", "+inf"), is(0L));
	assertThat(jedis.zcount(bar, "0", "1"), is(0L));
    }

    @Test
    public void zincrby() {
	assertThat(jedis.zincrby(foo, a, 1.), is(1.));
	assertThat(jedis.zincrby(foo, a, 1.), is(2.));
	assertThat(jedis.zincrby(foo, b, 1.), is(1.));
	assertThat(jedis.zincrby(foo, b, -1.), is(0.));
    }

    @Test
    public void zinterstore() {

    }

    @SuppressWarnings("unchecked")
    @Test
    public void zinterstoreMax() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zinterstoreMax(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(1L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(a, 4.)));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void zinterstoreMin() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zinterstoreMin(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(1L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(a, 1.)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zinterstoreSum() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zinterstoreSum(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(1L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(a, 5.)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zrange() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}
	assertThat(jedis.zrange(foo, 0L, 1L), contains(a, b));
	assertThat(jedis.zrange(foo, 0L, -1L), contains(a, b, c, d));

	assertThat(jedis.zrangeWithScores(foo, 0L, 1L),
		contains(newPair(a, 0.), newPair(b, 1.)));
	assertThat(
		jedis.zrangeWithScores(foo, 0L, -1L),
		contains(newPair(a, 0.), newPair(b, 1.), newPair(c, 2.),
			newPair(d, 3.)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zrangeByScore() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}

	assertThat(jedis.zrangeByScore(foo, "0.0", "1.0"), contains(a, b));
	assertThat(jedis.zrangeByScore(foo, "0", "(1"), contains(a));
	assertThat(jedis.zrangeByScore(foo, "(0", "1"), contains(b));
	assertThat(jedis.zrangeByScore(foo, "(0", "(2"), contains(b));
	assertThat(jedis.zrangeByScore(foo, "0", "+inf"), contains(a, b, c, d));
	assertThat(jedis.zrangeByScore(foo, "-inf", "+inf"),
		contains(a, b, c, d));
	assertThat(jedis.zrangeByScore(foo, "-inf", "(1"), contains(a));
	assertThat(jedis.zrangeByScore(foo, "10", "100"),
		Matchers.<String> empty());

	assertThat(jedis.zrangeByScore(foo, "-inf", "+inf", 0, 2),
		contains(a, b));

	assertThat(jedis.zrangeByScoreWithScores(foo, "0", "1"),
		contains(newPair(a, 0.), newPair(b, 1.)));

	assertThat(jedis.zrangeByScoreWithScores(foo, "-inf", "+inf", 0, 2),
		contains(newPair(a, 0.), newPair(b, 1.)));
    }

    @Test
    public void zrank() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}

	assertThat(jedis.zrank(foo, c), is(2L));
	assertThat(jedis.zrank(foo, e), nullValue());

    }

    @Test
    public void zrem() {
	jedis.zadd(foo, newPair(a, 0.));

	assertThat(jedis.zrem(foo, a), is(true));
	assertThat(jedis.zrem(foo, a), is(false));
    }

    @Test
    public void zremrangeByRank() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}
	assertThat(jedis.zremrangeByRank(foo, 1L, 3L), is(3L));
	assertThat(jedis.zremrangeByRank(bar, 1L, 3L), is(0L));
    }

    @Test
    public void zremrangeByScore() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	    jedis.zadd(bar, it);
	    jedis.zadd(car, it);
	}
	assertThat(jedis.zremrangeByScore(foo, "1.", "3."), is(3L));
	assertThat(jedis.zremrangeByScore(foo, "1.", "3."), is(0L));
	assertThat(jedis.zremrangeByScore(bar, "1.", "(3."), is(2L));
	assertThat(jedis.zremrangeByScore(car, "-inf", "+inf"), is(4L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zrevrange() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}
	assertThat(jedis.zrevrange(foo, 0L, 1L), contains(d, c));
	assertThat(jedis.zrevrange(foo, 0L, -1L), contains(d, c, b, a));

	assertThat(jedis.zrevrangeWithScores(foo, 0L, 1L),
		contains(newPair(d, 3.), newPair(c, 2.)));
	assertThat(
		jedis.zrevrangeWithScores(foo, 0L, -1L),
		contains(newPair(d, 3.), newPair(c, 2.), newPair(b, 1.),
			newPair(a, 0.)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zrevrangeByScore() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}

	assertThat(jedis.zrevrangeByScore(foo, "1.0", "0.0"), contains(b, a));
	assertThat(jedis.zrevrangeByScore(foo, "(1", "0"), contains(a));
	assertThat(jedis.zrevrangeByScore(foo, "1", "(0"), contains(b));
	assertThat(jedis.zrevrangeByScore(foo, "(2", "(0"), contains(b));
	assertThat(jedis.zrevrangeByScore(foo, "+inf", "0"),
		contains(d, c, b, a));
	assertThat(jedis.zrevrangeByScore(foo, "+inf", "-inf"),
		contains(d, c, b, a));
	assertThat(jedis.zrevrangeByScore(foo, "(1", "-inf"), contains(a));
	assertThat(jedis.zrevrangeByScore(foo, "100", "10"),
		Matchers.<String> empty());

	assertThat(jedis.zrevrangeByScore(foo, "+inf", "-inf", 0, 2),
		contains(d, c));

	assertThat(jedis.zrevrangeByScoreWithScores(foo, "1", "0"),
		contains(newPair(b, 1.), newPair(a, 0.)));

	assertThat(jedis.zrevrangeByScoreWithScores(foo, "+inf", "-inf", 0, 2),
		contains(newPair(d, 3.), newPair(c, 2.)));
    }

    @Test
    public void zrevrank() {
	for (Pair<String, Double> it : ImmutableList.of(newPair(a, 0.),
		newPair(b, 1.), newPair(c, 2.), newPair(d, 3.))) {
	    jedis.zadd(foo, it);
	}

	assertThat(jedis.zrevrank(foo, b), is(2L));
	assertThat(jedis.zrevrank(foo, e), nullValue());
    }

    @Test
    public void zscore() {
	jedis.zadd(foo, 2., a);
	assertThat(jedis.zscore(foo, a), is(2.));
	assertThat(jedis.zscore(foo, b), nullValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zuniunstoreMax() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zunionstoreMax(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(2L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(b, 1.), newPair(a, 4.)));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void zuniunstoreMin() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zunionstoreMin(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(2L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(a, 1.), newPair(b, 1.)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zuniunstoreSum() {
	jedis.zadd(foo, 1., a);
	jedis.zadd(foo, 1., b);
	jedis.zadd(bar, 2., a);

	assertThat(
		jedis.zunionstoreSum(car, newPair(foo, 1.), newPair(bar, 2.)),
		is(2L));
	assertThat(jedis.zrangeByScoreWithScores(car, "-inf", "+inf"),
		contains(newPair(b, 1.), newPair(a, 5.)));
    }

}