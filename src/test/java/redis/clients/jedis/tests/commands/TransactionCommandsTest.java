package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.TransactionBlock;
import redis.clients.jedis.tests.JedisTest;

public class TransactionCommandsTest extends JedisCommandTestBase {
	Jedis nj;
	@Before
	public void setUp() throws Exception {
		super.setUp();

		nj = new Jedis(hnp.host, hnp.port, 500);
		nj.connect();
		nj.auth("foobared");
		nj.flushAll();
	}

    @Test
    public void multi() {
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
    public void multiBlock() {
	List<Object> response = jedis.multi(new TransactionBlock() {
	    public void execute() {
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
    public void watch() throws UnknownHostException, IOException {
	jedis.watch("mykey");
	Transaction t = jedis.multi();

	nj.connect();
	nj.auth("foobared");
	nj.set("mykey", "bar");
	nj.disconnect();

	t.set("mykey", "foo");
	List<Object> resp = t.exec();
	assertEquals(null, resp);
	assertEquals("bar", jedis.get("mykey"));
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
	List<Object> expected = new ArrayList<Object>();
	expected.add("OK".getBytes(Protocol.UTF8));
	JedisTest.isListAreEquals(expected, resp);
//	assertEquals(expected, resp);
    }

    @Test(expected = JedisException.class)
    public void validateWhenInMulti() {
	jedis.multi();
	jedis.ping();
    }
}