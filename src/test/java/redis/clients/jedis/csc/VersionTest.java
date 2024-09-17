package redis.clients.jedis.csc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionTest {

    @Test
    public void compareSameVersions() {
        RedisVersion a = new RedisVersion("5.2.4");
        RedisVersion b = new RedisVersion("5.2.4");
        assertEquals(a, b);

        RedisVersion c = new RedisVersion("5.2.0.0");
        RedisVersion d = new RedisVersion("5.2");
        assertEquals(a, b);
    }

    @Test
    public void compareDifferentVersions() {
        RedisVersion a = new RedisVersion("5.2.4");
        RedisVersion b = new RedisVersion("5.1.4");
        assertEquals(1, a.compareTo(b));

        RedisVersion c = new RedisVersion("5.2.4");
        RedisVersion d = new RedisVersion("5.2.5");
        assertEquals(-1, c.compareTo(d));
    }
}
