package redis.clients.jedis.commands.unified.cluster.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.AggregationCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class AggregationClusterCommandsIT extends AggregationCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  public AggregationClusterCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }

  @Override
  public void aggregateIteration() {
    // skip the test because ftAggregateIteration is not compatible with cluster
  }

  @Override
  public void aggregateIterationCollect() {
    // skip the test because ftAggregateIteration is not compatible with cluster
  }

}
