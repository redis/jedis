package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public abstract class JedisCommandTestBase extends Assert {
	protected static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0); 

	protected Jedis jedis;

	public JedisCommandTestBase() {
		super();
	}

	@Before
	public void setUp() throws Exception {
		jedis = new Jedis(hnp.host, hnp.port, 500);
		jedis.connect();
		jedis.auth("foobared");
		jedis.flushAll();
	}

	@After
	public void tearDown() throws Exception {
		jedis.disconnect();
	}

	protected Jedis createJedis() throws UnknownHostException, IOException {
		Jedis j = new Jedis(hnp.host, hnp.port);
		j.connect();
		j.auth("foobared");
		j.flushAll();
		return j;
	}
}