package redis.clients.jedis.commands.unified.cluster.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.SearchWithParamsCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class SearchWithParamsClusterCommandsIT extends SearchWithParamsCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  public SearchWithParamsClusterCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }
}
