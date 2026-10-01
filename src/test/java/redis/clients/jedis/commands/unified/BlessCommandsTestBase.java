package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Covers the BLESS (avoid eviction) command family: {@code BLESS SET}, {@code BLESS CLEAR},
 * {@code BLESS GET}, {@code BLESS SCAN}. Scenarios mirror the showcase transcripts gathered from
 * the HLD/server PR: a session key is protected from eviction, queried, found via a scan, then
 * unprotected again.
 */
@Tag("integration")
@EnabledOnCommand("BLESS")
public abstract class BlessCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public BlessCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessSetAndClearLifecycle() {
    String key = "session:user:42";
    jedis.set(key, "sessiondata");

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    // Already on: second call is a no-op and reports 0.
    assertEquals(0L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(Collections.singletonList("NO-EVICT"), jedis.blessGet(key));

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    // Already off: second call is a no-op and reports 0.
    assertEquals(0L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessSetAndClearLifecycleBinary() {
    byte[] key = SafeEncoder.encode("session:user:43");
    jedis.set(key, SafeEncoder.encode("sessiondata"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(1, jedis.blessGet(key).size());

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessGetOnMissingKeyThrows() {
    assertThrows(JedisDataException.class, () -> jedis.blessGet("bless-missing-key"));
  }

  @Test
  public void blessSetOnMissingKeyThrows() {
    assertThrows(JedisDataException.class,
      () -> jedis.blessSet("bless-missing-key-set", BlessFlag.NO_EVICT));
  }

  @Test
  public void blessClearOnMissingKeyThrows() {
    assertThrows(JedisDataException.class,
      () -> jedis.blessClear("bless-missing-key-clear", BlessFlag.NO_EVICT));
  }

  @Test
  public void blessScanFindsBlessedKey() {
    String key = "bless-scan-target";
    jedis.set(key, "v");
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    List<String> found = new ArrayList<>();
    String cursor = "0";
    do {
      ScanResult<String> result = jedis.blessScan(cursor, BlessFlag.NO_EVICT);
      found.addAll(result.getResult());
      cursor = result.getCursor();
    } while (!"0".equals(cursor));

    assertTrue(found.contains(key));
  }

  @Test
  public void blessScanWithCountHint() {
    String key = "bless-scan-count-target";
    jedis.set(key, "v");
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    List<String> found = new ArrayList<>();
    String cursor = "0";
    do {
      ScanResult<String> result = jedis.blessScan(cursor, BlessFlag.NO_EVICT, 10);
      found.addAll(result.getResult());
      cursor = result.getCursor();
    } while (!"0".equals(cursor));

    assertTrue(found.contains(key));
  }

  @Test
  public void infoStatsReportsBlessedKeysField() {
    String key = "bless-info-target";
    jedis.set(key, "v");
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    String info = jedis.info("stats");
    assertTrue(info.contains("blessed_keys:"));
  }
}
