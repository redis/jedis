package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.ClientCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterClientCommandsTest extends ClientCommandsTestBase {

  public ClusterClientCommandsTest(RedisProtocol protocol) {
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

  // Override tests that are not compatible with cluster mode due to connection-specific behavior
  @Override
  @Test
  @Disabled("CLIENT SETNAME/GETNAME are connection-specific and may not round-trip in cluster mode")
  public void clientSetnameAndGetname() {
    // FIXME: should work in cluster mode
    // Disabled in cluster mode
  }

  @Override
  @Test
  public void clientListByIds() {
    // FIXME: good candidate for node selection API
    // In cluster mode, clientId() and clientList(clientId) may go to different nodes
    // Just verify the methods are callable and return non-null results
    long clientId = jedis.clientId();
    assertTrue(clientId > 0, "Client ID should be positive");

    String list = jedis.clientList(clientId);
    assertNotNull(list);
    // Note: In cluster mode, the list doesn't contain the clientId from another node
  }
}

