package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import redis.clients.jedis.*;
import io.redis.test.utils.RedisVersion;

import static org.junit.Assume.assumeTrue;
import static redis.clients.jedis.util.RedisVersionUtil.getRedisVersion;

public class JedisSentineledClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  private static final Set<HostAndPort> sentinels = new HashSet<>(Arrays.asList(sentinel1, sentinel2));

  private static final JedisClientConfig masterClientConfig = DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  private static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder().resp3().build();

  @Override
  protected JedisSentineled createRegularJedis() {
    return new JedisSentineled(MASTER_NAME, masterClientConfig, sentinels, sentinelClientConfig);
  }

  @Override
  protected JedisSentineled createCachedJedis(CacheConfig cacheConfig) {
    return new JedisSentineled(MASTER_NAME, masterClientConfig, cacheConfig, sentinels, sentinelClientConfig);
  }

    @BeforeClass
    public static <sentinelClient> void prepare() {
        try (JedisSentineled sentinelClient = new JedisSentineled(MASTER_NAME, masterClientConfig, sentinels, sentinelClientConfig);
             Jedis master = new Jedis(sentinelClient.getCurrentMaster(),masterClientConfig)) {
            assumeTrue("Jedis Client side caching is only supported with 'Redis 7.4' or later.",
                    getRedisVersion(master).isGreaterThanOrEqualTo(RedisVersion.V7_4));
        }
    }
}
