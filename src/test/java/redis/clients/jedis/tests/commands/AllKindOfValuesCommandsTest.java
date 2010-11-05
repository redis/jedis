package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.JedisException;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.JedisTest;

public class AllKindOfValuesCommandsTest extends JedisCommandTestBase {
	final byte[] bfoo  = {0x01, 0x02, 0x03, 0x04};
	final byte[] bfoo1 = {0x01, 0x02, 0x03, 0x04, 0x0A};
	final byte[] bfoo2 = {0x01, 0x02, 0x03, 0x04, 0x0B};
	final byte[] bfoo3 = {0x01, 0x02, 0x03, 0x04, 0x0C};
	final byte[] bbar  = {0x05, 0x06, 0x07, 0x08};
	final byte[] bbar1  = {0x05, 0x06, 0x07, 0x08, 0x0A};
	final byte[] bbar2  = {0x05, 0x06, 0x07, 0x08, 0x0B};
	final byte[] bbar3  = {0x05, 0x06, 0x07, 0x08, 0x0C};
	
	final byte[] bfoobar = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

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

	int reply = jedis.exists("foo");
	assertEquals(1, reply);

	reply = jedis.exists(bfoo);
	assertEquals(1, reply);

	reply = jedis.del("foo");
	assertEquals(1, reply);

	reply = jedis.del(bfoo);
	assertEquals(1, reply);

	reply = jedis.exists("foo");
	assertEquals(0, reply);

