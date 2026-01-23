package redis.clients.jedis.prefix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisSentinelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;

@Tag("integration")
@ResourceLocks({
    @ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_1),
    @ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_3)
})
public class RedisSentinelClientPrefixedKeysTest extends PrefixedKeysTest<RedisSentinelClient> {

  private static final String MASTER_NAME = "mymaster";
  private static final JedisClientConfig MASTER_CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("foobared").build();
  private static Set<HostAndPort> SENTINEL_NODES;
  private static final JedisClientConfig SENTINEL_CLIENT_CONFIG = DefaultJedisClientConfig.builder().build();

  @BeforeAll
  public static void prepareEndpoints() {
    SENTINEL_NODES = new HashSet<>(
        Arrays.asList(
            Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_1).getHostAndPort(),
            Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_3).getHostAndPort()));
  }

  @Override
  RedisSentinelClient nonPrefixingJedis() {
    return RedisSentinelClient.builder()
        .masterName(MASTER_NAME)
        .clientConfig(MASTER_CLIENT_CONFIG)
        .sentinels(SENTINEL_NODES)
        .sentinelClientConfig(SENTINEL_CLIENT_CONFIG)
        .build();
  }
}
