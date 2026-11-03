package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.Collection;
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

  private static final String NO_EVICT_TOKEN = "NO-EVICT";

  public BlessCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessSetIsIdempotentAndReportsChange() {
    String key = keys.key("session:alice");
    jedis.set(key, "{\"user\":\"alice\"}");

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(Collections.singletonList(BlessFlag.NO_EVICT), jedis.blessGet(key));
  }

  @Test
  public void blessClearIsIdempotentAndReportsChange() {
    String key = keys.key("session:alice");
    jedis.set(key, "{\"user\":\"alice\"}");
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessGetOnUnblessedKeyReturnsEmptyList() {
    String key = keys.key("cache:page:1");
    jedis.set(key, "html");

    List<BlessFlag> flags = jedis.blessGet(key);
    assertTrue(flags.isEmpty());
  }

  @Test
  public void blessSurvivesValueOverwriteButNotDelete() {
    String key = keys.key("session:alice");
    jedis.set(key, "v1");
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    jedis.set(key, "v2");
    assertEquals(Collections.singletonList(BlessFlag.NO_EVICT), jedis.blessGet(key));

    jedis.del(key);
    jedis.set(key, "v3");
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void missingKeyIsAnError() {
    String key = keys.key("nosuchkey");

    JedisDataException set = assertThrows(JedisDataException.class,
      () -> jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertTrue(set.getMessage().contains("no such key"), set.getMessage());

    JedisDataException clear = assertThrows(JedisDataException.class,
      () -> jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(clear.getMessage().contains("no such key"), clear.getMessage());

    JedisDataException get = assertThrows(JedisDataException.class, () -> jedis.blessGet(key));
    assertTrue(get.getMessage().contains("no such key"), get.getMessage());
  }

  @Test
  public void blessScanIteratesBlessedKeysOnly() {
    Set<String> blessed = new HashSet<>();
    for (String name : new String[] { "session:alice", "session:bob" }) {
      String key = keys.key(name);
      jedis.set(key, "v");
      jedis.blessSet(key, BlessFlag.NO_EVICT);
      blessed.add(key);
    }
    jedis.set(keys.key("cache:page:1"), "html");

    assertEquals(blessed, collectBlessScan(1));
    assertEquals(blessed, collectBlessScanWithoutCount());
  }

  @Test
  public void blessScanReturnsCompleteCursorWhenNothingIsBlessed() {
    jedis.set(keys.key("cache:page:1"), "html");

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
    byte[] key = keys.bKey("session:alice");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    List<byte[]> flags = jedis.blessGet(key);
    assertEquals(1, flags.size());
    assertArrayEquals(SafeEncoder.encode(NO_EVICT_TOKEN), flags.get(0));

    assertEquals(Collections.singleton(SafeEncoder.encode(key)), collectBlessScanBinary());

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessScanIterationCoversEveryBlessedKey() {
    Set<String> blessed = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      String key = keys.key("session:" + i);
      jedis.set(key, "v");
      jedis.blessSet(key, BlessFlag.NO_EVICT);
      blessed.add(key);
    }
    jedis.set(keys.key("cache:page:1"), "html");

    Set<String> scanned = new HashSet<>();
    jedis.blessScanIteration(7, BlessFlag.NO_EVICT).collect(scanned);
    assertEquals(blessed, onlyTestKeys(scanned));
  }

  private Set<String> collectBlessScan(int count) {
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
    return onlyTestKeys(all);
  }

  private Set<String> collectBlessScanWithoutCount() {
    Set<String> all = new HashSet<>();
    String cursor = SCAN_POINTER_START;
    do {
      ScanResult<String> batch = jedis.blessScan(cursor, BlessFlag.NO_EVICT);
      all.addAll(batch.getResult());
      cursor = batch.getCursor();
    } while (!SCAN_POINTER_START.equals(cursor));
    return onlyTestKeys(all);
  }

  /** Binary scan, decoded so the result can be filtered and compared like the String variants. */
  private Set<String> collectBlessScanBinary() {
    Set<String> all = new HashSet<>();
    byte[] cursor = SCAN_POINTER_START_BINARY;
    do {
      ScanResult<byte[]> batch = jedis.blessScan(cursor, BlessFlag.NO_EVICT);
      for (byte[] key : batch.getResult()) {
        all.add(SafeEncoder.encode(key));
      }
      cursor = batch.getCursorAsBytes();
    } while (!SCAN_POINTER_START.equals(SafeEncoder.encode(cursor)));
    return onlyTestKeys(all);
  }

  /**
   * BLESS SCAN walks the whole keyspace, so on a shared endpoint it can surface keys blessed by
   * other tests; only this test's keys take part in the assertions.
   */
  private Set<String> onlyTestKeys(Collection<String> scanned) {
    String prefix = keys.name("%test%:");
    Set<String> own = new HashSet<>();
    for (String key : scanned) {
      if (key.startsWith(prefix)) {
        own.add(key);
      }
    }
    return own;
  }
}
