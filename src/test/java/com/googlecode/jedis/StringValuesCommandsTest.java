package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.testng.annotations.Test;

public class StringValuesCommandsTest extends JedisTestBase {

    String foo = "foo";
    String bar = "bar";
    String car = "car";

    @Test
    public void getSet() {
	assertThat(jedis.getSet(foo, bar), nullValue());
	assertThat(jedis.getSet(foo, car), is(bar));
    }

    //
    // @Test
    // public void mget() {
    // List<String> values = jedis.mget("foo", "bar");
    // List<String> expected = new ArrayList<String>();
    // expected.add(null);
    // expected.add(null);
    //
    // assertEquals(expected, values);
    //
    // jedis.set("foo", "bar");
    //
    // expected = new ArrayList<String>();
    // expected.add("bar");
    // expected.add(null);
    // values = jedis.mget("foo", "bar");
    //
    // assertEquals(expected, values);
    //
    // jedis.set("bar", "foo");
    //
    // expected = new ArrayList<String>();
    // expected.add("bar");
    // expected.add("foo");
    // values = jedis.mget("foo", "bar");
    //
    // assertEquals(expected, values);
    // }
    //
    // @Test
    // public void setnx() {
    // long status = jedis.setnx("foo", "bar");
    // assertEquals(1, status);
    // assertEquals("bar", jedis.get("foo"));
    //
    // status = jedis.setnx("foo", "bar2");
    // assertEquals(0, status);
    // assertEquals("bar", jedis.get("foo"));
    // }
    //
    // @Test
    // public void setex() {
    // String status = jedis.setex("foo", 20, "bar");
    // assertEquals("OK", status);
    // long ttl = jedis.ttl("foo");
    // assertTrue(ttl > 0 && ttl <= 20);
    // }
    //
    // @Test
    // public void mset() {
    // String status = jedis.mset("foo", "bar", "bar", "foo");
    // assertEquals("OK", status);
    // assertEquals("bar", jedis.get("foo"));
    // assertEquals("foo", jedis.get("bar"));
    // }
    //
    // @Test
    // public void msetnx() {
    // long status = jedis.msetnx("foo", "bar", "bar", "foo");
    // assertEquals(1, status);
    // assertEquals("bar", jedis.get("foo"));
    // assertEquals("foo", jedis.get("bar"));
    //
    // status = jedis.msetnx("foo", "bar1", "bar2", "foo2");
    // assertEquals(0, status);
    // assertEquals("bar", jedis.get("foo"));
    // assertEquals("foo", jedis.get("bar"));
    // }
    //
    // @Test(expected = JedisException.class)
    // public void incrWrongValue() {
    // jedis.set("foo", "bar");
    // jedis.incr("foo");
    // }
    //
    // @Test
    // public void incr() {
    // long value = jedis.incr("foo");
    // assertEquals(1, value);
    // value = jedis.incr("foo");
    // assertEquals(2, value);
    // }
    //
    // @Test(expected = JedisException.class)
    // public void incrByWrongValue() {
    // jedis.set("foo", "bar");
    // jedis.incrBy("foo", 2);
    // }
    //
    // @Test
    // public void incrBy() {
    // long value = jedis.incrBy("foo", 2);
    // assertEquals(2, value);
    // value = jedis.incrBy("foo", 2);
    // assertEquals(4, value);
    // }
    //
    // @Test(expected = JedisException.class)
    // public void decrWrongValue() {
    // jedis.set("foo", "bar");
    // jedis.decr("foo");
    // }
    //
    // @Test
    // public void decr() {
    // long value = jedis.decr("foo");
    // assertEquals(-1, value);
    // value = jedis.decr("foo");
    // assertEquals(-2, value);
    // }
    //
    // @Test(expected = JedisException.class)
    // public void decrByWrongValue() {
    // jedis.set("foo", "bar");
    // jedis.decrBy("foo", 2);
    // }
    //
    // @Test
    // public void decrBy() {
    // long value = jedis.decrBy("foo", 2);
    // assertEquals(-2, value);
    // value = jedis.decrBy("foo", 2);
    // assertEquals(-4, value);
    // }
    //
    // @Test
    // public void append() {
    // long value = jedis.append("foo", "bar");
    // assertEquals(3, value);
    // assertEquals("bar", jedis.get("foo"));
    // value = jedis.append("foo", "bar");
    // assertEquals(6, value);
    // assertEquals("barbar", jedis.get("foo"));
    // }
    //
    // @Test
    // public void substr() {
    // jedis.set("s", "This is a string");
    // assertEquals("This", jedis.substr("s", 0, 3));
    // assertEquals("ing", jedis.substr("s", -3, -1));
    // assertEquals("This is a string", jedis.substr("s", 0, -1));
    // assertEquals(" string", jedis.substr("s", 9, 100000));
    // }
    //
    // @Test
    // public void strlen() {
    // jedis.set("s", "This is a string");
    // assertEquals("This is a string".length(), jedis.strlen("s").intValue());
    // }
    //
    // @Test
    // public void incrLargeNumbers() {
    // long value = jedis.incr("foo");
    // assertEquals(1, value);
    // assertEquals(1L + Integer.MAX_VALUE, (long) jedis.incrBy("foo",
    // Integer.MAX_VALUE));
    // }
    //
    // @Test
    // public void incrReallyLargeNumbers() {
    // jedis.set("foo", Long.toString(Long.MAX_VALUE));
    // long value = jedis.incr("foo");
    // assertEquals(Long.MIN_VALUE, value);
    // }
    @Test
    public void setAndGet() {
	assertThat(jedis.set(foo, bar), is(true));
	assertThat(jedis.get("foo"), is("bar"));
	assertThat(jedis.get("bar"), is(nullValue()));
    }
}