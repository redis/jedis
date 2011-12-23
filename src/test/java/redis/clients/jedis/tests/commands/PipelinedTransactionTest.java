package redis.clients.jedis.tests.commands;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.tests.HostAndPortUtil;

public class PipelinedTransactionTest {
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
	
	@Test
	public void testPipelinedTransactionSeveralCommands(){
		Pipeline pipeline = connection.pipelined();
		pipeline.multi();
		Response<String> responseSet = pipeline.set("A", "1");
		Response<String> responseGet = pipeline.get("A");
		pipeline.exec();
		pipeline.sync();
		Assert.assertEquals("OK", responseSet.get());
		Assert.assertEquals("1", responseGet.get());
	}
	
	@Test
	public void testPipelinedTransaction1Command(){
		Pipeline pipeline = connection.pipelined();
		pipeline.multi();
		Response<String> responseSet = pipeline.set("A", "1");
		pipeline.exec();
		pipeline.sync();
		Assert.assertEquals("OK", responseSet.get());
	}
}
