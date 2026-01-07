package redis.clients.jedis.commands.unified.sentinel.search;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.unified.search.FTHybridCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel (RedisSentinelClient) implementation of FT.HYBRID tests.
 * Uses Sentinel to connect to the primary which has RediSearch modules built-in for Redis 8.0+.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class SentinelSearchCommandsTest extends FTHybridCommandsTestBase {

  static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);

  static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  static final Set<HostAndPort> sentinels = new HashSet<>(Arrays.asList(sentinel1, sentinel2));

  static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder().build();

  static final EndpointConfig primary = HostAndPorts.getRedisEndpoint("standalone2-primary");

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      primary.getHostAndPort(), primary.getClientConfigBuilder().build());

  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      primary.getHostAndPort(), primary.getClientConfigBuilder().build());

  public SentinelSearchCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisSentinelClient.builder()
        .clientConfig(primary.getClientConfigBuilder().protocol(protocol).build())
        .sentinels(sentinels)
        .sentinelClientConfig(sentinelClientConfig)
        .masterName("mymaster")
        .build();
  }
}

