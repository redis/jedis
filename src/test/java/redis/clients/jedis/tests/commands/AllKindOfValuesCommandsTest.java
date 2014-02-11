package redis.clients.jedis.tests.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

public class AllKindOfValuesCommandsTest extends JedisCommandTestBase {
    final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
    final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x0A };
    final byte[] bfoo2 = { 0x01, 0x02, 0x03, 0x04, 0x0B };
    final byte[] bfoo3 = { 0x01, 0x02, 0x03, 0x04, 0x0C };
    final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
    final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
    final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
    final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };

    final byte[] bfoobar = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    final byte[] bfoostar = { 0x01, 0x02, 0x03, 0x04, '*' };
    final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

    @Test
    public void ping() {
	String status = jedis.ping();
	assertEquals("PONG", status);
    }

    @Test
    public void exists() {
	String status = jedis.set("foo", "bar");
	assertEquals("OK", status);

	status = jedis.set(bfoo, bbar);
	assertEquals("OK", status);

	boolean reply = jedis.exists("foo");
	assertTrue(reply);

	reply = jedis.exists(bfoo);
	assertTrue(reply);

	long lreply = jedis.del("foo");
	assertEquals(1, lreply);

	lreply = jedis.del(bfoo);
	assertEquals(1, lreply);

	reply = jedis.exists("foo");
	assertFalse(reply);

	reply = jedis.exists(bfoo);
	assertFalse(reply);
    }

    @Test
    public void del() {
	jedis.set("foo1", "bar1");
	jedis.set("foo2", "bar2");
	jedis.set("foo3", "bar3");

	long reply = jedis.del("foo1", "foo2", "foo3");
	assertEquals(3, reply);

	Boolean breply = jedis.exists("foo1");
	assertFalse(breply);
	breply = jedis.exists("foo2");
	assertFalse(breply);
	breply = jedis.exists("foo3");
	assertFalse(breply);

	jedis.set("foo1", "bar1");

	reply = jedis.del("foo1", "foo2");
	assertEquals(1, reply);

	reply = jedis.del("foo1", "foo2");
	assertEquals(0, reply);

	// Binary ...
	jedis.set(bfoo1, bbar1);
	jedis.set(bfoo2, bbar2);
	jedis.set(bfoo3, bbar3);

	reply = jedis.del(bfoo1, bfoo2, bfoo3);
	assertEquals(3, reply);

	breply = jedis.exists(bfoo1);
	assertFalse(breply);
	breply = jedis.exists(bfoo2);
	assertFalse(breply);
	breply = jedis.exists(bfoo3);
	assertFalse(breply);

	jedis.set(bfoo1, bbar1);

	reply = jedis.del(bfoo1, bfoo2);
	assertEquals(1, reply);

	reply = jedis.del(bfoo1, bfoo2);
	assertEquals(0, reply);
    }

    @Test
    public void type() {
	jedis.set("foo", "bar");
	String status = jedis.type("foo");
	assertEquals("string", status);

	// Binary
	jedis.set(bfoo, bbar);
	status = jedis.type(bfoo);
	assertEquals("string", status);
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

	// Binary
	jedis.set(bfoo, bbar);
	jedis.set(bfoobar, bbar);

	Set<byte[]> bkeys = jedis.keys(bfoostar);
	assertEquals(2, bkeys.size());
	assertTrue(setContains(bkeys, bfoo));
	assertTrue(setContains(bkeys, bfoobar));

	bkeys = jedis.keys(bbarstar);

	assertEquals(0, bkeys.size());
    }

    @Test
    public void randomKey() {
	assertEquals(null, jedis.randomKey());

	jedis.set("foo", "bar");

	assertEquals("foo", jedis.randomKey());

	jedis.set("bar", "foo");

	String randomkey = jedis.randomKey();
	assertTrue(randomkey.equals("foo") || randomkey.equals("bar"));

	// Binary
	jedis.del("foo");
	jedis.del("bar");
	assertEquals(null, jedis.randomKey());

	jedis.set(bfoo, bbar);

	assertArrayEquals(bfoo, jedis.randomBinaryKey());

	jedis.set(bbar, bfoo);

	byte[] randomBkey = jedis.randomBinaryKey();
	assertTrue(Arrays.equals(randomBkey, bfoo)
		|| Arrays.equals(randomBkey, bbar));

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

	// Binary
	jedis.set(bfoo, bbar);
	String bstatus = jedis.rename(bfoo, bbar);
	assertEquals("OK", bstatus);

	byte[] bvalue = jedis.get(bfoo);
	assertEquals(null, bvalue);

	bvalue = jedis.get(bbar);
	assertArrayEquals(bbar, bvalue);
    }

    @Test
    public void renameOldAndNewAreTheSame() {
	try {
	    jedis.set("foo", "bar");
	    jedis.rename("foo", "foo");
	    fail("JedisDataException expected");
	} catch (final JedisDataException e) {
	}

	// Binary
	try {
	    jedis.set(bfoo, bbar);
	    jedis.rename(bfoo, bfoo);
	    fail("JedisDataException expected");
	} catch (final JedisDataException e) {
	}

    }

    @Test
    public void renamenx() {
	jedis.set("foo", "bar");
	long status = jedis.renamenx("foo", "bar");
	assertEquals(1, status);

	jedis.set("foo", "bar");
	status = jedis.renamenx("foo", "bar");
	assertEquals(0, status);

	// Binary
	jedis.set(bfoo, bbar);
	long bstatus = jedis.renamenx(bfoo, bbar);
	assertEquals(1, bstatus);

	jedis.set(bfoo, bbar);
	bstatus = jedis.renamenx(bfoo, bbar);
	assertEquals(0, bstatus);

    }

    @Test
    public void dbSize() {
	long size = jedis.dbSize();
	assertEquals(0, size);

	jedis.set("foo", "bar");
	size = jedis.dbSize();
	assertEquals(1, size);

	// Binary
	jedis.set(bfoo, bbar);
	size = jedis.dbSize();
	assertEquals(2, size);
    }

    @Test
    public void expire() {
	long status = jedis.expire("foo", 20);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.expire("foo", 20);
	assertEquals(1, status);

	// Binary
	long bstatus = jedis.expire(bfoo, 20);
	assertEquals(0, bstatus);

	jedis.set(bfoo, bbar);
	bstatus = jedis.expire(bfoo, 20);
	assertEquals(1, bstatus);

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

	// Binary
	long bstatus = jedis.expireAt(bfoo, unixTime);
	assertEquals(0, bstatus);

	jedis.set(bfoo, bbar);
	unixTime = (System.currentTimeMillis() / 1000L) + 20;
	bstatus = jedis.expireAt(bfoo, unixTime);
	assertEquals(1, bstatus);

    }

    @Test
    public void ttl() {
	long ttl = jedis.ttl("foo");
	assertEquals(-2, ttl);

	jedis.set("foo", "bar");
	ttl = jedis.ttl("foo");
	assertEquals(-1, ttl);

	jedis.expire("foo", 20);
	ttl = jedis.ttl("foo");
	assertTrue(ttl >= 0 && ttl <= 20);

	// Binary
	long bttl = jedis.ttl(bfoo);
	assertEquals(-2, bttl);

	jedis.set(bfoo, bbar);
	bttl = jedis.ttl(bfoo);
	assertEquals(-1, bttl);

	jedis.expire(bfoo, 20);
	bttl = jedis.ttl(bfoo);
	assertTrue(bttl >= 0 && bttl <= 20);

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
	// Binary
	jedis.set(bfoo, bbar);
	String bstatus = jedis.select(1);
	assertEquals("OK", bstatus);
	assertEquals(null, jedis.get(bfoo));
	bstatus = jedis.select(0);
	assertEquals("OK", bstatus);
	assertArrayEquals(bbar, jedis.get(bfoo));
    }

    @Test
    public void getDB() {
	assertEquals(0, jedis.getDB().longValue());
	jedis.select(1);
	assertEquals(1, jedis.getDB().longValue());
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

	// Binary
	jedis.select(0);
	long bstatus = jedis.move(bfoo, 1);
	assertEquals(0, bstatus);

	jedis.set(bfoo, bbar);
	bstatus = jedis.move(bfoo, 1);
	assertEquals(1, bstatus);
	assertEquals(null, jedis.get(bfoo));

	jedis.select(1);
	assertArrayEquals(bbar, jedis.get(bfoo));

    }

    @Test
    public void flushDB() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	String status = jedis.flushDB();
	assertEquals("OK", status);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(1, jedis.dbSize().intValue());
	jedis.del("bar");

	// Binary
	jedis.select(0);
	jedis.set(bfoo, bbar);
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set(bbar, bfoo);
	jedis.move(bbar, 1);
	String bstatus = jedis.flushDB();
	assertEquals("OK", bstatus);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(1, jedis.dbSize().intValue());

    }

    @Test
    public void flushAll() {
	jedis.set("foo", "bar");
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set("bar", "foo");
	jedis.move("bar", 1);
	String status = jedis.flushAll();
	assertEquals("OK", status);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(0, jedis.dbSize().intValue());

	// Binary
	jedis.select(0);
	jedis.set(bfoo, bbar);
	assertEquals(1, jedis.dbSize().intValue());
	jedis.set(bbar, bfoo);
	jedis.move(bbar, 1);
	String bstatus = jedis.flushAll();
	assertEquals("OK", bstatus);
	assertEquals(0, jedis.dbSize().intValue());
	jedis.select(1);
	assertEquals(0, jedis.dbSize().intValue());

    }

    @Test
    public void persist() {
	jedis.setex("foo", 60 * 60, "bar");
	assertTrue(jedis.ttl("foo") > 0);
	long status = jedis.persist("foo");
	assertEquals(1, status);
	assertEquals(-1, jedis.ttl("foo").intValue());

	// Binary
	jedis.setex(bfoo, 60 * 60, bbar);
	assertTrue(jedis.ttl(bfoo) > 0);
	long bstatus = jedis.persist(bfoo);
	assertEquals(1, bstatus);
	assertEquals(-1, jedis.ttl(bfoo).intValue());

    }

    @Test
    public void echo() {
	String result = jedis.echo("hello world");
	assertEquals("hello world", result);

	// Binary
	byte[] bresult = jedis.echo(SafeEncoder.encode("hello world"));
	assertArrayEquals(SafeEncoder.encode("hello world"), bresult);
    }

    @Test
    public void dumpAndRestore() {
	jedis.set("foo1", "bar1");
	byte[] sv = jedis.dump("foo1");
	jedis.restore("foo2", 0, sv);
	assertTrue(jedis.exists("foo2"));
    }

    @Test
    public void pexpire() {
	long status = jedis.pexpire("foo", 10000);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.pexpire("foo", 10000);
	assertEquals(1, status);
    }

    @Test
    public void pexpireAt() {
	long unixTime = (System.currentTimeMillis()) + 10000;

	long status = jedis.pexpireAt("foo", unixTime);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	unixTime = (System.currentTimeMillis()) + 10000;
	status = jedis.pexpireAt("foo", unixTime);
	assertEquals(1, status);
    }

    @Test
    public void pttl() {
	long pttl = jedis.pttl("foo");
	assertEquals(-2, pttl);

	jedis.set("foo", "bar");
	pttl = jedis.pttl("foo");
	assertEquals(-1, pttl);

	jedis.pexpire("foo", 20000);
	pttl = jedis.pttl("foo");
	assertTrue(pttl >= 0 && pttl <= 20000);
    }

    @Test
    public void scan() {
	jedis.set("b", "b");
	jedis.set("a", "a");

	ScanResult<String> result = jedis.scan(SCAN_POINTER_START);

	assertEquals(SCAN_POINTER_START, result.getStringCursor());
	assertFalse(result.getResult().isEmpty());
    }

    @Test
    public void scanMatch() {
	ScanParams params = new ScanParams();
	params.match("a*");

	jedis.set("b", "b");
	jedis.set("a", "a");
	jedis.set("aa", "aa");
	ScanResult<String> result = jedis.scan(SCAN_POINTER_START, params);

	assertEquals(SCAN_POINTER_START, result.getStringCursor());
	assertFalse(result.getResult().isEmpty());
    }

    @Test
    public void scanCount() {
	ScanParams params = new ScanParams();
	params.count(2);

	for (int i = 0; i < 10; i++) {
	    jedis.set("a" + i, "a" + i);
	}

	ScanResult<String> result = jedis.scan(SCAN_POINTER_START, params);

	assertFalse(result.getResult().isEmpty());
    }
}
