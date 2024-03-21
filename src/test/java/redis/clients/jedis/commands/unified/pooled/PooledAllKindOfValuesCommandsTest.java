package redis.clients.jedis.commands.unified.pooled;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.AllKindOfValuesCommandsTestBase;

@RunWith(Parameterized.class)
public class PooledAllKindOfValuesCommandsTest extends AllKindOfValuesCommandsTestBase {

  public PooledAllKindOfValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = PooledCommandsTestHelper.getPooled(protocol);
    PooledCommandsTestHelper.clearData();
  }

  @After
  public void cleanUp() {
    jedis.close();
  }
}
