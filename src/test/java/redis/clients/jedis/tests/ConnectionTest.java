package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;

public class ConnectionTest extends Assert {
    private Connection client;

    @Before
    public void setUp() throws Exception {
	client = new Connection();
    }

    @After
    public void tearDown() throws Exception {
	client.disconnect();
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