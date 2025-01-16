package redis.clients.jedis.csc;

import io.redis.test.utils.RedisVersion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assume;
import org.junit.BeforeClass;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentineled;
import redis.clients.jedis.util.RedisVersionUtil;

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
    public static void prepare() {
        try (JedisSentineled sentinelClient = new JedisSentineled(MASTER_NAME, masterClientConfig, sentinels, sentinelClientConfig);
            Jedis master = new Jedis(sentinelClient.getCurrentMaster(),masterClientConfig)) {
            Assume.assumeTrue("Jedis Client side caching is only supported with 'Redis 7.4' or later.",
                    RedisVersionUtil.getRedisVersion(master).isGreaterThanOrEqualTo(RedisVersion.V7_4));
        }
    }
}
