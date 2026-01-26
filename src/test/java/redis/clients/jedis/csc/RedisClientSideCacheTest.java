package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.util.RedisVersionCondition;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
public class RedisClientSideCacheTest extends RedisClientSideCacheTestBase {

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone0"));

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }
}
