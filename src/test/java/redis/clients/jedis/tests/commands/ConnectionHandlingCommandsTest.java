package redis.clients.jedis.tests.commands;

import org.junit.Test;

public class ConnectionHandlingCommandsTest extends JedisCommandTestBase {
    @Test
    public void quit() {
	jedis.quit();
    }
}