package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

@SuppressWarnings("unchecked")
public class AllKindOfValuesCommandsTest extends JedisTestBase {

    @Test
    public void dbSize() {
	long size = jedis.dbSize();
	assertEquals(0, size);

	jedis.set("foo", "bar");
	size = jedis.dbSize();
	assertEquals(1, size);
    }

    @Test
    public void del() {
	jedis.set("foo1", "bar1");
	jedis.set("foo2", "bar2");
	jedis.set("foo3", "bar3");

	long reply = jedis.del(null, "foo1", "foo2", "foo3");
	assertEquals(3, reply);

	Boolean breply = jedis.exists("foo1");
	assertFalse(breply);
	breply = jedis.exists("foo2");
	assertFalse(breply);
	breply = jedis.exists("foo3");
	assertFalse(breply);

	jedis.set("foo1", "bar1");

	reply = jedis.del(null, "foo1", "foo2");
	assertEquals(1, reply);

	reply = jedis.del(null, "foo1", "foo2");
	assertEquals(0, reply);
    }

    @Test
    public void echo() {
	String result = jedis.echo("hello world");
	assertEquals("hello world", result);

	// Binary
	byte[] pong = "pong".getBytes(Protocol.DEFAULT_CHARSET);
	assertThat(jedis.echo(pong), is(pong));
    }

    @Test
    public void exists() {
	jedis.set("foo", "bar");
	assertThat(jedis.exists("foo"), is(true));
	assertThat(jedis.exists("bar"), is(false));
    }

    @Test
    public void expire() {
	long status = jedis.expire("foo", 20);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.expire("foo", 20);
	assertEquals(1, status);
    }

    @Test
    public void expireAt() {
	long unixTime = (System.currentTimeMillis() / 1000L) + 20;

	long status = jedis.expireAt("foo", unixTime);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	unixTime = (System.currentTimeMillis() / 1000L) + 20;
	status = jedis.expireAt("foo", unixTime);
	assertEquals(1, status);
    }

    @Test
    public void flushAll() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(0, jedis.dbSize().intValue());
    }

    @Test
    public void flushDB() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(1, jedis.dbSize().intValue());
	jedis.del(null, "bar");

    }

    @Test
    public void keys() {
	jedis.set("foo", "bar");
	jedis.set("foobar", "bar");

	Set<String> keys = jedis.keys("foo*");
	Set<String> expected = new HashSet<String>();
	expected.add("foo");
	expected.add("foobar");
	assertEquals(expected, keys);

	expected = new HashSet<String>();
	keys = jedis.keys("bar*");

	assertEquals(expected, keys);
    }

    @Test
    public void move() {
	long status = jedis.move("foo", 1);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.move("foo", 1);
	assertEquals(1, status);
	assertEquals(null, jedis.get("foo"));

	jedis.select(1);
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void persist() {
	jedis.setex("foo", 60 * 60, "bar");
	assertTrue(jedis.ttl("foo") > 0);
	long status = jedis.persist("foo");
	assertEquals(1, status);
	assertEquals(-1, jedis.ttl("foo").intValue());
    }

    @Test
    public void ping() {
	assertThat(jedis.ping(), is(true));
    }

    @Test
    public void randomKey() {
	assertEquals(null, jedis.randomKey());

	jedis.set("foo", "bar");

	assertEquals("foo", jedis.randomKey());

	jedis.set("bar", "foo");

	String randomkey = jedis.randomKey();
	assertTrue(randomkey.equals("foo") || randomkey.equals("bar"));
    }

    @Test
    public void rename() {
	jedis.set("foo", "bar");
	String status = jedis.rename("foo", "bar");
	assertEquals("OK", status);

	String value = jedis.get("foo");
	assertEquals(null, value);

	value = jedis.get("bar");
	assertEquals("bar", value);
    }

    @Test
    public void renamenx() {
	jedis.set("foo", "bar");
	long status = jedis.renamenx("foo", "bar");
	assertEquals(1, status);

	jedis.set("foo", "bar");
	status = jedis.renamenx("foo", "bar");
	assertEquals(0, status);
    }

    @Test
    public void renameOldAndNewAreTheSame() {
	try {
	    jedis.set("foo", "bar");
	    jedis.rename("foo", "foo");
	    fail("JedisException expected");
	} catch (final JedisException e) {
	}
    }

    @Test
    public void select() {
	jedis.set("foo", "bar");
	String status = jedis.select(1);
	assertEquals("OK", status);
	assertEquals(null, jedis.get("foo"));
	status = jedis.select(0);
	assertEquals("OK", status);
	assertEquals("bar", jedis.get("foo"));
    }

    @Test
    public void ttl() {
	jedis.set("foo", "bar");

	assertThat(jedis.ttl("foo"), is(-1L));
	jedis.expire("foo", 20);
	assertThat(jedis.ttl("foo"),
		allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(20L)));

	assertThat(jedis.ttl("bar"), is(-1L));
    }

    @Test
    public void type() {
	jedis.set("foo", "bar");
	assertThat(jedis.type("foo"), is("string"));
    }

}