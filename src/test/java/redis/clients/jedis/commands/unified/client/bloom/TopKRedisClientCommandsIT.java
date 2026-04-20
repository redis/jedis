package redis.clients.jedis.commands.unified.client.bloom;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.bloom.TopKCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class TopKRedisClientCommandsIT extends TopKCommandsTestBase {

  public TopKRedisClientCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }
}
