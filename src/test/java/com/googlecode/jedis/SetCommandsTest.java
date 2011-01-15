package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class SetCommandsTest extends JedisTestBase {
    private final String foo = "foo";
    private final String bar = "bar";
    private final String car = "car";
    private final String src = "src";
    private final String dst = "dst";
    private final String a = "a";
    private final String b = "b";
    private final String c = "c";
    private final String d = "d";

    @Test
    public void sadd() {
	assertThat(jedis.sadd(foo, a), is(true));
	assertThat(jedis.sadd(foo, a), is(false));
    }

    @Test
    public void scard() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	assertThat(jedis.scard(foo), is(2L));
	assertThat(jedis.scard(bar), is(0L));
    }

    @Test
    public void sdiff() {
	for (String it : ImmutableList.of(a, b, c)) {
	    jedis.sadd(foo, it);
	}
	jedis.sadd(bar, c);
	jedis.sadd(car, a);
	jedis.sadd(car, d);

	assertThat(jedis.sdiff(foo), containsInAnyOrder(a, b, c));
	assertThat(jedis.sdiff(foo, bar), containsInAnyOrder(a, b));
	assertThat(jedis.sdiff(foo, bar, car), containsInAnyOrder(b));
    }

    @Test
    public void sdiffstore() {
	for (String it : ImmutableList.of(a, b, c)) {
	    jedis.sadd(foo, it);
	}
	jedis.sadd(bar, c);
	jedis.sadd(car, a);
	jedis.sadd(car, d);

	assertThat(jedis.sdiffstore(dst, foo), is(3L));
	assertThat(jedis.sdiff(dst), containsInAnyOrder(a, b, c));
	assertThat(jedis.sdiffstore(dst, foo, bar), is(2L));
	assertThat(jedis.sdiffstore(dst, foo, bar, car), is(1L));
    }

    @Test
    public void sinter() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	jedis.sadd(bar, a);
	jedis.sadd(bar, b);
	jedis.sadd(bar, c);

	assertThat(jedis.sinter(foo), containsInAnyOrder(a, b));
	assertThat(jedis.sinter(foo, bar), containsInAnyOrder(a, b));

	// looks awful, see:
	// https://code.google.com/p/hamcrest/issues/detail?id=97
	assertThat(jedis.sinter(foo, bar, car), Matchers.<String> empty());
    }

    @Test
    public void sinterstore() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	jedis.sadd(bar, a);
	jedis.sadd(bar, b);
	jedis.sadd(bar, c);

	assertThat(jedis.sinterstore(car, foo, bar), is(2L));
	assertThat(jedis.sinter(car), containsInAnyOrder(a, b));
    }

    @Test
    public void sismember() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);
	assertThat(jedis.sismember(foo, a), is(true));
	assertThat(jedis.sismember(foo, c), is(false));
	assertThat(jedis.sismember(bar, a), is(false));
    }

    @Test
    public void smembers() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	assertThat(jedis.smembers(foo), containsInAnyOrder(a, b));
	assertThat(jedis.smembers(bar), Matchers.<String> empty());
    }

    @Test
    public void smove() {
	jedis.sadd(src, a);
	jedis.sadd(src, b);
	jedis.sadd(dst, c);

	assertThat(jedis.smove(src, dst, a), is(true));
	assertThat(jedis.smembers(src), contains(b));
	assertThat(jedis.smembers(dst), containsInAnyOrder(a, c));

	assertThat(jedis.smove(src, dst, a), is(false));
	assertThat(jedis.smove(foo, dst, a), is(false));
	assertThat(jedis.smove(dst, dst, a), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void spop() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	assertThat(jedis.spop(foo), anyOf(equalTo(a), equalTo(b)));
	assertThat(jedis.spop(foo), anyOf(equalTo(a), equalTo(b)));
	assertThat(jedis.spop(foo), is(nullValue()));
	assertThat(jedis.spop(bar), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void srandmember() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);

	assertThat(jedis.srandmember(foo), anyOf(equalTo(a), equalTo(b)));
	assertThat(jedis.srandmember(foo), anyOf(equalTo(a), equalTo(b)));
	assertThat(jedis.srandmember(bar), is(nullValue()));

    }

    public void srem() {
	assertThat(jedis.srem(foo, a), is(false));

	jedis.sadd(foo, a);
	jedis.sadd(foo, b);
	assertThat(jedis.srem(foo, a), is(true));

	assertThat(jedis.smembers(foo), contains(b));

	assertThat(jedis.srem(foo, a), is(false));
    }

    @Test
    public void sunion() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);
	jedis.sadd(bar, b);
	jedis.sadd(bar, c);

	assertThat(jedis.sunion(foo, bar), containsInAnyOrder(a, b, c));
    }

    @Test
    public void sunionstore() {
	jedis.sadd(foo, a);
	jedis.sadd(foo, b);
	jedis.sadd(bar, b);
	jedis.sadd(bar, c);

	assertThat(jedis.sunionstore(car, foo, bar), is(3L));
	assertThat(jedis.sinter(car), containsInAnyOrder(a, b, c));
    }
}