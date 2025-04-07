package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.BitCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class PooledBitCommandsTest extends BitCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(PooledCommandsTestHelper.nodeInfo);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(PooledCommandsTestHelper.nodeInfo);

  public PooledBitCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void setUp() {
    jedis = PooledCommandsTestHelper.getPooled(protocol);
    PooledCommandsTestHelper.clearData();
  }

  @AfterEach
  public void cleanUp() {
    jedis.close();
  }
}
