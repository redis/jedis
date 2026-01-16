package redis.clients.jedis.commands.unified.cluster.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.JedisBroadcastAndRoundRobinConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.FTHybridCommandsTestBase;

/**
 * Cluster (RedisClusterClient) implementation of FT.HYBRID tests. Uses cluster-stable endpoint
 * which has RediSearch modules built-in for Redis 8.0+. Requires Redis 8.0+ because earlier
 * versions don't have modules in cluster mode.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class FTHybridCommandsClusterTest extends FTHybridCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stack");
  }

  public FTHybridCommandsClusterTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    UnifiedJedis cluster = ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
    cluster.setBroadcastAndRoundRobinConfig(
      () -> JedisBroadcastAndRoundRobinConfig.RediSearchMode.LIGHT);
    return cluster;
  }
}
