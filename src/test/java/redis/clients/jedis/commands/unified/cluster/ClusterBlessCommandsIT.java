package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.commands.unified.BlessCommandsTestBase;
import redis.clients.jedis.util.SafeEncoder;

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

  /**
   * Single-node BLESS SCAN would only see the node it lands on, so the cluster client refuses it
   * and points to {@code blessScanIteration}.
   */
  @Test
  public void blessScanIsRejectedInClusterMode() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT));
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT, 10));
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT));
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT, 10));
  }

  @Test
  @Override
  public void blessScanIteratesBlessedKeysOnly() {
    blessScanIsRejectedInClusterMode();
  }

  @Test
  @Override
  public void blessScanReturnsCompleteCursorWhenNothingIsBlessed() {
    blessScanIsRejectedInClusterMode();
  }

  @Test
  @Override
  public void blessScanCountHintMustBePositive() {
    blessScanIsRejectedInClusterMode();
  }

  @Test
  @Override
  public void blessBinary() {
    byte[] key = SafeEncoder.encode("session:alice");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(1, jedis.blessGet(key).size());
    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }
}
