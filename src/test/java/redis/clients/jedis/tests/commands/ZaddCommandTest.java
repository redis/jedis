package redis.clients.jedis.tests.commands;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

public class ZaddCommandTest extends JedisCommandTestBase {
	final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
	final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
	final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
	final byte[] ba = { 0x0A };
	final byte[] bb = { 0x0B };
	final byte[] bc = { 0x0C };

	@Before
	@Override
	public void setUp() throws Exception {
		jedis = new Jedis(hnp.host, hnp.port, 500);
		jedis.connect();
		
		jedis.configSet("timeout", "300");
		jedis.flushAll();
	}
	@After
    public void tearDown() {
		
        jedis.disconnect();
    }

	@Test
	public void zadd() {
		long status = jedis.zadd("foo", 1d, "a");
		assertEquals(1, status);

		status = jedis.zadd("foo", 10d, "b");
		assertEquals(1, status);

		status = jedis.zadd("foo", 0.1d, "c");
		assertEquals(1, status);

		status = jedis.zadd("foo", 2d, "a");
		assertEquals(0, status);

		// Binary
		// long bstatus = jedis.zadd(bfoo, 1d, ba);
		// assertEquals(1, bstatus);
		//
		// bstatus = jedis.zadd(bfoo, 10d, bb);
		// assertEquals(1, bstatus);
		//
		// bstatus = jedis.zadd(bfoo, 0.1d, bc);
		// assertEquals(1, bstatus);
		//
		// bstatus = jedis.zadd(bfoo, 2d, ba);
		// assertEquals(0, bstatus);

	}
	
	/**
	 * Test to prove that with the current Jedis interface is impossible to add two different
	 * members with the same score in a single command. This is possible with Redis commands
	 */
	@Test
	public void zaddAll() {
		Map<Double,String> scoreMembers=new HashMap<Double, String>();
		scoreMembers.put(1D, "member_a");
		scoreMembers.put(1D, "member_b");
        jedis.zadd("foo", scoreMembers);	
        assertEquals(1, jedis.zcard("foo").longValue());
        

	}
	
	/**
	 * Test to prove that with the "fixed" Jedis interface is possible to add two different
	 * members with the same score in a single command.
	 */
	@Test
	public void zaddAllFixed() {
		Map<String,Double> scoreMembers=new HashMap<String,Double >();
		scoreMembers.put("member_a",1D );
		scoreMembers.put("member_b",1D );
        jedis.zaddFixed("foo", scoreMembers);	
        assertEquals(2, jedis.zcard("foo").longValue());
        

	}

}