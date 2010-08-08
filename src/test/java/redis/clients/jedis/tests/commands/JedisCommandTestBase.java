package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

public abstract class JedisCommandTestBase extends Assert {

    protected Jedis jedis;

    public JedisCommandTestBase() {
	super();
    }

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost", Protocol.DEFAULT_PORT, 500);
	jedis.connect();
	jedis.auth("foobared");
	jedis.flushDB();
    }

    @After
    public void tearDown() throws Exception {
	jedis.disconnect();
    }

    protected Jedis createJedis() throws UnknownHostException, IOException {
	Jedis j = new Jedis("localhost");
	j.connect();
	j.auth("foobared");
	j.flushAll();
	return j;
    }
}