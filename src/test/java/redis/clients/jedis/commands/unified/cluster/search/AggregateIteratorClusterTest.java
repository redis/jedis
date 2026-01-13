package redis.clients.jedis.commands.unified.cluster.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.AggregateIteratorBaseTest;

/**
 * Cluster (RedisClusterClient) implementation of AggregateIterator tests. Uses cluster-stack
 * endpoint which has RediSearch modules built-in for Redis 8.0+. Requires Redis 8.0+ because
 * earlier versions don't have modules in cluster mode.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class AggregateIteratorClusterTest extends AggregateIteratorBaseTest {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stack");
  }

  public AggregateIteratorClusterTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }
}
