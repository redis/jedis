package redis.clients.jedis.commands.unified.cluster.search;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.AggregateIteratorCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@SinceRedisVersion(value = "8.0.0", message = "Cluster aggregate iterator tests require Redis OSS 8.0 or higher")
public class AggregateIteratorClusterCommandsIT extends AggregateIteratorCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  public AggregateIteratorClusterCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }
}
