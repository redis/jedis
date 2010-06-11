package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Client;

public class ClientTest extends Assert {
    private Client client;

    @Before
    public void setUp() throws Exception {
	client = new Client();
    }

    @After
    public void tearDown() throws Exception {
	client.disconnect();
    }

    @Test
    public void ping() throws UnknownHostException, IOException {
	client.setHost("localhost");
	client.connect();

	assertTrue(client.isConnected());

	String status = client.ping();
	assertEquals("PONG", status);
    }

    @Test
    public void setAndGet() throws UnknownHostException, IOException {
	client.setHost("localhost");
	client.connect();

	String status = client.set("foo", "bar");
	assertEquals("OK", status);

	String value = client.get("foo");
	assertEquals("bar", value);
    }

    @Test(expected = UnknownHostException.class)
    public void checkUnkownHost() throws UnknownHostException, IOException {
	client.setHost("someunknownhost");
	client.connect();
    }

    @Test(expected = IOException.class)
    public void checkWrongPort() throws UnknownHostException, IOException {
	client.setHost("localhost");
	client.setPort(55665);
	client.connect();
    }
}