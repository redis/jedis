package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.fail;

import java.util.List;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ListCommandsTest extends JedisTestBase {
    final byte[] bfoo = "bfoo".getBytes(Protocol.DEFAULT_CHARSET);
    final byte[] bbar = "bbar".getBytes(Protocol.DEFAULT_CHARSET);
    final byte[] bcar = "bcar".getBytes(Protocol.DEFAULT_CHARSET);
    final byte[] bsrc = "bsrc".getBytes(Protocol.DEFAULT_CHARSET);
    final byte[] bdst = "bdst".getBytes(Protocol.DEFAULT_CHARSET);
    final byte[] bA = { 0x0A };
    final byte[] bB = { 0x0B };
    final byte[] bC = { 0x0C };
    final byte[] b1 = { 0x01 };
    final byte[] b2 = { 0x02 };
    final byte[] b3 = { 0x03 };
    final byte[] bhello = { 0x04, 0x02 };
    final byte[] bx = { 0x02, 0x04 };

    @Test
    public void blpop() throws InterruptedException {
	assertThat(jedis.blpop(1, "foo"), notNullValue());

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		try {
		    Jedis j = JedisFactory.newJedisInstance(c1);
		    j.lpush("foo", "bar");
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	assertThat(jedis.blpop(1, "foo"), contains("foo", "bar"));

	// raw
	assertThat(jedis.blpop(1, bfoo), notNullValue());

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		try {
		    Jedis j = JedisFactory.newJedisInstance(c1);
		    j.lpush(bfoo, bbar);
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	assertThat(jedis.blpop(1, bfoo), contains(bfoo, bbar));
    }

    @Test
    public void brpop() throws InterruptedException {
	assertThat(jedis.brpop(1, "foo"), notNullValue());

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		try {
		    Jedis j = JedisFactory.newJedisInstance(c1);
		    j.lpush("foo", "bar");
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	assertThat(jedis.brpop(1, "foo"), contains("foo", "bar"));

	// raw
	assertThat(jedis.brpop(1, bfoo), notNullValue());

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		try {
		    Jedis j = JedisFactory.newJedisInstance(c1);
		    j.lpush(bfoo, bbar);
		    j.disconnect();
		} catch (Exception ex) {
		    fail(ex.getMessage());
		}
	    }
	}).start();

	assertThat(jedis.brpop(1, bfoo), contains(bfoo, bbar));
    }

    @Test
    public void lindex() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	assertThat(jedis.lset("foo", 1, "bar"), is("OK"));
	assertThat(jedis.lrange("foo", 0, 100), contains("3", "bar", "1"));

	// Binary
	jedis.lpush(bfoo, b1);
	jedis.lpush(bfoo, b2);
	jedis.lpush(bfoo, b3);

	assertThat(jedis.lset(bfoo, 1, bbar), is("OK"));
	assertThat(jedis.lrange(bfoo, 0, 100), contains(b3, bbar, b1));
    }

    @Test(expectedExceptions = JedisException.class)
    public void linsertAfterRaw() {
	assertThat("foo is no key", jedis.linsertAfter(bfoo, bbar, bcar),
		is(0L));

	jedis.rpush(bfoo, bA);
	jedis.rpush(bfoo, bC);

	assertThat(jedis.linsertAfter(bfoo, bA, bB), is(3L));

	assertThat(jedis.lrange(bfoo, 0, 100), contains(bA, bB, bC));

	assertThat("element not in list", jedis.linsertAfter(bfoo, bbar, bcar),
		is(-1L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.linsertAfter(bbar, bfoo, bcar);
    }

    @Test(expectedExceptions = JedisException.class)
    public void linsertAfterString() {
	assertThat("foo is no key", jedis.linsertAfter("foo", "bar", "car"),
		is(0L));

	jedis.rpush("foo", "a");
	jedis.rpush("foo", "c");

	assertThat(jedis.linsertAfter("foo", "a", "b"), is(3L));

	assertThat(jedis.lrange("foo", 0, 100), contains("a", "b", "c"));

	assertThat("element not in list",
		jedis.linsertAfter("foo", "bar", "car"), is(-1L));

	// get exception
	jedis.set("bar", "foo");
	jedis.linsertAfter("bar", "foo", "car");
    }

    @Test(expectedExceptions = JedisException.class)
    public void linsertBeforeRaw() {
	assertThat("foo is no key", jedis.linsertBefore(bfoo, bbar, bcar),
		is(0L));

	jedis.rpush(bfoo, bA);
	jedis.rpush(bfoo, bC);

	assertThat(jedis.linsertBefore(bfoo, bC, bB), is(3L));

	assertThat(jedis.lrange(bfoo, 0, 100), contains(bA, bB, bC));

	assertThat("element not in list",
		jedis.linsertBefore(bfoo, bbar, bcar), is(-1L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.linsertBefore(bbar, bfoo, bcar);
    }

    @Test(expectedExceptions = JedisException.class)
    public void linsertBeforeString() {
	assertThat("foo is no key", jedis.linsertBefore("foo", "bar", "car"),
		is(0L));

	jedis.rpush("foo", "a");
	jedis.rpush("foo", "c");

	assertThat(jedis.linsertBefore("foo", "c", "b"), is(3L));

	assertThat(jedis.lrange("foo", 0, 100), contains("a", "b", "c"));

	assertThat("element not in list",
		jedis.linsertBefore("foo", "bar", "car"), is(-1L));

	// get exception
	jedis.set("bar", "foo");
	jedis.linsertBefore("bar", "foo", "car");
    }

    @Test
    public void llen() {
	assertThat(jedis.llen("foo"), is(0L));
	jedis.lpush("foo", "bar");
	jedis.lpush("foo", "car");
	assertThat(jedis.llen("foo"), is(2L));

	// Binary
	assertThat(jedis.llen(bfoo), is(0L));
	jedis.lpush(bfoo, bbar);
	jedis.lpush(bfoo, bcar);
	assertThat(jedis.llen(bfoo), is(2L));

    }

    @Test(expectedExceptions = JedisException.class)
    public void llenNotOnListRaw() {
	jedis.set(bfoo, bbar);
	jedis.llen(bfoo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void llenNotOnListString() {
	jedis.set("foo", "bar");
	jedis.llen("foo");
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpopRaw() {
	for (byte[] it : ImmutableList.of(bA, bB, bC)) {
	    jedis.rpush(bfoo, it);
	}

	assertThat(jedis.lpop(bfoo), is(bA));
	assertThat(jedis.lpop(bfoo), is(bB));
	assertThat(jedis.lpop(bfoo), is(bC));
	assertThat(jedis.lpop(bfoo), is(nullValue()));

	assertThat(jedis.lpop(bbar), is(nullValue()));

	// get exception
	jedis.set(bbar, bbar);
	assertThat(jedis.lpop(bbar), is(nullValue()));
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpopString() {
	for (String it : ImmutableList.of("a", "b", "c")) {
	    jedis.rpush("foo", it);
	}

	assertThat(jedis.lpop("foo"), is("a"));
	assertThat(jedis.lpop("foo"), is("b"));
	assertThat(jedis.lpop("foo"), is("c"));
	assertThat(jedis.lpop("foo"), is(nullValue()));

	assertThat(jedis.lpop("bar"), is(nullValue()));

	// get exception
	jedis.set("bar", "bar");
	assertThat(jedis.lpop("bar"), is(nullValue()));
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpushRaw() {
	assertThat(jedis.lpush(bfoo, bbar), is(1L));
	assertThat(jedis.lpush(bfoo, bbar), is(2L));
	assertThat(jedis.lpush(bfoo, bfoo), is(3L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.lpush(bbar, bfoo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpushString() {
	assertThat(jedis.lpush("foo", "bar"), is(1L));
	assertThat(jedis.lpush("foo", "bar"), is(2L));
	assertThat(jedis.lpush("foo", "foo"), is(3L));

	// get exception
	jedis.set("bar", "foo");
	jedis.lpush("bar", "foo");
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpushxRaw() {
	assertThat(jedis.lpushx(bfoo, bfoo), is(0L));
	jedis.lpush(bfoo, bbar);
	assertThat(jedis.lpushx(bfoo, bfoo), is(2L));

	jedis.lpop(bfoo);
	jedis.lpop(bfoo);
	assertThat(jedis.lpushx(bfoo, bfoo), is(0L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.lpushx(bbar, bfoo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void lpushxString() {
	assertThat(jedis.lpushx("foo", "bar"), is(0L));
	jedis.lpush("foo", "bar");
	assertThat(jedis.lpushx("foo", "foo"), is(2L));

	jedis.lpop("foo");
	jedis.lpop("foo");
	assertThat(jedis.lpushx("foo", "foo"), is(0L));

	// get exception
	jedis.set("bar", "foo");
	jedis.lpushx("bar", "foo");
    }

    @Test
    public void lrange() {
	jedis.rpush("foo", "a");
	jedis.rpush("foo", "b");
	jedis.rpush("foo", "c");

	assertThat(jedis.lrange("foo", 0, 2), contains("a", "b", "c"));
	assertThat(jedis.lrange("foo", 0, 20), contains("a", "b", "c"));
	assertThat(jedis.lrange("foo", 1, 2), contains("b", "c"));
	// looks awful, see:
	// https://code.google.com/p/hamcrest/issues/detail?id=97
	assertThat(jedis.lrange("foo", 2, 1), Matchers.<String> empty());

	// Binary
	jedis.rpush(bfoo, bA);
	jedis.rpush(bfoo, bB);
	jedis.rpush(bfoo, bC);

	assertThat(jedis.lrange(bfoo, 0, 2), contains(bA, bB, bC));
	assertThat(jedis.lrange(bfoo, 0, 20), contains(bA, bB, bC));
	assertThat(jedis.lrange(bfoo, 1, 2), contains(bB, bC));
	// looks awful, see:
	// https://code.google.com/p/hamcrest/issues/detail?id=97
	assertThat(jedis.lrange(bfoo, 2, 1), Matchers.<byte[]> empty());
    }

    @Test
    public void lrem() {
	ImmutableList<String> list = ImmutableList.of("hello", "hello", "x",
		"hello", "c", "b", "a");
	for (String it : list) {
	    jedis.lpush("foo", it);
	}

	assertThat(jedis.lrem("foo", -2, "hello"), is(2L));
	assertThat(jedis.lrange("foo", 0, 1000),
		contains("a", "b", "c", "hello", "x"));
	List<String> expected = list.reverse().subList(0, list.size() - 2);
	assertThat(jedis.lrange("foo", 0, 1000), is(expected));
	assertThat(jedis.lrem("bar", 100, "foo"), is(0L));

	// Binary
	ImmutableList<byte[]> blist = ImmutableList.of(bhello, bhello, bx,
		bhello, bC, bB, bA);
	for (byte[] it : blist) {
	    jedis.lpush(bfoo, it);
	}

	assertThat(jedis.lrem(bfoo, -2, bhello), is(2L));
	assertThat(jedis.lrange(bfoo, 0, 1000),
		contains(bA, bB, bC, bhello, bx));
	assertThat(jedis.lrem(bbar, 100, bfoo), is(0L));
    }

    @Test
    public void lset() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	assertThat(jedis.lindex("foo", 0), is("3"));
	assertThat(jedis.lindex("foo", 100), nullValue());

	// Binary
	jedis.lpush(bfoo, b1);
	jedis.lpush(bfoo, b2);
	jedis.lpush(bfoo, b3);

	assertThat(jedis.lindex(bfoo, 0), is(b3));
	assertThat(jedis.lindex(bfoo, 100), nullValue());
    }

    @Test
    public void ltrim() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");

	assertThat(jedis.ltrim("foo", 0, 1), is("OK"));
	assertThat(jedis.llen("foo"), is(2L));
	assertThat(jedis.lrange("foo", 0, 100), contains("3", "2"));

	// Binary
	jedis.lpush(bfoo, b1);
	jedis.lpush(bfoo, b2);
	jedis.lpush(bfoo, b3);

	assertThat(jedis.ltrim(bfoo, 0, 1), is("OK"));
	assertThat(jedis.llen(bfoo), is(2L));
	assertThat(jedis.lrange(bfoo, 0, 100), contains(b3, b2));
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpoplpushRaw() {
	for (byte[] it : ImmutableSet.of(bA, bB, bC)) {
	    jedis.rpush(bsrc, it);
	}
	for (byte[] it : ImmutableSet.of(b1, b2)) {
	    jedis.rpush(bdst, it);
	}

	// rotate
	assertThat(jedis.rpoplpush(bsrc, bsrc), is(bC));
	assertThat(jedis.rpoplpush(bsrc, bsrc), is(bB));
	assertThat(jedis.rpoplpush(bsrc, bsrc), is(bA));
	assertThat(jedis.lrange(bsrc, 0, 100), contains(bA, bB, bC));

	// move
	assertThat(jedis.rpoplpush(bsrc, bdst), is(bC));
	assertThat(jedis.rpoplpush(bsrc, bdst), is(bB));
	assertThat(jedis.rpoplpush(bsrc, bdst), is(bA));
	assertThat(jedis.rpoplpush(bsrc, bdst), is(nullValue()));
	assertThat(jedis.lrange(bdst, 0, 100), contains(bA, bB, bC, b1, b2));

	// no list at src and dst
	assertThat(jedis.rpoplpush(bfoo, bbar), is(nullValue()));
	// no list at dst
	assertThat(jedis.rpoplpush(bdst, bbar), is(b2));

	// get exception
	jedis.set(bfoo, bbar);
	jedis.rpoplpush(bfoo, bdst);
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpoplpushString() {
	for (String it : ImmutableSet.of("a", "b", "c")) {
	    jedis.rpush("src", it);
	}
	for (String it : ImmutableSet.of("1", "2")) {
	    jedis.rpush("dst", it);
	}

	// rotate
	assertThat(jedis.rpoplpush("src", "src"), is("c"));
	assertThat(jedis.rpoplpush("src", "src"), is("b"));
	assertThat(jedis.rpoplpush("src", "src"), is("a"));
	assertThat(jedis.lrange("src", 0, 100), contains("a", "b", "c"));

	// move
	assertThat(jedis.rpoplpush("src", "dst"), is("c"));
	assertThat(jedis.rpoplpush("src", "dst"), is("b"));
	assertThat(jedis.rpoplpush("src", "dst"), is("a"));
	assertThat(jedis.rpoplpush("src", "dst"), is(nullValue()));
	assertThat(jedis.lrange("dst", 0, 100),
		contains("a", "b", "c", "1", "2"));

	// no list at src and dst
	assertThat(jedis.rpoplpush("bar", "bar"), is(nullValue()));
	// no list at dst
	assertThat(jedis.rpoplpush("dst", "bar"), is("2"));

	// get exception
	jedis.set("foo", "bar");
	jedis.rpoplpush("foo", "dst");
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpopRaw() {
	for (byte[] it : ImmutableList.of(bA, bB, bC)) {
	    jedis.rpush(bfoo, it);
	}

	assertThat(jedis.rpop(bfoo), is(bC));
	assertThat(jedis.rpop(bfoo), is(bB));
	assertThat(jedis.rpop(bfoo), is(bA));
	assertThat(jedis.rpop(bfoo), is(nullValue()));

	assertThat(jedis.rpop(bbar), is(nullValue()));

	// get exception
	jedis.set(bbar, bbar);
	assertThat(jedis.rpop(bbar), is(nullValue()));
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpopString() {
	for (String it : ImmutableList.of("a", "b", "c")) {
	    jedis.rpush("foo", it);
	}

	assertThat(jedis.rpop("foo"), is("c"));
	assertThat(jedis.rpop("foo"), is("b"));
	assertThat(jedis.rpop("foo"), is("a"));
	assertThat(jedis.rpop("foo"), is(nullValue()));

	assertThat(jedis.rpop("bar"), is(nullValue()));

	// get exception
	jedis.set("bar", "bar");
	assertThat(jedis.rpop("bar"), is(nullValue()));
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpushRaw() {
	assertThat(jedis.rpush(bfoo, bbar), is(1L));
	assertThat(jedis.rpush(bfoo, bbar), is(2L));
	assertThat(jedis.rpush(bfoo, bfoo), is(3L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.rpush(bbar, bfoo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpushString() {
	assertThat(jedis.rpush("foo", "bar"), is(1L));
	assertThat(jedis.rpush("foo", "bar"), is(2L));
	assertThat(jedis.rpush("foo", "foo"), is(3L));

	// get exception
	jedis.set("bar", "foo");
	jedis.rpush("bar", "foo");
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpushxRaw() {
	assertThat(jedis.rpushx(bfoo, bfoo), is(0L));
	jedis.lpush(bfoo, bbar);
	assertThat(jedis.rpushx(bfoo, bfoo), is(2L));

	jedis.lpop(bfoo);
	jedis.lpop(bfoo);
	assertThat(jedis.rpushx(bfoo, bfoo), is(0L));

	// get exception
	jedis.set(bbar, bfoo);
	jedis.rpushx(bbar, bfoo);
    }

    @Test(expectedExceptions = JedisException.class)
    public void rpushxString() {
	assertThat(jedis.rpushx("foo", "bar"), is(0L));
	jedis.lpush("foo", "bar");
	assertThat(jedis.rpushx("foo", "foo"), is(2L));

	jedis.lpop("foo");
	jedis.lpop("foo");
	assertThat(jedis.rpushx("foo", "foo"), is(0L));

	// get exception
	jedis.set("bar", "foo");
	jedis.rpushx("bar", "foo");
    }
}