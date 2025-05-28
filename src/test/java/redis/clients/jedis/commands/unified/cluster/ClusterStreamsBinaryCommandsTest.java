package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.StreamsBinaryCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterStreamsBinaryCommandsTest extends StreamsBinaryCommandsTestBase {

  public ClusterStreamsBinaryCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected void setUpTestClient() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    jedis.close();
    ClusterCommandsTestHelper.clearClusterData();
  }

}
