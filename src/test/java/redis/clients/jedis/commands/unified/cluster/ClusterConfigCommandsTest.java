package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.ConfigCommandsTestBase;

/**
 * Exercises {@code CONFIG GET} through {@link redis.clients.jedis.RedisClusterClient} against an
 * OSS Cluster. The fan-out and reply aggregation are driven by the request/response policies
 * registered for {@code CONFIG GET} in the
 * {@link redis.clients.jedis.StaticCommandFlagsRegistryInitializer}.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterConfigCommandsTest extends ConfigCommandsTestBase {

  public ClusterConfigCommandsTest(RedisProtocol protocol) {
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
