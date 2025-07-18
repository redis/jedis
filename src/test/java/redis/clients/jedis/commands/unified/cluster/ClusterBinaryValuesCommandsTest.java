package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper#testParamsProvider")
public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public ClusterBinaryValuesCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
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
