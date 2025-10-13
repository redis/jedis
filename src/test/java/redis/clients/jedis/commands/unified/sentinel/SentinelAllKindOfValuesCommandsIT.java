package redis.clients.jedis.commands.unified.sentinel;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentineled;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.AllKindOfValuesCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class SentinelAllKindOfValuesCommandsIT extends AllKindOfValuesCommandsTestBase {

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

  public SentinelAllKindOfValuesCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {

    return JedisSentineled.builder()
        .clientConfig(primary.getClientConfigBuilder().protocol(protocol).build())
        .sentinels(sentinels).sentinelClientConfig(sentinelClientConfig).masterName("mymaster")
        .build();
  }

}
