package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersionRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import redis.clients.jedis.HostAndPorts;

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
