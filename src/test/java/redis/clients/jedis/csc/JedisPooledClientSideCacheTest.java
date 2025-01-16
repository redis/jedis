package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.util.RedisVersionRule;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
public class JedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

  @ClassRule
  public static RedisVersionRule versionRule = new RedisVersionRule(
          HostAndPorts.getRedisEndpoint("standalone1"));

  @BeforeClass
  public static void prepare() {
    endpoint = HostAndPorts.getRedisEndpoint("standalone1");
  }
}
