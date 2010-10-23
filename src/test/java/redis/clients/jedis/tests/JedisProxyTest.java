package redis.clients.jedis.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisProxy;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class JedisProxyTest {

    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    private static JedisPool JEDIS_POOL;
    private JedisCommands proxiedJedis;

    @BeforeClass
    public static void setupPool() {
	JedisPool thePool = new JedisPool(hnp.host, hnp.port);
	thePool.init();
	JEDIS_POOL = thePool;
    }

    @Before
    public void setup() {
	this.proxiedJedis = JedisProxy.newInstance(JEDIS_POOL);
    }

    @Test
    public void aJedisProxyInstanceCanSetAndGetAStringValue() {
	String testString = "testvalue";
	String testKey = "testkey";
	proxiedJedis.set(testKey, testString);
	assertTrue(testString.equals(proxiedJedis.get(testKey)));
    }
}
