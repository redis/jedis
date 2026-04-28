package redis.clients.jedis.commands.unified.client.json;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.json.RedisJsonV1CommandsTestBase;

/**
 * V1 of the RedisJSON is only supported with RESP2, hence this test is not parameterized.
 */
public class RedisJsonV1RedisClientCommandsIT extends RedisJsonV1CommandsTestBase {

  public RedisJsonV1RedisClientCommandsIT() {
    super(RedisProtocol.RESP2);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }
}
