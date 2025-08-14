package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public ClusterBinaryValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Disabled
  @Override
  public void mget() {
  }

  @Disabled
  @Override
  public void mset() {
  }

  @Disabled
  @Override
  public void msetnx() {
  }
}
