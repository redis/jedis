package redis.clients.jedis.csc;

import io.redis.test.utils.RedisVersion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import redis.clients.jedis.*;
import redis.clients.jedis.util.RedisVersionUtil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_1)
@ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_3)
public class RedisSentinelClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final String MASTER_NAME = "mymaster";

  protected static HostAndPort sentinel1;
  protected static HostAndPort sentinel2;

  private static Set<HostAndPort> sentinels;

  private static final JedisClientConfig masterClientConfig = DefaultJedisClientConfig.builder()
      .resp3().password("foobared").build();

  private static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder()
      .resp3().build();

  @BeforeAll
  public static void prepareEndpoints() {
    sentinel1 = Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_1).getHostAndPort();
    sentinel2 = Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_3).getHostAndPort();
    sentinels = new HashSet<>(Arrays.asList(sentinel1, sentinel2));
  }

  @Override
  protected RedisSentinelClient createRegularJedis() {
    return RedisSentinelClient.builder().masterName(MASTER_NAME).clientConfig(masterClientConfig)
        .sentinels(sentinels).sentinelClientConfig(sentinelClientConfig).build();
  }

  @Override
  protected RedisSentinelClient createCachedJedis(CacheConfig cacheConfig) {
    return RedisSentinelClient.builder().masterName(MASTER_NAME).clientConfig(masterClientConfig)
        .sentinels(sentinels).sentinelClientConfig(sentinelClientConfig).cacheConfig(cacheConfig)
        .build();
  }

  @BeforeAll
  public static void prepare() {
    try (
        RedisSentinelClient sentinelClient = RedisSentinelClient.builder().masterName(MASTER_NAME)
            .clientConfig(masterClientConfig).sentinels(sentinels)
            .sentinelClientConfig(sentinelClientConfig).build();
        Jedis master = new Jedis(sentinelClient.getCurrentMaster(), masterClientConfig)) {
      assumeTrue(RedisVersionUtil.getRedisVersion(master).isGreaterThanOrEqualTo(RedisVersion.V7_4),
        "Jedis Client side caching is only supported with 'Redis 7.4' or later.");
    }
  }
}
