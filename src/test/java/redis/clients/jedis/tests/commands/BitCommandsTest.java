package redis.clients.jedis.tests.commands;

import org.junit.Test;

import redis.clients.jedis.BitOP;

public class BitCommandsTest extends JedisCommandTestBase {
    @Test
    public void setAndgetbit() {
	boolean bit = jedis.setbit("foo", 0, true);
	assertEquals(false, bit);

	bit = jedis.getbit("foo", 0);
	assertEquals(true, bit);

	boolean bbit = jedis.setbit("bfoo".getBytes(), 0, "1".getBytes());
	assertFalse(bbit);

	bbit = jedis.getbit("bfoo".getBytes(), 0);
	assertTrue(bbit);
    }

    @Test
    public void setAndgetrange() {
	jedis.set("key1", "Hello World");
	long reply = jedis.setrange("key1", 6, "Jedis");
	assertEquals(11, reply);

	assertEquals(jedis.get("key1"), "Hello Jedis");

	assertEquals("Hello", jedis.getrange("key1", 0, 4));
	assertEquals("Jedis", jedis.getrange("key1", 6, 11));
    }

    @Test
    public void bitCount() {
	jedis.del("foo");

	jedis.setbit("foo", 16, true);
	jedis.setbit("foo", 24, true);
	jedis.setbit("foo", 40, true);
	jedis.setbit("foo", 56, true);

	long c4 = jedis.bitcount("foo");
	assertEquals(4, c4);

	long c3 = jedis.bitcount("foo", 2L, 5L);
	assertEquals(3, c3);

	jedis.del("foo");
    }

    @Test
    public void bitOp() {
	jedis.set("key1", "\u0060");
	jedis.set("key2", "\u0044");

	jedis.bitop(BitOP.AND, "resultAnd", "key1", "key2");
	String resultAnd = jedis.get("resultAnd");
	assertEquals("\u0040", resultAnd);

	jedis.bitop(BitOP.OR, "resultOr", "key1", "key2");
	String resultOr = jedis.get("resultOr");
	assertEquals("\u0064", resultOr);

	jedis.bitop(BitOP.XOR, "resultXor", "key1", "key2");
	String resultXor = jedis.get("resultXor");
	assertEquals("\u0024", resultXor);

	jedis.del("resultAnd");
	jedis.del("resultOr");
	jedis.del("resultXor");
	jedis.del("key1");
	jedis.del("key2");
    }

    @Test
    public void bitOpNot() {
	jedis.del("key");
	jedis.setbit("key", 0, true);
	jedis.setbit("key", 4, true);

	jedis.bitop(BitOP.NOT, "resultNot", "key");

	String resultNot = jedis.get("resultNot");
	assertEquals("\u0077", resultNot);

	jedis.del("key");
	jedis.del("resultNot");
    }
}
