package redis.clients.jedis.commands.unified.client.json;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.json.RedisJsonV2CommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class RedisJsonV2RedisClientCommandsIT extends RedisJsonV2CommandsTestBase {

  public RedisJsonV2RedisClientCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }
}
