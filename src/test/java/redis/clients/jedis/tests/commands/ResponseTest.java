package redis.clients.jedis.tests.commands;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.tests.HostAndPortUtil;

public class ResponseTest {
	private Jedis connection;
	private static HostAndPortUtil.HostAndPort redis1 = HostAndPortUtil.getRedisServers()
    .get(0);
	
	@Before
	public void initializeTests(){
		connection = new Jedis(redis1.host, redis1.port);
	}
	
	@After
	public void destroyTests(){
		connection.disconnect();
	}
	
	// This test checks the issue: #158
	@Test
	public void testResponseRedisReturnsNull(){
		connection.flushAll();
		Pipeline pipeline = connection.pipelined();
		Response<String> response = pipeline.get("A");
		pipeline.sync();
		Assert.assertNull(response.get());
	}
	
	// This test checks the issue: #158
	@Test
	public void testResponseRedisReturnsNullTransaction(){
		connection.flushAll();
		
		Transaction transaction = connection.multi();
		Response<String> response = transaction.get("A");
		transaction.exec();
		Assert.assertNull(response.get());
	}
}
