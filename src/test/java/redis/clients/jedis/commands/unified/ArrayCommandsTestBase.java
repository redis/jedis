package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ArrayAggregate;
import redis.clients.jedis.args.ArrayBitwise;
import redis.clients.jedis.args.LongRange;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ArgrepParams;
import redis.clients.jedis.resps.ArrayFullInfo;
import redis.clients.jedis.resps.ArrayInfo;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

@SinceRedisVersion("8.7.225")
@Tag("integration")
public abstract class ArrayCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public ArrayCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void arcountMissingKey() {
    long count = jedis.arcount("missing-array");
    assertEquals(0, count);
  }

  @Test
  public void arcountMissingKeyBinary() {
    byte[] bKey = SafeEncoder.encode("missing-array");
    long count = jedis.arcount(bKey);
    assertEquals(0, count);
  }

  @Test
  public void ardel() {
    String key = "ardel";
    jedis.arinsert(key, "a", "b", "c");
    assertEquals(2L, jedis.ardel(key, 0L, 2L));
  }

  @Test
  public void ardelBinary() {
    byte[] key = SafeEncoder.encode("ardel-b");
    jedis.arinsert(key, "a".getBytes());
    assertEquals(1L, jedis.ardel(key, 0L));
  }

  @Test
  public void ardelrange() {
    String key = "ardelrange";
    jedis.arinsert(key, "a", "b", "c", "d");
    long deleted = jedis.ardelrange(key, LongRange.of(0L, 1L));
    assertTrue(deleted >= 0);
  }

  @Test
  public void ardelrangeBinary() {
    byte[] key = SafeEncoder.encode("ardelrange-b");
    jedis.arinsert(key, "a".getBytes(), "b".getBytes());
    long deleted = jedis.ardelrange(key, LongRange.of(0L, 1L));
    assertTrue(deleted >= 0);
  }

  @Test
  public void arget() {
    String key = "arget";
    jedis.arinsert(key, "hello");
    assertEquals("hello", jedis.arget(key, 0L));
    assertNull(jedis.arget(key, 999L));
  }

  @Test
  public void argetBinary() {
    byte[] key = SafeEncoder.encode("arget-b");
    jedis.arinsert(key, "hi".getBytes());
    assertNotNull(jedis.arget(key, 0L));
  }

  @Test
  public void argetrange() {
    String key = "argetrange";
    jedis.arinsert(key, "a", "b", "c");
    List<String> r = jedis.argetrange(key, 0L, 2L);
    assertEquals(3, r.size());
  }

  @Test
  public void argetrangeBinary() {
    byte[] key = SafeEncoder.encode("argetrange-b");
    jedis.arinsert(key, "a".getBytes());
    List<byte[]> r = jedis.argetrange(key, 0L, 0L);
    assertEquals(1, r.size());
  }

  @Test
  public void argrep() {
    String key = "argrep";
    jedis.arinsert(key, "foo", "foobar");
    List<Long> r = jedis.argrep(key, 0L, 10L, ArgrepParams.argrepParams().match("foo"));
    assertNotNull(r);
  }

  @Test
  public void argrepBinary() {
    byte[] key = SafeEncoder.encode("argrep-b");
    jedis.arinsert(key, "abc".getBytes());
    List<Long> r = jedis.argrep(key, 0L, 10L, ArgrepParams.argrepParams().exact("abc"));
    assertNotNull(r);
  }

  @Test
  public void argrepWithValues() {
    String key = "argrepWithValues";
    jedis.arinsert(key, "foo", "foobar");
    List<KeyValue<Long, String>> r = jedis.argrepWithValues(key, 0L, 10L,
        ArgrepParams.argrepParams().match("foo"));
    assertNotNull(r);
  }

  @Test
  public void argrepWithValuesBinary() {
    byte[] key = SafeEncoder.encode("argrepWithValues-b");
    jedis.arinsert(key, "abc".getBytes());
    List<KeyValue<Long, byte[]>> r = jedis.argrepWithValues(key, 0L, 10L,
        ArgrepParams.argrepParams().exact("abc"));
    assertNotNull(r);
  }

  @Test
  public void arinfo() {
    String key = "arinfo";
    jedis.arinsert(key, "a");
    ArrayInfo info = jedis.arinfo(key);
    assertNotNull(info);
  }

  @Test
  public void arinfoFullBinary() {
    byte[] key = SafeEncoder.encode("arinfo-b");
    jedis.arinsert(key, "a".getBytes());
    ArrayFullInfo info = jedis.arinfoFull(key);
    assertNotNull(info);
  }

  @Test
  public void arinfoMissingThrows() {
    assertThrows(JedisDataException.class, () -> jedis.arinfo("arinfo-missing-key"));
  }

  @Test
  public void arinsert() {
    String key = "arinsert";
    assertEquals(2L, jedis.arinsert(key, "a", "b", "c"));
  }

  @Test
  public void arinsertBinary() {
    byte[] key = SafeEncoder.encode("arinsert-b");
    assertEquals(0L, jedis.arinsert(key, "a".getBytes()));
  }

  @Test
  public void arlastitems() {
    String key = "arlastitems";
    jedis.arinsert(key, "a", "b", "c");
    List<String> r = jedis.arlastitems(key, 2L);
    assertEquals(2, r.size());
  }

  @Test
  public void arlastitemsRevBinary() {
    byte[] key = SafeEncoder.encode("arlastitems-b");
    jedis.arinsert(key, "a".getBytes(), "b".getBytes());
    List<byte[]> r = jedis.arlastitems(key, 2L, true);
    assertEquals(2, r.size());
  }

  @Test
  public void arlen() {
    String key = "arlen";
    jedis.arinsert(key, "a", "b");
    assertEquals(2L, jedis.arlen(key));
  }

  @Test
  public void arlenMissingBinary() {
    assertEquals(0L, jedis.arlen(SafeEncoder.encode("arlen-missing-b")));
  }

  @Test
  public void armget() {
    String key = "armget";
    jedis.arinsert(key, "x", "y");
    List<String> r = jedis.armget(key, 0L, 1L, 99L);
    assertEquals("x", r.get(0));
    assertNull(r.get(2));
  }

  @Test
  public void armgetBinary() {
    byte[] key = SafeEncoder.encode("armget-b");
    jedis.arinsert(key, "x".getBytes());
    List<byte[]> r = jedis.armget(key, 0L);
    assertEquals(1, r.size());
  }

  @Test
  public void armset() {
    String key = "armset";
    Map<Long, String> map = new LinkedHashMap<>();
    map.put(0L, "a");
    map.put(3L, "b");
    assertEquals(2L, jedis.armset(key, map));
  }

  @Test
  public void armsetBinary() {
    byte[] key = SafeEncoder.encode("armset-b");
    Map<Long, byte[]> map = new LinkedHashMap<>();
    map.put(0L, "a".getBytes());
    assertEquals(1L, jedis.armset(key, map));
  }

  @Test
  public void arnext() {
    String key = "arnext";
    jedis.arinsert(key, "a", "b");
    assertEquals(OptionalLong.of(2L), jedis.arnext(key));
  }

  @Test
  public void arnextMissingBinary() {
    assertEquals(OptionalLong.empty(), jedis.arnext(SafeEncoder.encode("arnext-missing-b")));
  }

  @Test
  public void aropAggregate() {
    String key = "arop";
    jedis.arinsert(key, "1", "2");
    String r = jedis.aropAggregate(key, LongRange.of(0L, 10L), ArrayAggregate.SUM);
    assertNotNull(r);
  }

  @Test
  public void aropBitwiseBinary() {
    byte[] key = SafeEncoder.encode("arop-b");
    jedis.arinsert(key, "1".getBytes());
    long r = jedis.aropBitwise(key, LongRange.of(0L, 10L), ArrayBitwise.AND);
    assertTrue(r >= 0);
  }

  @Test
  public void aropCount() {
    String key = "aropCount";
    jedis.arinsert(key, "1", "2");
    long r = jedis.aropCount(key, LongRange.of(0L, 10L));
    assertTrue(r >= 0);
  }

  @Test
  public void aropCountMatch() {
    String key = "aropCountMatch";
    jedis.arinsert(key, "a", "a", "b");
    long r = jedis.aropCount(key, LongRange.of(0L, 10L), "a");
    assertTrue(r >= 0);
  }

  @Test
  public void aropCountMatchBinary() {
    byte[] key = SafeEncoder.encode("aropCountMatch-b");
    jedis.arinsert(key, "a".getBytes());
    long r = jedis.aropCount(key, LongRange.of(0L, 10L), "a".getBytes());
    assertTrue(r >= 0);
  }

  @Test
  public void arring() {
    String key = "arring";
    long r = jedis.arring(key, 3L, "x", "y", "z", "w");
    assertTrue(r >= 0);
  }

  @Test
  public void arringBinary() {
    byte[] key = SafeEncoder.encode("arring-b");
    long r = jedis.arring(key, 2L, "x".getBytes());
    assertTrue(r >= 0);
  }

  @Test
  public void arscanMissing() {
    List<KeyValue<Long, String>> r = jedis.arscan("arscan-missing", 0L, 10L);
    assertTrue(r.isEmpty());
  }

  @Test
  public void arscanLimitBinary() {
    byte[] key = SafeEncoder.encode("arscan-b");
    jedis.arinsert(key, "a".getBytes(), "b".getBytes());
    List<KeyValue<Long, byte[]>> r = jedis.arscan(key, 0L, 10L, 1L);
    assertNotNull(r);
  }

  @Test
  public void arseekMissing() {
    assertEquals(0L, jedis.arseek("arseek-missing", 0L));
  }

  @Test
  public void arseekBinary() {
    byte[] key = SafeEncoder.encode("arseek-b");
    jedis.arinsert(key, "a".getBytes());
    assertEquals(1L, jedis.arseek(key, 10L));
  }

  @Test
  public void arset() {
    String key = "arset";
    assertEquals(3L, jedis.arset(key, 0L, "x", "y", "z"));
  }

  @Test
  public void arsetBinary() {
    byte[] key = SafeEncoder.encode("arset-b");
    assertEquals(1L, jedis.arset(key, 0L, "x".getBytes()));
  }
}
