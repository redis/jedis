package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;

public class TransactionCommandsTest extends JedisCommandTestBase {
    final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
    final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
    final byte[] ba = { 0x0A };
    final byte[] bb = { 0x0B };

    final byte[] bmykey = { 0x42, 0x02, 0x03, 0x04 };

    Jedis nj;

    @Before
    public void setUp() throws Exception {
	super.setUp();

	nj = new Jedis(hnp.getHost(), hnp.getPort(), 500);
	nj.connect();
	nj.auth("foobared");
	nj.flushAll();
    }

    @Test
    public void multi() {
	Transaction trans = jedis.multi();

	trans.sadd("foo", "a");
	trans.sadd("foo", "b");
	trans.scard("foo");

	List<Object> response = trans.exec();

	List<Object> expected = new ArrayList<Object>();
	expected.add(1L);
	expected.add(1L);
	expected.add(2L);
	assertEquals(expected, response);

	// Binary
	trans = jedis.multi();

	trans.sadd(bfoo, ba);
	trans.sadd(bfoo, bb);
	trans.scard(bfoo);

	response = trans.exec();

	expected = new ArrayList<Object>();
	expected.add(1L);
	expected.add(1L);
	expected.add(2L);
	assertEquals(expected, response);

    }

    @Test
    public void watch() throws UnknownHostException, IOException {
	jedis.watch("mykey", "somekey");
	Transaction t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set("mykey", "bar");
	nj.disconnect();

	t.set("mykey", "foo");
	List<Object> resp = t.exec();
	assertEquals(null, resp);
	assertEquals("bar", jedis.get("mykey"));

	// Binary
	jedis.watch(bmykey, "foobar".getBytes());
	t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set(bmykey, bbar);
	nj.disconnect();

	t.set(bmykey, bfoo);
	resp = t.exec();
	assertEquals(null, resp);
	assertTrue(Arrays.equals(bbar, jedis.get(bmykey)));
    }

    @Test
    public void unwatch() throws UnknownHostException, IOException {
	jedis.watch("mykey");
	String val = jedis.get("mykey");
	val = "foo";
	String status = jedis.unwatch();
	assertEquals("OK", status);
	Transaction t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set("mykey", "bar");
	nj.disconnect();

	t.set("mykey", val);
	List<Object> resp = t.exec();
	assertEquals(1, resp.size());
	assertEquals("OK", resp.get(0));

	// Binary
	jedis.watch(bmykey);
	byte[] bval = jedis.get(bmykey);
	bval = bfoo;
	status = jedis.unwatch();
	assertEquals(Keyword.OK.name(), status);
	t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set(bmykey, bbar);
	nj.disconnect();

	t.set(bmykey, bval);
	resp = t.exec();
	assertEquals(1, resp.size());
	assertEquals("OK", resp.get(0));
    }

    @Test(expected = JedisDataException.class)
    public void validateWhenInMulti() {
	jedis.multi();
	jedis.ping();
    }

    @Test
    public void discard() {
	Transaction t = jedis.multi();
	String status = t.discard();
	assertEquals("OK", status);
    }

    @Test
    public void transactionResponse() {
	jedis.set("string", "foo");
	jedis.lpush("list", "foo");
	jedis.hset("hash", "foo", "bar");
	jedis.zadd("zset", 1, "foo");
	jedis.sadd("set", "foo");

	Transaction t = jedis.multi();
	Response<String> string = t.get("string");
	Response<String> list = t.lpop("list");
	Response<String> hash = t.hget("hash", "foo");
	Response<Set<String>> zset = t.zrange("zset", 0, -1);
	Response<String> set = t.spop("set");
	t.exec();

	assertEquals("foo", string.get());
	assertEquals("foo", list.get());
	assertEquals("bar", hash.get());
	assertEquals("foo", zset.get().iterator().next());
	assertEquals("foo", set.get());
    }

    @Test
    public void transactionResponseBinary() {
	jedis.set("string", "foo");
	jedis.lpush("list", "foo");
	jedis.hset("hash", "foo", "bar");
	jedis.zadd("zset", 1, "foo");
	jedis.sadd("set", "foo");

	Transaction t = jedis.multi();
	Response<byte[]> string = t.get("string".getBytes());
	Response<byte[]> list = t.lpop("list".getBytes());
	Response<byte[]> hash = t.hget("hash".getBytes(), "foo".getBytes());
	Response<Set<byte[]>> zset = t.zrange("zset".getBytes(), 0, -1);
	Response<byte[]> set = t.spop("set".getBytes());
	t.exec();

	assertArrayEquals("foo".getBytes(), string.get());
	assertArrayEquals("foo".getBytes(), list.get());
	assertArrayEquals("bar".getBytes(), hash.get());
	assertArrayEquals("foo".getBytes(), zset.get().iterator().next());
	assertArrayEquals("foo".getBytes(), set.get());
    }

    @Test(expected = JedisDataException.class)
    public void transactionResponseWithinPipeline() {
	jedis.set("string", "foo");

	Transaction t = jedis.multi();
	Response<String> string = t.get("string");
	string.get();
	t.exec();
    }

    @Test
    public void transactionResponseWithError() {
	Transaction t = jedis.multi();
	t.set("foo", "bar");
	Response<Set<String>> error = t.smembers("foo");
	Response<String> r = t.get("foo");
	List<Object> l = t.exec();
	assertEquals(JedisDataException.class, l.get(1).getClass());
	try {
	    error.get();
	    fail("We expect exception here!");
	} catch (JedisDataException e) {
	    // that is fine we should be here
	}
	assertEquals(r.get(), "bar");
    }

    @Test
    public void execGetResponse() {
	Transaction t = jedis.multi();

	t.set("foo", "bar");
	t.smembers("foo");
	t.get("foo");

	List<Response<?>> lr = t.execGetResponse();
	try {
	    lr.get(1).get();
	    fail("We expect exception here!");
	} catch (JedisDataException e) {
	    // that is fine we should be here
	}
	assertEquals("bar", lr.get(2).get());
    }

    @Test
    public void select() {
	jedis.select(1);
	jedis.set("foo", "bar");
	jedis.watch("foo");
	Transaction t = jedis.multi();
	t.select(0);
	t.set("bar", "foo");

	Jedis jedis2 = createJedis();
	jedis2.select(1);
	jedis2.set("foo", "bar2");

	List<Object> results = t.exec();

	assertNull(results);
    }

    @Test
    public void testResetStateWhenInMulti() {
	jedis.auth("foobared");

	Transaction t = jedis.multi();
	t.set("foooo", "barrr");

	jedis.resetState();
	assertEquals(null, jedis.get("foooo"));
    }

    @Test
    public void testResetStateWhenInMultiWithinPipeline() {
	jedis.auth("foobared");

	Pipeline p = jedis.pipelined();
	p.multi();
	p.set("foooo", "barrr");

	jedis.resetState();
	assertEquals(null, jedis.get("foooo"));
    }

    @Test
    public void testResetStateWhenInWatch() {
	jedis.watch("mykey", "somekey");

	// state reset : unwatch
	jedis.resetState();

	Transaction t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set("mykey", "bar");
	nj.disconnect();

	t.set("mykey", "foo");
	List<Object> resp = t.exec();
	assertNotNull(resp);
	assertEquals(1, resp.size());
	assertEquals("foo", jedis.get("mykey"));
    }
}