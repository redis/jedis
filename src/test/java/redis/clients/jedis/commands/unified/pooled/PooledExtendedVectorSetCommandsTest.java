package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.ExtendedVectorSetCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class PooledExtendedVectorSetCommandsTest extends ExtendedVectorSetCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      PooledCommandsTestHelper.nodeInfo);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      PooledCommandsTestHelper.nodeInfo);

  public PooledExtendedVectorSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return PooledCommandsTestHelper.getPooled(protocol);
  }

  @Override
  protected void clearData() {
    PooledCommandsTestHelper.clearData();
  }

}
