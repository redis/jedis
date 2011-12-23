package redis.clients.jedis.tests.unix;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import redis.clients.jedis.unix.UnixDomainJedis;

public class UnixDomainJedisTest {

	@Test
	public void testUnixDomainSocket() throws IOException{
		UnixDomainJedis connection = new UnixDomainJedis("/var/run/redis/redis1.sock");
		connection.flushAll();
		connection.set("UNIX", "HI!");
		Assert.assertEquals("HI!", connection.get("UNIX"));
	}
	
}
