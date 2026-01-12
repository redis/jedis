package redis.clients.jedis.commands.jedis;

import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@Tag("integration")
public abstract class ClusterJedisCommandsTestBase {

  protected static EndpointConfig endpoint;

  protected RedisClusterClient cluster;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("cluster-stable"));
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint("cluster-stable"));

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
  }

  @BeforeEach
  public void setUp() {
    cluster = RedisClusterClient.builder()
        .nodes(new HashSet<>(endpoint.getHostsAndPorts()))
        .clientConfig(endpoint.getClientConfigBuilder().build())
        .build();
    cluster.flushAll();
  }

  @AfterEach
  public void tearDown() {
    if (cluster != null) {
      cluster.flushAll();
      cluster.close();
    }
  }
}
