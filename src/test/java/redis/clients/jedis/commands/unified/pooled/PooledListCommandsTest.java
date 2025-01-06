package redis.clients.jedis.commands.unified.pooled;

import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.ListCommandsTestBase;

@RunWith(Parameterized.class)
public class PooledListCommandsTest extends ListCommandsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(PooledCommandsTestHelper.nodeInfo);
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(PooledCommandsTestHelper.nodeInfo);

  public PooledListCommandsTest(RedisProtocol protocol) {
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
