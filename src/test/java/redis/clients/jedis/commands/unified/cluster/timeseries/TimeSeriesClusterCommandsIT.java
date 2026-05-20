package redis.clients.jedis.commands.unified.cluster.timeseries;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.timeseries.TimeSeriesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@SinceRedisVersion(value = "8.0.0", message = "Cluster timeseries tests require Redis 8.0 or higher")
public class TimeSeriesClusterCommandsIT extends TimeSeriesCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  public TimeSeriesClusterCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }

  // The following tests use label-based queries (TS.MGET, TS.MRANGE, TS.MREVRANGE, TS.QUERYINDEX)
  // which query across all keys with matching labels. In cluster mode, these commands only
  // query on a single node, so they are disabled for cluster tests.

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void testAdd() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void testMGet() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void testQueryIndex() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void empty() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void bucketTimestamp() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void latestMulti() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void groupByReduce() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void countNanAndCountAllWithBucketTimestamp() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void mrangeFilterBy() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void testMRevRange() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void countNanAndCountAll() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void mRangeMultipleAggregators() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void mRevRangeMultipleAggregators() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void mRangeMultipleAggregatorsEmptyResult() {
  }

  @Disabled("Label-based queries not compatible with cluster mode")
  @Override
  public void mRevRangeMultipleAggregatorsEmptyResult() {
  }

}
