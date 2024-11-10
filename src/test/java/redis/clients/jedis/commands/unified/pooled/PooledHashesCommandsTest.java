package redis.clients.jedis.commands.unified.pooled;

import io.redis.test.utils.EnabledOnCommandRule;
import io.redis.test.utils.RedisVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.HashesCommandsTestBase;

@RunWith(Parameterized.class)
public class PooledHashesCommandsTest extends HashesCommandsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(
          PooledCommandsTestHelper.nodeInfo.getHostAndPort(),
          PooledCommandsTestHelper.nodeInfo.getClientConfigBuilder().build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(
          PooledCommandsTestHelper.nodeInfo.getHostAndPort(),
          PooledCommandsTestHelper.nodeInfo.getClientConfigBuilder().build());

  public PooledHashesCommandsTest(RedisProtocol protocol) {
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
