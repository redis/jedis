package com.googlecode.jedis;

import static com.googlecode.jedis.PairImpl.newPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("unchecked")
public class AllKindOfValuesCommandsTest extends JedisTestBase {

    String bar = "bar";
    String foo = "foo";
    String car = "car";

    @Test
    public void dbSize() {
	assertThat(jedis.dbSize(), is(0L));
	jedis.set(foo, bar);
	assertThat(jedis.dbSize(), is(1L));
    }

    @Test
    public void del() {
	for (String it : ImmutableList.of(foo, bar, car)) {
	    jedis.set(it, it);
	}
	assertThat(jedis.del(foo, bar, car), is(3L));
	for (String it : ImmutableList.of(foo, bar, car)) {
	    assertThat(jedis.exists(it), is(false));
	}

	jedis.set(foo, foo);
	assertThat(jedis.del(foo, bar, car), is(1L));
	assertThat(jedis.del(foo, bar, car), is(0L));
    }

    @Test
    public void echo() {
	assertThat(jedis.echo("hello world"), is("hello world"));
    }

    @Test
    public void exists() {
	jedis.set(foo, bar);
	assertThat(jedis.exists(foo), is(true));
	assertThat(jedis.exists(bar), is(false));
    }

    @Test
    public void expire() {
	assertThat(jedis.expire(foo, 20), is(false));
	jedis.set(foo, bar);
	assertThat(jedis.expire(foo, 20), is(true));
    }

    @Test
    public void expireAt() {
	long unixTime = (System.currentTimeMillis() / 1000L) + 20;

	assertThat(jedis.expireAt(foo, unixTime), is(false));
	jedis.set(foo, bar);
	assertThat(jedis.expireAt(foo, unixTime), is(true));
    }

    @Test
    public void flushAll() {
	jedis.set(foo, bar);
	assertThat(jedis.dbSize(), is(1L));
	jedis.select(1);
	jedis.set(foo, bar);
	assertThat(jedis.dbSize(), is(1L));
	jedis.flushAll();
	assertThat(jedis.dbSize(), is(0L));
	jedis.select(0);
	assertThat(jedis.dbSize(), is(0L));

    }

    @Test
    public void flushDB() {
	jedis.set(foo, bar);
	assertThat(jedis.dbSize(), is(1L));
	jedis.flushDB();
	assertThat(jedis.dbSize(), is(0L));
    }

    @Test
    public void keys() {
	jedis.set(foo, bar);
	jedis.set(foo + bar, bar);
	assertThat(jedis.keys(foo + "*"), containsInAnyOrder(foo, foo + bar));
    }

    @Test
    public void move() {
	assertThat(jedis.move(foo, 1), is(false));
	jedis.set(foo, bar);
	assertThat(jedis.move(foo, 1), is(true));
	assertThat(jedis.get(foo), is((String) null));
	jedis.select(1);
	assertThat(jedis.get(foo), is(bar));
    }

    @Test
    public void mset() {
	jedis.mset(newPair(foo, bar));
	assertThat(jedis.dbSize(), is(1L));

	jedis.mset(newPair(bar, foo), newPair(car, foo));
	assertThat(jedis.dbSize(), is(3L));
    }

    @Test
    public void msetnx() {
	assertThat(jedis.msetnx(newPair(foo, bar)), is(true));
	assertThat(jedis.dbSize(), is(1L));

	assertThat(jedis.msetnx(newPair(bar, foo), newPair(car, foo)), is(true));
	assertThat(jedis.dbSize(), is(3L));

	assertThat(jedis.msetnx(newPair(foo, bar)), is(false));
    }

    @Test
    public void persist() {
	jedis.setex(foo, bar, 60 * 60);
	assertThat(jedis.ttl(foo), greaterThan(0L));
	assertThat(jedis.persist(foo), is(true));
	assertThat(jedis.ttl(foo), is(-1L));
    }

    @Test
    public void ping() {
	assertThat(jedis.ping(), is(true));
    }

    @Test
    public void randomKey() {
	assertThat(jedis.randomKey(), nullValue());
	jedis.set(foo, bar);
	assertThat(jedis.randomKey(), is(foo));
	jedis.set(bar, foo);
	assertThat(jedis.randomKey(), anyOf(equalTo(foo), equalTo(bar)));
    }

    @Test
    public void rename() {
	jedis.set(foo, bar);
	assertThat(jedis.rename(foo, bar), is(true));
	assertThat(jedis.get(foo), nullValue());
	assertThat(jedis.get(bar), is(bar));
    }

    @Test
    public void renamenx() {
	jedis.set(foo, bar);
	assertThat(jedis.renamenx(foo, bar), is(true));
	jedis.set(foo, bar);
	assertThat(jedis.renamenx(foo, bar), is(false));
    }

    @Test(expectedExceptions = JedisException.class)
    public void renamenxWithOldAndNewKeyIsNotSet() {
	jedis.renamenx(foo, bar);
    }

    @Test(expectedExceptions = JedisException.class)
    public void renamenxWithOldAndNewKeyIsSame() {
	jedis.set(foo, foo);
	jedis.renamenx(foo, foo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void renameWithOldAndNewKeyIsNotSet() {
	jedis.rename(foo, bar);
    }

    @Test(expectedExceptions = JedisException.class)
    public void renameWithOldAndNewKeyIsSame() {
	jedis.set(foo, foo);
	jedis.rename(foo, foo);
    }

    @Test
    public void select() {
	jedis.set(foo, bar);
	assertThat(jedis.select(1), is(true));
	assertThat(jedis.get(foo), nullValue());
	assertThat(jedis.select(0), is(true));
	assertThat(jedis.get(foo), is(bar));
    }

    @Test
    public void ttl() {
	jedis.set(foo, bar);

	assertThat(jedis.ttl(foo), is(-1L));
	jedis.expire(foo, 20);
	assertThat(jedis.ttl(foo),
		allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(20L)));

	assertThat(jedis.ttl(bar), is(-1L));
    }

    @Test
    public void type() {
	assertThat(jedis.type(foo), is(RedisType.NONE));
	jedis.set(foo, bar);
	assertThat(jedis.type(foo), is(RedisType.STRING));
	jedis.del(foo);
	jedis.lpush(foo, bar);
	assertThat(jedis.type(foo), is(RedisType.LIST));
	jedis.del(foo);
	jedis.sadd(foo, bar);
	assertThat(jedis.type(foo), is(RedisType.SET));
	jedis.del(foo);
	jedis.hset(foo, bar, bar);
	assertThat(jedis.type(foo), is(RedisType.HASH));
    }

}