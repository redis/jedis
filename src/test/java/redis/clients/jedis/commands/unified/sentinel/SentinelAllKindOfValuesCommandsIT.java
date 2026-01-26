package redis.clients.jedis.commands.unified.sentinel;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.unified.AllKindOfValuesCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class SentinelAllKindOfValuesCommandsIT extends AllKindOfValuesCommandsTestBase {

  static HostAndPort sentinel1;

  static HostAndPort sentinel2;

  static Set<HostAndPort> sentinels;

  static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder().build();

  static EndpointConfig primary;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone2-primary"));

  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint("standalone2-primary"));

  @BeforeAll
  public static void prepareEndpoints() {
    sentinel1 = Endpoints.getRedisEndpoint("sentinel-standalone2-1").getHostAndPort();
    sentinel2 = Endpoints.getRedisEndpoint("sentinel-standalone2-3").getHostAndPort();
    sentinels = new HashSet<>(Arrays.asList(sentinel1, sentinel2));
    primary = Endpoints.getRedisEndpoint("standalone2-primary");
  }

  public SentinelAllKindOfValuesCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {

    return RedisSentinelClient.builder()
        .clientConfig(primary.getClientConfigBuilder().protocol(protocol).build())
        .sentinels(sentinels).sentinelClientConfig(sentinelClientConfig).masterName("mymaster")
        .build();
  }

}
