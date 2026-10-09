package redis.clients.jedis.commands.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.BlessScanIteration;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;

@EnabledOnCommand("BLESS")
public abstract class BlessCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String NO_EVICT_TOKEN = "NO-EVICT";

  public BlessCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessSetGetClearLifecycle() {
    // a session cache entry that must survive maxmemory eviction
    jedis.set("session:42", "{\"user\":7}");

    assertThat(jedis.blessGet("session:42"), empty());
    assertEquals(1L, jedis.blessSet("session:42", BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessSet("session:42", BlessFlag.NO_EVICT));
    assertThat(jedis.blessGet("session:42"), contains(NO_EVICT_TOKEN));

    assertEquals(1L, jedis.blessClear("session:42", BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessClear("session:42", BlessFlag.NO_EVICT));
    assertThat(jedis.blessGet("session:42"), empty());
  }

  @Test
  public void blessSetGetClearLifecycleBinary() {
    byte[] key = SafeEncoder.encode("session:bin");
    jedis.set(key, new byte[] { 1, 2, 3 });

    assertTrue(jedis.blessGet(key).isEmpty());
    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessSet(key, BlessFlag.NO_EVICT));

    List<byte[]> flags = jedis.blessGet(key);
    assertEquals(1, flags.size());
    assertArrayEquals(SafeEncoder.encode(NO_EVICT_TOKEN), flags.get(0));

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertEquals(0L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void missingKeyIsAnError() {
    JedisDataException setError = assertThrows(JedisDataException.class,
      () -> jedis.blessSet("missing", BlessFlag.NO_EVICT));
    assertThat(setError.getMessage(), containsString("no such key"));

    JedisDataException clearError = assertThrows(JedisDataException.class,
      () -> jedis.blessClear("missing", BlessFlag.NO_EVICT));
    assertThat(clearError.getMessage(), containsString("no such key"));

    JedisDataException getError = assertThrows(JedisDataException.class,
      () -> jedis.blessGet("missing"));
    assertThat(getError.getMessage(), containsString("no such key"));
  }

  @Test
  public void missingKeyIsAnErrorBinary() {
    byte[] key = SafeEncoder.encode("missing-bin");
    assertThrows(JedisDataException.class, () -> jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertThrows(JedisDataException.class, () -> jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertThrows(JedisDataException.class, () -> jedis.blessGet(key));
  }

  @Test
  public void blessingSurvivesOverwriteButNotDeletion() {
    jedis.set("config:flags", "v1");
    jedis.blessSet("config:flags", BlessFlag.NO_EVICT);

    // overwriting the value, even with another type, keeps the protection
    jedis.set("config:flags", "v2");
    assertThat(jedis.blessGet("config:flags"), contains(NO_EVICT_TOKEN));
    jedis.del("config:flags");
    jedis.hset("config:flags", "f", "v");
    assertThat(jedis.blessGet("config:flags"), empty());
  }

  @Test
  public void blessScanIterationCoversEveryNode() {
    Set<String> blessed = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      String key = "hot:" + i;
      jedis.set(key, "v" + i);
      if (i % 2 == 0) {
        jedis.blessSet(key, BlessFlag.NO_EVICT);
        blessed.add(key);
      }
    }

    BlessScanIteration iteration = jedis.blessScanIteration(5, BlessFlag.NO_EVICT);
    // SCAN semantics allow duplicates, so collect into a set
    Set<String> scanned = (Set<String>) iteration.collect(new HashSet<>());
    assertEquals(blessed, scanned);
    assertTrue(iteration.isIterationCompleted());
  }

  @Test
  public void blessScanLoopsUntilCursorZero() {
    Set<String> blessed = new HashSet<>();
    for (int i = 0; i < 30; i++) {
      String key = "pinned:" + i;
      jedis.set(key, "v");
      jedis.blessSet(key, BlessFlag.NO_EVICT);
      blessed.add(key);
    }
    jedis.set("unpinned", "v");

    Set<String> scanned = new HashSet<>();
    String cursor = ScanParams.SCAN_POINTER_START;
    do {
      ScanResult<String> page = jedis.blessScan(cursor, BlessFlag.NO_EVICT, 4);
      scanned.addAll(page.getResult());
      cursor = page.getCursor();
    } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

    assertEquals(blessed, scanned);
  }

  @Test
  public void blessScanWithoutCount() {
    jedis.set("pinned", "v");
    jedis.blessSet("pinned", BlessFlag.NO_EVICT);
    jedis.set("unpinned", "v");

    ScanResult<String> page = jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT);
    assertTrue(page.isCompleteIteration());
    assertThat(page.getResult(), contains("pinned"));
  }

  @Test
  public void blessScanBinary() {
    byte[] key = new byte[] { 'p', 0, (byte) 0xff };
    jedis.set(key, SafeEncoder.encode("v"));
    jedis.blessSet(key, BlessFlag.NO_EVICT);

    List<byte[]> scanned = new ArrayList<>();
    byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
    do {
      ScanResult<byte[]> page = jedis.blessScan(cursor, BlessFlag.NO_EVICT, 10);
      scanned.addAll(page.getResult());
      cursor = page.getCursorAsBytes();
    } while (!ScanParams.SCAN_POINTER_START.equals(SafeEncoder.encode(cursor)));

    assertEquals(1, scanned.size());
    assertArrayEquals(key, scanned.get(0));

    ScanResult<byte[]> single = jedis.blessScan(ScanParams.SCAN_POINTER_START_BINARY,
      BlessFlag.NO_EVICT);
    assertTrue(single.isCompleteIteration());
    assertEquals(1, single.getResult().size());
  }

  @Test
  public void blessScanInvalidCountIsAnError() {
    assertThrows(JedisDataException.class,
      () -> jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT, 0));
  }

  @Test
  public void pipelinedBlessCommands() {
    jedis.set("pipe:1", "v");
    jedis.set(SafeEncoder.encode("pipe:2"), SafeEncoder.encode("v"));

    try (AbstractPipeline pipeline = jedis.pipelined()) {
      Response<Long> set = pipeline.blessSet("pipe:1", BlessFlag.NO_EVICT);
      Response<List<String>> get = pipeline.blessGet("pipe:1");
      Response<Long> setBinary = pipeline.blessSet(SafeEncoder.encode("pipe:2"),
        BlessFlag.NO_EVICT);
      Response<List<byte[]>> getBinary = pipeline.blessGet(SafeEncoder.encode("pipe:2"));
      Response<Long> clear = pipeline.blessClear("pipe:1", BlessFlag.NO_EVICT);
      Response<Long> clearBinary = pipeline.blessClear(SafeEncoder.encode("pipe:2"),
        BlessFlag.NO_EVICT);
      pipeline.sync();

      assertEquals(1L, set.get());
      assertThat(get.get(), contains(NO_EVICT_TOKEN));
      assertEquals(1L, setBinary.get());
      assertArrayEquals(SafeEncoder.encode(NO_EVICT_TOKEN), getBinary.get().get(0));
      assertEquals(1L, clear.get());
      assertEquals(1L, clearBinary.get());
    }
  }
}
