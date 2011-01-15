package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class HashesCommandsTest extends JedisTestBase {

    final private String bar = "bar";
    final private String car = "car";
    final private String foo = "foo";

    @Test
    public void hdel() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	assertThat(jedis.hdel(bar, foo), is(0L));
	assertThat(jedis.hdel(foo, foo), is(0L));
	assertThat(jedis.hdel(foo, bar), is(1L));
	assertThat(jedis.hdel(foo, bar), is(0L));
    }

    @Test
    public void hexists() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	assertThat(jedis.hexists(bar, foo), is(false));
	assertThat(jedis.hexists(foo, foo), is(false));
	assertThat(jedis.hexists(foo, bar), is(true));
    }

    @Test
    public void hget() {
	jedis.hset(foo, bar, car);
	assertThat(jedis.hget(bar, foo), is(nullValue()));
	assertThat(jedis.hget(foo, car), is(nullValue()));
	assertThat(jedis.hget(foo, bar), is(car));
    }

    @Test
    public void hgetAll() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	Map<String, String> result = jedis.hgetAll(foo);
	assertThat(result.size(), is(2));
	assertThat(result, allOf(hasEntry(car, bar), hasEntry(bar, car)));
    }

    @Test
    public void hincrBy() {
	long value = jedis.hincrBy(foo, bar, 1);
	assertThat(value, is(1L));
	value = jedis.hincrBy(foo, bar, -1);
	assertThat(value, is(0L));
	value = jedis.hincrBy(foo, bar, -10);
	assertThat(value, is(-10L));
    }

    @Test
    public void hkeys() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));
	assertThat(jedis.hkeys(foo), containsInAnyOrder(bar, car));
    }

    @Test
    public void hlen() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	assertThat(jedis.hlen(bar), is(0L));
	assertThat(jedis.hlen(foo), is(2L));
    }

    @Test
    public void hmget() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	assertThat(jedis.hmget(foo, bar, car, foo),
		contains(car, bar, (String) null));
    }

    @Test
    public void hmset() {

	assertThat(jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar)),
		is(true));
	assertThat(jedis.hget(foo, bar), is(car));
	assertThat(jedis.hget(foo, car), is(bar));
    }

    @Test
    public void hset() {
	long status = jedis.hset(foo, bar, car);
	assertThat(status, is(1L));
	status = jedis.hset(foo, bar, foo);
	assertThat(status, is(0L));
    }

    @Test
    public void hsetnx() {
	long status = jedis.hsetnx(foo, bar, car);
	assertThat(status, is(1L));
	assertThat(jedis.hget(foo, bar), is(car));

	status = jedis.hsetnx(foo, bar, foo);
	assertThat(status, is(0L));
	assertThat(jedis.hget(foo, bar), is(car));

	status = jedis.hsetnx(foo, car, bar);
	assertThat(status, is(1L));
	assertThat(jedis.hget(foo, car), is(bar));
    }

    @Test
    public void hvals() {
	jedis.hmset(foo, ImmutableMap.of(bar, car, car, bar));

	assertThat(jedis.hvals(foo), containsInAnyOrder(car, bar));
    }
}
