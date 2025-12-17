package redis.clients.jedis.csc;

import io.redis.test.utils.RedisVersion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.*;
import redis.clients.jedis.util.RedisVersionUtil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
public class RedisSentinelClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  private static final Set<HostAndPort> sentinels = new HashSet<>(
      Arrays.asList(sentinel1, sentinel2));

  private static final JedisClientConfig masterClientConfig = DefaultJedisClientConfig.builder()
      .resp3().password("foobared").build();

  private static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder()
      .resp3().build();

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
