package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.StreamsBinaryCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper#testParamsProvider")
public class ClusterStreamsBinaryCommandsTest extends StreamsBinaryCommandsTestBase {

  public ClusterStreamsBinaryCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    super(protocol, clientType);
  }

  @Override
  protected void setUpTestClient() {
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
