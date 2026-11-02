package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.commands.unified.BlessCommandsTestBase;
import redis.clients.jedis.util.SafeEncoder;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBlessCommandsIT extends BlessCommandsTestBase {

  private static final String BLESS_SCAN_REJECTED = "BLESS SCAN is rejected in cluster mode";

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
  public void blessScanIsRejectedInClusterPipeline() {
    try (AbstractPipeline pipeline = jedis.pipelined()) {
      assertThrows(UnsupportedOperationException.class,
        () -> pipeline.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT));
      assertThrows(UnsupportedOperationException.class,
        () -> pipeline.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT, 10));
      assertThrows(UnsupportedOperationException.class,
        () -> pipeline.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT));
      assertThrows(UnsupportedOperationException.class,
        () -> pipeline.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT, 10));
    }
  }

  @Test
  @Disabled(BLESS_SCAN_REJECTED)
  @Override
  public void blessScanIteratesBlessedKeysOnly() {
  }

  @Test
  @Disabled(BLESS_SCAN_REJECTED)
  @Override
  public void blessScanReturnsCompleteCursorWhenNothingIsBlessed() {
  }

  @Test
  @Disabled(BLESS_SCAN_REJECTED)
  @Override
  public void blessScanCountHintMustBePositive() {
  }

  @Test
  @Override
  public void blessBinary() {
    byte[] key = keys.bKey("session:alice");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(1, jedis.blessGet(key).size());
    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }
}
