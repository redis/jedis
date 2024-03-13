package redis.clients.jedis.commands.unified.cluster;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  @Before
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster();
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
