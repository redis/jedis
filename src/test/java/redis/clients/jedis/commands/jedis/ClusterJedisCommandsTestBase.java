package redis.clients.jedis.commands.jedis;

import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@Tag("integration")
public abstract class ClusterJedisCommandsTestBase {

  protected static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("cluster-stable");

  protected RedisClusterClient cluster;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());

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
