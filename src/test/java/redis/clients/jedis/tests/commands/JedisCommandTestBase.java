package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

public abstract class JedisCommandTestBase extends Assert {
	protected static String host = "localhost";
	protected static int port = Protocol.DEFAULT_PORT;
	static {
		final String envHost = System.getProperty("redis-host");
		final String envPort = System.getProperty("redis-port");
		if (null != envHost && 0 < envHost.length()) {
			host = envHost;
		}
		if (null != envPort && 0 < envPort.length()) {
			try {
				port = Integer.parseInt(envPort);
			} catch (final NumberFormatException e) {
			}
		}

		System.out.println("Redis host to be used : " + host + ":" + port);
	}

	protected Jedis jedis;

	public JedisCommandTestBase() {
		super();
	}

	@Before
	public void setUp() throws Exception {
		jedis = new Jedis(host, port, 500);
		jedis.connect();
		jedis.auth("foobared");
		jedis.flushAll();
	}

	@After
	public void tearDown() throws Exception {
		jedis.disconnect();
	}

	protected Jedis createJedis() throws UnknownHostException, IOException {
		Jedis j = new Jedis(host, port);
		j.connect();
		j.auth("foobared");
		j.flushAll();
		return j;
	}
}