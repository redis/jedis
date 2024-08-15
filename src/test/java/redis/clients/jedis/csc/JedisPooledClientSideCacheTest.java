package redis.clients.jedis.csc;

import org.junit.BeforeClass;
import redis.clients.jedis.HostAndPorts;

public class JedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

  @BeforeClass
  public static void prepare() {
    endpoint = HostAndPorts.getRedisEndpoint("standalone1");
  }

}
