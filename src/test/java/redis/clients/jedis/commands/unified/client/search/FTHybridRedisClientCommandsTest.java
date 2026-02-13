package redis.clients.jedis.commands.unified.client.search;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.FTHybridCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class FTHybridRedisClientCommandsTest extends FTHybridCommandsTestBase {

  public FTHybridRedisClientCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }
}
