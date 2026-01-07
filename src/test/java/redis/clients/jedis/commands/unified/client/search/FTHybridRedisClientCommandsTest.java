package redis.clients.jedis.commands.unified.client.search;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.FTHybridCommandsTestBase;

/**
 * Standalone (RedisClient) implementation of FT.HYBRID tests.
 * Uses standalone0 endpoint which has RediSearch modules built-in for Redis 8.0+.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class FTHybridRedisClientCommandsTest extends FTHybridCommandsTestBase {

  public FTHybridRedisClientCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol);
  }
}

