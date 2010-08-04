package redis.clients.jedis.tests.commands;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;

public class JedisCommandTestBase extends Assert {

    protected Jedis jedis;

    public JedisCommandTestBase() {
	super();
    }

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost");
	jedis.connect();
	jedis.auth("foobared");
	jedis.flushDB();
    }

    @After
    public void tearDown() throws Exception {
	jedis.disconnect();
    }
}