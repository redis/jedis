package redis.clients.jedis.commands.unified.pooled;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.HashesCommandsTestBase;

@RunWith(Parameterized.class)
public class PooledHashesCommandsTest extends HashesCommandsTestBase {

  public PooledHashesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = PooledCommandsTestHelper.getPooled(protocol);
    PooledCommandsTestHelper.clearData();
    jedis.configSet("hash-max-listpack-entries", "0"); // TODO: remove
  }

  @After
  public void cleanUp() {
    jedis.close();
  }
}
