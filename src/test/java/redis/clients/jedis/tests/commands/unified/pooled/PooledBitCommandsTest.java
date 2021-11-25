package redis.clients.jedis.tests.commands.unified.pooled;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import redis.clients.jedis.tests.commands.unified.BitCommandsTestBase;

public class PooledBitCommandsTest extends BitCommandsTestBase {

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
