package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.ExtendedVectorSetCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterExtendedVectorSetCommandsTest extends ExtendedVectorSetCommandsTestBase {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-stable");

  public ClusterExtendedVectorSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }
}
