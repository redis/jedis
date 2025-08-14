package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class PooledBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public PooledBinaryValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return PooledCommandsTestHelper.getPooled(protocol);
  }

  @BeforeEach
  public void setUp() {
    PooledCommandsTestHelper.clearData();
  }

}
