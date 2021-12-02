package redis.clients.jedis.commands.unified.pooled;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import redis.clients.jedis.commands.unified.SetCommandsTestBase;

public class PooledSetCommandsTest extends SetCommandsTestBase {

  @BeforeClass
  public static void prepare() throws InterruptedException {
    jedis = PooledCommandsTestHelper.getPooled();
  }

  @AfterClass
  public static void closeCluster() {
    jedis.close();
  }

  @Before
  public void setUp() {
    PooledCommandsTestHelper.clearData();
  }
}
