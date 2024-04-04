package redis.clients.jedis.commands.unified.cluster;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@RunWith(Parameterized.class)
public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public ClusterBinaryValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @After
  public void tearDown() {
    jedis.close();
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Ignore
  @Override
  public void mget() {
  }

  @Ignore
  @Override
  public void mset() {
  }

  @Ignore
  @Override
  public void msetnx() {
  }
}
