package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class UDSTest {
    protected static File udsHost = HostAndPortUtil.getUDSServers().get(0);
    @Test
    public void testCompareTo() {
        Jedis jedis = new Jedis(udsHost);
        assertEquals("PONG", jedis.ping());
        jedis.close();
    }
}
