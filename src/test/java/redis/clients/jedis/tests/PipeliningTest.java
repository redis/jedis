package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPipeline;
import redis.clients.jedis.Protocol;

public class PipeliningTest extends Assert {
	private static String host = "localhost";
	private static int port = Protocol.DEFAULT_PORT;
	
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
	
	private Jedis jedis;

	@Before
	public void setUp() throws Exception {
		jedis = new Jedis(host, port, 500);
		jedis.connect();
		jedis.auth("foobared");
		jedis.flushAll();
	}

    @Test
    public void pipeline() throws UnknownHostException, IOException {
	List<Object> results = jedis.pipelined(new JedisPipeline() {
	    public void execute() {
		client.set("foo", "bar");
		client.get("foo");
	    }
	});

	assertEquals(2, results.size());
	assertEquals("OK", results.get(0));
	assertEquals("bar", results.get(1));
    }
}
