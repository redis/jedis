package redis.clients.jedis.commands.unified.cluster.search;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.ClusterCommandArguments;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.JsonSearchTestBase;
import redis.clients.jedis.json.JsonProtocol;
import redis.clients.jedis.json.Path2;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class JsonSearchClusterTest extends JsonSearchTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stack");
  }

  public JsonSearchClusterTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }

  @Override
  protected void setJson(String key, JSONObject json) {
    CommandObject command = new CommandObject<>(
        new ClusterCommandArguments(JsonProtocol.JsonCommand.SET).key(key).add(Path2.ROOT_PATH)
            .add(json),
        BuilderFactory.STRING);
    jedis.executeCommand(command);
  }
}
