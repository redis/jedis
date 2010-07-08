package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.TransactionBlock;

public class TransactionCommandsTest extends Assert {
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost");
	jedis.connect();
    }

    @After
    public void tearDown() throws Exception {
	jedis.flushDB();
	jedis.disconnect();
    }

    @Test
    public void multi() throws JedisException {
	Transaction trans = jedis.multi();

	String status = trans.sadd("foo", "a");
	assertEquals("QUEUED", status);

	status = trans.sadd("foo", "b");
	assertEquals("QUEUED", status);

	status = trans.scard("foo");
	assertEquals("QUEUED", status);

	List<Object> response = trans.exec();

	List<Object> expected = new ArrayList<Object>();
	expected.add(1);
	expected.add(1);
	expected.add(2);
	assertEquals(expected, response);
    }

    @Test
    public void multiBlock() throws JedisException {
	List<Object> response = jedis.multi(new TransactionBlock() {
	    public void execute() throws JedisException {
		String status = sadd("foo", "a");
		assertEquals("QUEUED", status);

		status = sadd("foo", "b");
		assertEquals("QUEUED", status);

		status = scard("foo");
		assertEquals("QUEUED", status);
	    }
	});

	List<Object> expected = new ArrayList<Object>();
	expected.add(1);
	expected.add(1);
	expected.add(2);
	assertEquals(expected, response);
    }

    @Test
    public void watch() throws JedisException, UnknownHostException,
	    IOException {
	jedis.watch("mykey");
	String val = jedis.get("mykey");
	val = "foo";
	Transaction t = jedis.multi();

	Jedis nj = new Jedis("localhost");
	nj.connect();
	nj.set("mykey", "bar");
	nj.disconnect();

	t.set("mykey", val);
	List<Object> resp = t.exec();
	assertEquals(new ArrayList<Object>(), resp);
    }
}