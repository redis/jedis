package redis.clients.jedis.commands.unified.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class RedisClientBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public RedisClientBinaryValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol);
  }

  @BeforeEach
  public void setUp() {
    RedisClientCommandsTestHelper.clearData();
  }

}
