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
import redis.clients.jedis.params.ScanParams;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBlessCommandsIT extends BlessCommandsTestBase {

  public ClusterBlessCommandsIT(RedisProtocol protocol) {
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

  // A BLESS SCAN cursor is only valid on the node that issued it; cluster users go through
  // blessScanIteration, covered by blessScanIterationCoversEveryNode.
  @Test
  @Override
  public void blessScanLoopsUntilCursorZero() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT, 4));
  }

  @Test
  @Override
  public void blessScanWithoutCount() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT));
  }

  @Test
  @Override
  public void blessScanBinary() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT));
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT, 10));
  }

  @Test
  @Override
  public void blessScanInvalidCountIsAnError() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT, 0));
  }
}
