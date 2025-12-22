package redis.clients.jedis.prefix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisSentinelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

@Tag("integration")
@ResourceLock("sentinel")
public class RedisSentinelClientPrefixedKeysTest extends PrefixedKeysTest<RedisSentinelClient> {

  private static final String MASTER_NAME = "mymaster";
  private static final JedisClientConfig MASTER_CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("foobared").build();
  private static final Set<HostAndPort> SENTINEL_NODES = new HashSet<>(
      Arrays.asList(HostAndPorts.getSentinelServers().get(1), HostAndPorts.getSentinelServers().get(3)));
  private static final JedisClientConfig SENTINEL_CLIENT_CONFIG = DefaultJedisClientConfig.builder().build();

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
