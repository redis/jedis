package redis.clients.jedis.csc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.SSLJedisTest;

public class SSLJedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

  @BeforeClass
  public static void prepare() {
    SSLJedisTest.setupTrustStore();

    endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");
  }

  @AfterClass
  public static void unprepare() {
    SSLJedisTest.cleanupTrustStore();
  }

}
