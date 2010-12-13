package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.JedisTestBase;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public abstract class JedisCommandTestBase extends JedisTestBase {
    protected static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    protected Jedis jedis;

    public JedisCommandTestBase() {
        super();
    }

    @Before
    public void setUp() throws Exception {
        jedis = new Jedis(hnp.host, hnp.port, 500);
        jedis.connect();
        jedis.auth("foobared");
        jedis.configSet("timeout", "300");
        jedis.flushAll();
    }

    @After
    public void tearDown() throws Exception {
        jedis.disconnect();
    }

    protected Jedis createJedis() throws UnknownHostException, IOException {
        Jedis j = new Jedis(hnp.host, hnp.port);
        j.connect();
        j.auth("foobared");
        j.flushAll();
        return j;
    }

    protected void assertEquals(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int n = 0; n < expected.size(); n++) {
            assertArrayEquals(expected.get(n), actual.get(n));
        }
    }

    protected void assertEquals(Set<byte[]> expected, Set<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        Iterator<byte[]> iterator = expected.iterator();
        Iterator<byte[]> iterator2 = actual.iterator();
        while (iterator.hasNext() || iterator2.hasNext()) {
            assertArrayEquals(iterator.next(), iterator2.next());
        }
    }

    protected boolean arrayContains(List<byte[]> array, byte[] expected) {
        return collectionContains(array, expected);
    }

    protected boolean setContains(Set<byte[]> set, byte[] expected) {
        return collectionContains(set, expected);
    }

    protected boolean collectionContains(Collection<byte[]> set, byte[] expected) {
        for (byte[] a : set) {
            try {
                assertArrayEquals(a, expected);
                return true;
            } catch (AssertionError e) {

            }
        }
        return false;
    }
}
