package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;

/**
 * BLESS command family: protecting keys from maxmemory eviction. Gated on command presence because
 * the feature is not in any GA server yet (targets Redis 8.12).
 */
@EnabledOnCommand("BLESS")
public abstract class BlessCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String NO_EVICT_TOKEN = "NO-EVICT";

  public BlessCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessSetIsIdempotentAndReportsChange() {
    jedis.set("session:alice", "{\"user\":\"alice\"}");

    assertEquals(1L, jedis.blessSet("session:alice", BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessSet("session:alice", BlessFlag.NO_EVICT));
    assertEquals(Collections.singletonList(NO_EVICT_TOKEN), jedis.blessGet("session:alice"));
  }

  @Test
  public void blessClearIsIdempotentAndReportsChange() {
    jedis.set("session:alice", "{\"user\":\"alice\"}");
    jedis.blessSet("session:alice", BlessFlag.NO_EVICT);

    assertEquals(1L, jedis.blessClear("session:alice", BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessClear("session:alice", BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet("session:alice").isEmpty());
  }

  @Test
  public void blessGetOnUnblessedKeyReturnsEmptyList() {
    jedis.set("cache:page:1", "html");

    List<String> flags = jedis.blessGet("cache:page:1");
    assertTrue(flags.isEmpty());
  }

  @Test
  public void blessSurvivesValueOverwriteButNotDelete() {
    jedis.set("session:alice", "v1");
    jedis.blessSet("session:alice", BlessFlag.NO_EVICT);

    jedis.set("session:alice", "v2");
    assertEquals(Collections.singletonList(NO_EVICT_TOKEN), jedis.blessGet("session:alice"));

    jedis.del("session:alice");
    jedis.set("session:alice", "v3");
    assertTrue(jedis.blessGet("session:alice").isEmpty());
  }

  @Test
  public void missingKeyIsAnError() {
    JedisDataException set = assertThrows(JedisDataException.class,
      () -> jedis.blessSet("nosuchkey", BlessFlag.NO_EVICT));
    assertTrue(set.getMessage().contains("no such key"), set.getMessage());

    JedisDataException clear = assertThrows(JedisDataException.class,
      () -> jedis.blessClear("nosuchkey", BlessFlag.NO_EVICT));
    assertTrue(clear.getMessage().contains("no such key"), clear.getMessage());

    JedisDataException get = assertThrows(JedisDataException.class,
      () -> jedis.blessGet("nosuchkey"));
    assertTrue(get.getMessage().contains("no such key"), get.getMessage());
  }

  @Test
  public void blessScanIteratesBlessedKeysOnly() {
    Set<String> blessed = new HashSet<>(Arrays.asList("session:alice", "session:bob"));
    for (String key : blessed) {
      jedis.set(key, "v");
      jedis.blessSet(key, BlessFlag.NO_EVICT);
    }
    jedis.set("cache:page:1", "html");

    assertEquals(blessed, collectBlessScan(1));
    assertEquals(blessed, collectBlessScanWithoutCount());
  }

  @Test
  public void blessScanReturnsCompleteCursorWhenNothingIsBlessed() {
    jedis.set("cache:page:1", "html");

    ScanResult<String> result = jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT);
    assertTrue(result.isCompleteIteration());
    assertTrue(result.getResult().isEmpty());
  }

  @Test
  public void blessScanCountHintMustBePositive() {
    JedisDataException e = assertThrows(JedisDataException.class,
      () -> jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT, 0));
    assertTrue(e.getMessage().contains("out of range"), e.getMessage());
  }

  @Test
  public void blessBinary() {
    byte[] key = SafeEncoder.encode("session:alice");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    List<byte[]> flags = jedis.blessGet(key);
    assertEquals(1, flags.size());
    assertArrayEquals(SafeEncoder.encode(NO_EVICT_TOKEN), flags.get(0));

    ScanResult<byte[]> scan = jedis.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT);
    assertTrue(scan.isCompleteIteration());
    assertEquals(1, scan.getResult().size());
    assertArrayEquals(key, scan.getResult().get(0));

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessScanIterationCoversEveryBlessedKey() {
    Set<String> blessed = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      String key = "session:" + i;
      jedis.set(key, "v");
      jedis.blessSet(key, BlessFlag.NO_EVICT);
      blessed.add(key);
    }
    jedis.set("cache:page:1", "html");

    Set<String> scanned = new HashSet<>();
    jedis.blessScanIteration(7, BlessFlag.NO_EVICT).collect(scanned);
    assertEquals(blessed, scanned);
  }

  protected Set<String> collectBlessScan(int count) {
    Set<String> all = new HashSet<>();
    String cursor = SCAN_POINTER_START;
    int batches = 0;
    do {
      ScanResult<String> batch = jedis.blessScan(cursor, BlessFlag.NO_EVICT, count);
      all.addAll(batch.getResult());
      cursor = batch.getCursor();
      batches++;
    } while (!SCAN_POINTER_START.equals(cursor));
    // COUNT 1 with two blessed keys must take more than one round trip
    assertFalse(batches < 2, "expected a multi-batch iteration, got " + batches);
    return all;
  }

  protected Set<String> collectBlessScanWithoutCount() {
    Set<String> all = new HashSet<>();
    String cursor = SCAN_POINTER_START;
    do {
      ScanResult<String> batch = jedis.blessScan(cursor, BlessFlag.NO_EVICT);
      all.addAll(batch.getResult());
      cursor = batch.getCursor();
    } while (!SCAN_POINTER_START.equals(cursor));
    return all;
  }
}
