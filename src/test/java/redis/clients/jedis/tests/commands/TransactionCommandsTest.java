package redis.clients.jedis.tests.commands;

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

	trans.exec();
	
	//TODO: check for exec response
    }

    @Test
    public void multiBlock() throws JedisException {
	jedis.multi(new TransactionBlock() {
	    public void execute() throws JedisException {
		String status = sadd("foo", "a");
		assertEquals("QUEUED", status);

		status = sadd("foo", "b");
		assertEquals("QUEUED", status);

		status = scard("foo");
		assertEquals("QUEUED", status);
	    }
	});
	
	//TODO: check what happens when throwind an exception
    }
}