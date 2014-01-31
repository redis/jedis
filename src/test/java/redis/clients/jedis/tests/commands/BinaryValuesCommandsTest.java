package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.exceptions.JedisDataException;

public class BinaryValuesCommandsTest extends JedisCommandTestBase {
    byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
    byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
    byte[] bxx = { 0x78, 0x78 };
    byte[] bnx = { 0x6E, 0x78 };
    byte[] bex = { 0x65, 0x78 };
    byte[] bpx = { 0x70, 0x78 };
    long expireSeconds = 2;
    long expireMillis = expireSeconds * 1000;
    byte[] binaryValue;

    @Before
    public void startUp() {
	StringBuilder sb = new StringBuilder();

	for (int n = 0; n < 1000; n++) {
	    sb.append("A");
	}

	binaryValue = sb.toString().getBytes();
    }

    @Test
    public void setAndGet() {
	String status = jedis.set(bfoo, binaryValue);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));

	byte[] value = jedis.get(bfoo);
	assertTrue(Arrays.equals(binaryValue, value));

	assertNull(jedis.get(bbar));
    }

    @Test
    public void setNxExAndGet() {
	String status = jedis.set(bfoo, binaryValue, bnx, bex, expireSeconds);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
	byte[] value = jedis.get(bfoo);
	assertTrue(Arrays.equals(binaryValue, value));

	assertNull(jedis.get(bbar));
    }

    @Test
    public void setIfNotExistAndGet() {
	String status = jedis.set(bfoo, binaryValue);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
	// nx should fail if value exists
	String statusFail = jedis.set(bfoo, binaryValue, bnx, bex,
		expireSeconds);
	assertNull(statusFail);

	byte[] value = jedis.get(bfoo);
	assertTrue(Arrays.equals(binaryValue, value));

	assertNull(jedis.get(bbar));
    }

    @Test
    public void setIfExistAndGet() {
	String status = jedis.set(bfoo, binaryValue);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
	// nx should fail if value exists
	String statusSuccess = jedis.set(bfoo, binaryValue, bxx, bex,
		expireSeconds);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(statusSuccess));

	byte[] value = jedis.get(bfoo);
	assertTrue(Arrays.equals(binaryValue, value));

	assertNull(jedis.get(bbar));
    }

    @Test
    public void setFailIfNotExistAndGet() {
	// xx should fail if value does NOT exists
	String statusFail = jedis.set(bfoo, binaryValue, bxx, bex,
		expireSeconds);
	assertNull(statusFail);
    }

    @Test
    public void setAndExpireMillis() {
	String status = jedis.set(bfoo, binaryValue, bnx, bpx, expireMillis);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
	long ttl = jedis.ttl(bfoo);
	assertTrue(ttl > 0 && ttl <= expireSeconds);
    }

    @Test
    public void setAndExpire() {
	String status = jedis.set(bfoo, binaryValue, bnx, bex, expireSeconds);
	assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
	long ttl = jedis.ttl(bfoo);
	assertTrue(ttl > 0 && ttl <= expireSeconds);
    }

    @Test
    public void getSet() {
	byte[] value = jedis.getSet(bfoo, binaryValue);
	assertNull(value);
	value = jedis.get(bfoo);
	assertTrue(Arrays.equals(binaryValue, value));
    }

    @Test
    public void mget() {
	List<byte[]> values = jedis.mget(bfoo, bbar);
	List<byte[]> expected = new ArrayList<byte[]>();
	expected.add(null);
	expected.add(null);

	assertEquals(expected, values);

	jedis.set(bfoo, binaryValue);

	expected = new ArrayList<byte[]>();
	expected.add(binaryValue);
	expected.add(null);
	values = jedis.mget(bfoo, bbar);

	assertEquals(expected, values);

	jedis.set(bbar, bfoo);

	expected = new ArrayList<byte[]>();
	expected.add(binaryValue);
	expected.add(bfoo);
	values = jedis.mget(bfoo, bbar);

	assertEquals(expected, values);
    }

    @Test
    public void setnx() {
	long status = jedis.setnx(bfoo, binaryValue);
	assertEquals(1, status);
	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));

	status = jedis.setnx(bfoo, bbar);
	assertEquals(0, status);
	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
    }

    @Test
    public void setex() {
	String status = jedis.setex(bfoo, 20, binaryValue);
	assertEquals(Keyword.OK.name(), status);
	long ttl = jedis.ttl(bfoo);
	assertTrue(ttl > 0 && ttl <= 20);
    }

    @Test
    public void mset() {
	String status = jedis.mset(bfoo, binaryValue, bbar, bfoo);
	assertEquals(Keyword.OK.name(), status);
	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
	assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));
    }

    @Test
    public void msetnx() {
	long status = jedis.msetnx(bfoo, binaryValue, bbar, bfoo);
	assertEquals(1, status);
	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
	assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));

	status = jedis.msetnx(bfoo, bbar, "bar2".getBytes(), "foo2".getBytes());
	assertEquals(0, status);
	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
	assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));
    }

    @Test(expected = JedisDataException.class)
    public void incrWrongValue() {
	jedis.set(bfoo, binaryValue);
	jedis.incr(bfoo);
    }

    @Test
    public void incr() {
	long value = jedis.incr(bfoo);
	assertEquals(1, value);
	value = jedis.incr(bfoo);
	assertEquals(2, value);
    }

    @Test(expected = JedisDataException.class)
    public void incrByWrongValue() {
	jedis.set(bfoo, binaryValue);
	jedis.incrBy(bfoo, 2);
    }

    @Test
    public void incrBy() {
	long value = jedis.incrBy(bfoo, 2);
	assertEquals(2, value);
	value = jedis.incrBy(bfoo, 2);
	assertEquals(4, value);
    }

    @Test(expected = JedisDataException.class)
    public void decrWrongValue() {
	jedis.set(bfoo, binaryValue);
	jedis.decr(bfoo);
    }

    @Test
    public void decr() {
	long value = jedis.decr(bfoo);
	assertEquals(-1, value);
	value = jedis.decr(bfoo);
	assertEquals(-2, value);
    }

    @Test(expected = JedisDataException.class)
    public void decrByWrongValue() {
	jedis.set(bfoo, binaryValue);
	jedis.decrBy(bfoo, 2);
    }

    @Test
    public void decrBy() {
	long value = jedis.decrBy(bfoo, 2);
	assertEquals(-2, value);
	value = jedis.decrBy(bfoo, 2);
	assertEquals(-4, value);
    }

    @Test
    public void append() {
	byte[] first512 = new byte[512];
	System.arraycopy(binaryValue, 0, first512, 0, 512);
	long value = jedis.append(bfoo, first512);
	assertEquals(512, value);
	assertTrue(Arrays.equals(first512, jedis.get(bfoo)));

	byte[] rest = new byte[binaryValue.length - 512];
	System.arraycopy(binaryValue, 512, rest, 0, binaryValue.length - 512);
	value = jedis.append(bfoo, rest);
	assertEquals(binaryValue.length, value);

	assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
    }

    @Test
    public void substr() {
	jedis.set(bfoo, binaryValue);

	byte[] first512 = new byte[512];
	System.arraycopy(binaryValue, 0, first512, 0, 512);
	byte[] rfirst512 = jedis.substr(bfoo, 0, 511);
	assertTrue(Arrays.equals(first512, rfirst512));

	byte[] last512 = new byte[512];
	System.arraycopy(binaryValue, binaryValue.length - 512, last512, 0, 512);
	assertTrue(Arrays.equals(last512, jedis.substr(bfoo, -512, -1)));

	assertTrue(Arrays.equals(binaryValue, jedis.substr(bfoo, 0, -1)));

	assertTrue(Arrays.equals(last512,
		jedis.substr(bfoo, binaryValue.length - 512, 100000)));
    }

    @Test
    public void strlen() {
	jedis.set(bfoo, binaryValue);
	assertEquals(binaryValue.length, jedis.strlen(bfoo).intValue());
    }
}