	reply = jedis.exists(bfoo);
	assertEquals(0, reply);
    }

    @Test
    public void del() {
	jedis.set("foo1", "bar1");
	jedis.set("foo2", "bar2");
	jedis.set("foo3", "bar3");

	int reply = jedis.del("foo1", "foo2", "foo3");
	assertEquals(3, reply);

	reply = jedis.exists("foo1");
	assertEquals(0, reply);
	reply = jedis.exists("foo2");
	assertEquals(0, reply);
	reply = jedis.exists("foo3");
	assertEquals(0, reply);

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

	reply = jedis.exists(bfoo1);
	assertEquals(0, reply);
	reply = jedis.exists(bfoo2);
	assertEquals(0, reply);
	reply = jedis.exists(bfoo3);
	assertEquals(0, reply);

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

	List<String> keys = jedis.keys("foo*");
	List<String> expected = new ArrayList<String>();
	expected.add("foo");
	expected.add("foobar");
	assertEquals(expected, keys);

	expected = new ArrayList<String>();
	keys = jedis.keys("bar*");

	assertEquals(expected, keys);
	
	// Binary
	jedis.set(bfoo, bbar);
	jedis.set(bfoobar, bbar);
	
	byte[] bfoostar = new byte[bfoo.length+1];
	System.arraycopy(bfoo, 0, bfoostar, 0, bfoo.length);
	bfoostar[bfoostar.length-1] = '*';
	List<byte[]> bkeys = jedis.keys(bfoostar);
	List<byte[]> bexpected = new ArrayList<byte[]>();
	bexpected.add(bfoo);
	bexpected.add(bfoobar);
	assertTrue(JedisTest.isListAreEquals(bexpected, bkeys));
//	assertEquals(expected, keys);

	expected = new ArrayList<String>();
	keys = jedis.keys("bar*");

	assertEquals(expected, keys);
	
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
	assertTrue(
			Arrays.equals(randomBkey, bfoo) ||
			Arrays.equals(randomBkey, bbar));

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

	//Binary
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
			fail("JedisException expected");
    	} catch(final JedisException e){}
    	
    	//Binary
    	try {
			jedis.set(bfoo, bbar);
			jedis.rename(bfoo, bfoo);
			fail("JedisException expected");
    	} catch(final JedisException e){}

    }

    @Test
    public void renamenx() {
	jedis.set("foo", "bar");
	int status = jedis.renamenx("foo", "bar");
	assertEquals(1, status);

	jedis.set("foo", "bar");
	status = jedis.renamenx("foo", "bar");
	assertEquals(0, status);
	
	//Binary
	jedis.set(bfoo, bbar);
	int bstatus = jedis.renamenx(bfoo, bbar);
	assertEquals(1, bstatus);

	jedis.set(bfoo, bbar);
	bstatus = jedis.renamenx(bfoo, bbar);
	assertEquals(0, bstatus);

    }

    @Test
    public void dbSize() {
	int size = jedis.dbSize();
	assertEquals(0, size);

	jedis.set("foo", "bar");
	size = jedis.dbSize();
	assertEquals(1, size);
	
	//Binary
	jedis.set(bfoo, bbar);
	size = jedis.dbSize();
	assertEquals(2, size);
    }

    @Test
    public void expire() {
	int status = jedis.expire("foo", 20);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.expire("foo", 20);
	assertEquals(1, status);
	
	// Binary
	int bstatus = jedis.expire(bfoo, 20);
	assertEquals(0, bstatus);

	jedis.set(bfoo, bbar);
	bstatus = jedis.expire(bfoo, 20);
	assertEquals(1, bstatus);

    }

    @Test
    public void expireAt() {
	long unixTime = (System.currentTimeMillis() / 1000L) + 20;

	int status = jedis.expireAt("foo", unixTime);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	unixTime = (System.currentTimeMillis() / 1000L) + 20;
	status = jedis.expireAt("foo", unixTime);
	assertEquals(1, status);
	
	//Binary
	int bstatus = jedis.expireAt(bfoo, unixTime);
	assertEquals(0, bstatus);

	jedis.set(bfoo, bbar);
	unixTime = (System.currentTimeMillis() / 1000L) + 20;
	bstatus = jedis.expireAt(bfoo, unixTime);
	assertEquals(1, bstatus);

    }

    @Test
    public void ttl() {
	int ttl = jedis.ttl("foo");
	assertEquals(-1, ttl);

	jedis.set("foo", "bar");
	ttl = jedis.ttl("foo");
	assertEquals(-1, ttl);

	jedis.expire("foo", 20);
	ttl = jedis.ttl("foo");
	assertTrue(ttl >= 0 && ttl <= 20);

	//Binary
	int bttl = jedis.ttl(bfoo);
	assertEquals(-1, bttl);

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
	//Binary
	jedis.set(bfoo, bbar);
	String bstatus = jedis.select(1);
	assertEquals("OK", bstatus);
	assertEquals(null, jedis.get(bfoo));
	bstatus = jedis.select(0);
	assertEquals("OK", bstatus);
	assertArrayEquals(bbar, jedis.get(bfoo));
    }

    @Test
    public void move() {
	int status = jedis.move("foo", 1);
	assertEquals(0, status);

	jedis.set("foo", "bar");
	status = jedis.move("foo", 1);
	assertEquals(1, status);
	assertEquals(null, jedis.get("foo"));

	jedis.select(1);
	assertEquals("bar", jedis.get("foo"));
	
	//Binary
	jedis.select(0);
	int bstatus = jedis.move(bfoo, 1);
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
	
	//Binary
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
	
	//Binary
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
	int status = jedis.persist("foo");
	assertEquals(1, status);
	assertEquals(-1, jedis.ttl("foo").intValue());
	
	//Binary
	jedis.setex(bfoo, 60 * 60, bbar);
	assertTrue(jedis.ttl(bfoo) > 0);
	int bstatus = jedis.persist(bfoo);
	assertEquals(1, bstatus);
	assertEquals(-1, jedis.ttl(bfoo).intValue());

    }
    

    @Test
    public void echo() {
	String result = jedis.echo("hello world");
	assertEquals("hello world", result);
	
	// Binary
	byte[] bresult = jedis.echo("hello world".getBytes(Protocol.UTF8));
	assertArrayEquals("hello world".getBytes(Protocol.UTF8), bresult);
    }

}