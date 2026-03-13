package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.ConfigCommandsTestBase;
import redis.clients.jedis.exceptions.JedisBroadcastException;
import redis.clients.jedis.util.SafeEncoder;

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

  // Override binary config tests - they throw JedisBroadcastException in cluster mode
  // due to differences in how binary responses are merged across nodes
  @Override
  @Test
  public void configGetBinarySinglePattern() {
    // FIXME: This should work in cluster mode
    assertThrows(JedisBroadcastException.class,
      () -> jedis.configGet(SafeEncoder.encode("maxmemory")));
  }

  @Override
  @Test
  public void configGetBinaryMultiplePatterns() {
    // FIXME: This should work in cluster mode
    assertThrows(JedisBroadcastException.class,
      () -> jedis.configGet(SafeEncoder.encode("maxmemory"), SafeEncoder.encode("timeout")));
  }
}
