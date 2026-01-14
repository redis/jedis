package redis.clients.jedis.commands.unified.client.search;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.AggregateIteratorBaseTest;

/**
 * Standalone (RedisClient) implementation of AggregateIterator tests. Uses modules-docker endpoint
 * which has RediSearch modules built-in.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class AggregateIteratorRedisClientTest extends AggregateIteratorBaseTest {

  public AggregateIteratorRedisClientTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }
}
