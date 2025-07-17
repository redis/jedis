package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.HashesCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper#testParamsProvider")
public class ClusterHashesCommandsTest extends HashesCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster") .build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

  public ClusterHashesCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    super(protocol, clientType);
  }

  @BeforeEach
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol, clientType);
  }

  @AfterEach
  public void tearDown() {
    try {
      jedis.close();
    } catch (Exception e) {
      logger.warn("Exception while closing jedis", e);
    }
    ClusterCommandsTestHelper.clearClusterData();
  }
}
