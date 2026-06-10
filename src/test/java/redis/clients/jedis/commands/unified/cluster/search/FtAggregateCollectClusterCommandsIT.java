package redis.clients.jedis.commands.unified.cluster.search;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.FtAggregateCollectCommandsTestBase;
import redis.clients.jedis.util.TestEnvUtil;

/**
 * OSS Cluster integration test for FT.AGGREGATE COLLECT, mirroring
 * {@code FtAggregateCollectRedisClientCommandsIT} but against the {@code cluster-stable} endpoint
 * via {@link redis.clients.jedis.RedisClusterClient}.
 * <p>
 * Cluster-mode COLLECT corresponds to Deliverable 2 of the spec — coordinator merging across
 * shards. Tests that depend on later deliverables (FIELDS *, @__key) self-skip when the running
 * Search build hasn't shipped them yet.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public class FtAggregateCollectClusterCommandsIT extends FtAggregateCollectCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  public FtAggregateCollectClusterCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }
}
