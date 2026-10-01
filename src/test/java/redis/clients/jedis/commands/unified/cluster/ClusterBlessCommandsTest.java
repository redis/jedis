package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.commands.unified.BlessCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBlessCommandsTest extends BlessCommandsTestBase {

  public ClusterBlessCommandsTest(RedisProtocol protocol) {
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

  // BLESS SCAN is keyless with no hash-tag to pin it to a slot/node, so Jedis cannot route it
  // automatically in cluster mode (see ClusterCommandObjects#blessScan). Users who need full
  // cluster coverage must iterate each master's own connection directly.
  @Override
  @Test
  public void blessScanFindsBlessedKey() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan("0", BlessFlag.NO_EVICT));
  }

  @Override
  @Test
  public void blessScanWithCountHint() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan("0", BlessFlag.NO_EVICT, 10));
  }
}
