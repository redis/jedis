package redis.clients.jedis.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionTest {

    @Test
    public void compareSameVersions() {
        Version a = new Version("5.2.4");
        Version b = new Version("5.2.4");
        assertEquals(a, b);

        Version c = new Version("5.2.0.0");
        Version d = new Version("5.2");
        assertEquals(a, b);
    }

    @Test
    public void compareDifferentVersions() {
        Version a = new Version("5.2.4");
        Version b = new Version("5.1.4");
        assertEquals(1, a.compareTo(b));

        Version c = new Version("5.2.4");
        Version d = new Version("5.2.5");
        assertEquals(-1, c.compareTo(d));
    }
}
