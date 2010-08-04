package redis.clients.jedis.tests.commands;

import org.junit.Test;

import redis.clients.jedis.JedisException;

public class ConnectionHandlingCommandsTest extends JedisCommandTestBase {
    @Test
    public void quit() throws JedisException {
	jedis.quit();
    }
}