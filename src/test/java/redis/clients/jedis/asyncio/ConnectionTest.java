package redis.clients.jedis.asyncio;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

public class ConnectionTest extends JedisCommandsTestBase {

    @Test
    public void authAndSet() throws Exception {
        final String strVal = "string-value";
        try (Connection connection = new Connection(hnp.getHost(), hnp.getPort())) {
            connection.executeCommand("AUTH foobared");
            connection.executeCommand("SET foo " + strVal);
            connection.executeCommand("GET foo");
        }

        Assert.assertEquals(strVal, jedis.get("foo"));
    }
}
