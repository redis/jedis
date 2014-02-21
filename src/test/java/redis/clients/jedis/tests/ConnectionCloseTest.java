package redis.clients.jedis.tests;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConnectionCloseTest extends Assert {

	private final Closer closer = new Closer();

	private Connection client;

	@Before
	public void setUp() throws Exception {
		client = closer.register(new Connection());
	}

	@After
	public void tearDown() throws Exception {
		closer.close();
	}

	@Test(expected = JedisConnectionException.class)
	public void checkUnkownHost() {
		client.setHost("someunknownhost");
		client.connect();
	}

	@Test(expected = JedisConnectionException.class)
	public void checkWrongPort() {
		client.setHost("localhost");
		client.setPort(55665);
		client.connect();
	}

	@Test
	public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
		client.setHost("localhost");
		client.setPort(6379);
		client.setTimeoutInfinite();
	}
}
