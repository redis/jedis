package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import io.redis.test.annotations.SinceRedisVersion;
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

/**
 * Tests related to <a href="https://redis.io/commands/?group=array">Array</a> commands.
 */
@SinceRedisVersion("8.7.225")
public class CommandObjectsArrayCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsArrayCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testArcountMissingKey() {
    Long count = exec(commandObjects.arcount("nonexistent-array"));
    assertThat(count, equalTo(0L));
  }

  @Test
  public void testArcountMissingKeyBinary() {
    Long count = exec(commandObjects.arcount("nonexistent-array".getBytes()));
    assertThat(count, equalTo(0L));
  }

  @Test
  public void testArdel() {
    String key = "ardel-key";
    exec(commandObjects.arinsert(key, "a", "b", "c"));
    Long deleted = exec(commandObjects.ardel(key, 0L, 2L, 99L));
    assertThat(deleted, equalTo(2L));
  }

  @Test
  public void testArdelBinary() {
    byte[] key = "ardel-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes(), "b".getBytes()));
    Long deleted = exec(commandObjects.ardel(key, 0L));
    assertThat(deleted, equalTo(1L));
  }

  @Test
  public void testArdelrange() {
    String key = "ardelrange-key";
    exec(commandObjects.arinsert(key, "a", "b", "c", "d", "e"));
    Long deleted = exec(commandObjects.ardelrange(key, LongRange.of(0L, 1L), LongRange.of(3L, 4L)));
    assertThat(deleted, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testArdelrangeBinary() {
    byte[] key = "ardelrange-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes(), "b".getBytes(), "c".getBytes()));
    Long deleted = exec(commandObjects.ardelrange(key, LongRange.of(0L, 2L)));
    assertThat(deleted, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testArget() {
    String key = "arget-key";
    exec(commandObjects.arinsert(key, "hello", "world"));
    String v = exec(commandObjects.arget(key, 0L));
    assertThat(v, equalTo("hello"));
    String missing = exec(commandObjects.arget(key, 999L));
    assertThat(missing, is(nullValue()));
  }

  @Test
  public void testArgetBinary() {
    byte[] key = "arget-key-b".getBytes();
    exec(commandObjects.arinsert(key, "hello".getBytes()));
    byte[] v = exec(commandObjects.arget(key, 0L));
    assertThat(v, equalTo("hello".getBytes()));
  }

  @Test
  public void testArgetrange() {
    String key = "argetrange-key";
    exec(commandObjects.arinsert(key, "a", "b", "c", "d"));
    List<String> slice = exec(commandObjects.argetrange(key, 0L, 2L));
    assertThat(slice, contains("a", "b", "c"));
  }

  @Test
  public void testArgetrangeBinary() {
    byte[] key = "argetrange-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes(), "b".getBytes()));
    List<byte[]> slice = exec(commandObjects.argetrange(key, 0L, 1L));
    assertThat(slice.size(), equalTo(2));
  }

  @Test
  public void testArgrep() {
    String key = "argrep-key";
    exec(commandObjects.arinsert(key, "foo", "foobar", "baz"));
    List<Long> matches = exec(commandObjects.argrep(key, ArgrepParams.range(0L, 10L).match("foo")));
    assertThat(matches, notNullValue());
  }

  @Test
  public void testArgrepBinary() {
    byte[] key = "argrep-key-b".getBytes();
    exec(commandObjects.arinsert(key, "alpha".getBytes(), "beta".getBytes()));
    List<Long> matches = exec(
      commandObjects.argrep(key, ArgrepParams.range(0L, 10L).exact("alpha")));
    assertThat(matches, notNullValue());
  }

  @Test
  public void testArgrepWithValues() {
    String key = "argrep-wv-key";
    exec(commandObjects.arinsert(key, "foo", "foobar", "baz"));
    List<KeyValue<Long, String>> matches = exec(
      commandObjects.argrepWithValues(key, ArgrepParams.range(0L, 10L).match("foo")));
    assertThat(matches, notNullValue());
  }

  @Test
  public void testArgrepWithValuesBinary() {
    byte[] key = "argrep-wv-key-b".getBytes();
    exec(commandObjects.arinsert(key, "alpha".getBytes(), "beta".getBytes()));
    List<KeyValue<Long, byte[]>> matches = exec(
      commandObjects.argrepWithValues(key, ArgrepParams.range(0L, 10L).exact("alpha")));
    assertThat(matches, notNullValue());
  }

  @Test
  public void testArinfoMissingKeyThrows() {
    assertThrows(JedisDataException.class, () -> exec(commandObjects.arinfo("missing-arinfo-key")));
  }

  @Test
  public void testArinfo() {
    String key = "arinfo-key";
    exec(commandObjects.arinsert(key, "a"));
    ArrayInfo info = exec(commandObjects.arinfo(key));
    assertThat(info, notNullValue());
  }

  @Test
  public void testArinfoFullBinary() {
    byte[] key = "arinfo-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes()));
    ArrayFullInfo info = exec(commandObjects.arinfoFull(key));
    assertThat(info, notNullValue());
  }

  @Test
  public void testArinsert() {
    String key = "arinsert-key";
    Long lastIdx = exec(commandObjects.arinsert(key, "v1", "v2", "v3"));
    assertThat(lastIdx, equalTo(2L));
  }

  @Test
  public void testArinsertBinary() {
    byte[] key = "arinsert-key-b".getBytes();
    Long lastIdx = exec(commandObjects.arinsert(key, "v1".getBytes(), "v2".getBytes()));
    assertThat(lastIdx, equalTo(1L));
  }

  @Test
  public void testArlastitems() {
    String key = "arlastitems-key";
    exec(commandObjects.arinsert(key, "a", "b", "c", "d"));
    List<String> items = exec(commandObjects.arlastitems(key, 2L));
    assertThat(items.size(), equalTo(2));
  }

  @Test
  public void testArlastitemsRevBinary() {
    byte[] key = "arlastitems-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes(), "b".getBytes(), "c".getBytes()));
    List<byte[]> items = exec(commandObjects.arlastitems(key, 2L, true));
    assertThat(items.size(), equalTo(2));
  }

  @Test
  public void testArlen() {
    String key = "arlen-key";
    exec(commandObjects.arinsert(key, "a", "b", "c"));
    Long len = exec(commandObjects.arlen(key));
    assertThat(len, equalTo(3L));
  }

  @Test
  public void testArlenMissingBinary() {
    Long len = exec(commandObjects.arlen("missing-arlen-key-b".getBytes()));
    assertThat(len, equalTo(0L));
  }

  @Test
  public void testArmget() {
    String key = "armget-key";
    exec(commandObjects.arinsert(key, "x", "y", "z"));
    List<String> vs = exec(commandObjects.armget(key, 0L, 1L, 999L));
    assertThat(vs.get(0), equalTo("x"));
    assertThat(vs.get(2), is(nullValue()));
  }

  @Test
  public void testArmgetBinary() {
    byte[] key = "armget-key-b".getBytes();
    exec(commandObjects.arinsert(key, "x".getBytes()));
    List<byte[]> vs = exec(commandObjects.armget(key, 0L));
    assertThat(vs.size(), equalTo(1));
  }

  @Test
  public void testArmset() {
    String key = "armset-key";
    Map<Long, String> map = new LinkedHashMap<>();
    map.put(0L, "a");
    map.put(5L, "b");
    Long created = exec(commandObjects.armset(key, map));
    assertThat(created, equalTo(2L));
  }

  @Test
  public void testArmsetBinary() {
    byte[] key = "armset-key-b".getBytes();
    Map<Long, byte[]> map = new LinkedHashMap<>();
    map.put(0L, "a".getBytes());
    Long created = exec(commandObjects.armset(key, map));
    assertThat(created, equalTo(1L));
  }

  @Test
  public void testArnext() {
    String key = "arnext-key";
    exec(commandObjects.arinsert(key, "a", "b"));
    OptionalLong next = exec(commandObjects.arnext(key));
    assertThat(next, equalTo(OptionalLong.of(2L)));
  }

  @Test
  public void testArnextMissingBinary() {
    OptionalLong next = exec(commandObjects.arnext("arnext-missing-b".getBytes()));
    assertThat(next, equalTo(OptionalLong.of(0L)));
  }

  @Test
  public void testAropBitwise() {
    String key = "arop-bitwise-key";
    exec(commandObjects.arinsert(key, "1", "2", "3"));
    Long r = exec(commandObjects.aropBitwise(key, 0L, 10L, ArrayBitwise.AND));
    assertThat(r, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testAropBitwiseBinary() {
    byte[] key = "arop-bitwise-key-b".getBytes();
    exec(commandObjects.arinsert(key, "1".getBytes(), "2".getBytes()));
    Long r = exec(commandObjects.aropBitwise(key, 0L, 10L, ArrayBitwise.OR));
    assertThat(r, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testAropAggregate() {
    String key = "arop-agg-key";
    exec(commandObjects.arinsert(key, "1", "2", "3"));
    String r = exec(commandObjects.aropAggregate(key, 0L, 10L, ArrayAggregate.SUM));
    assertThat(r, notNullValue());
  }

  @Test
  public void testAropAggregateBinary() {
    byte[] key = "arop-agg-key-b".getBytes();
    exec(commandObjects.arinsert(key, "1".getBytes(), "2".getBytes()));
    byte[] r = exec(commandObjects.aropAggregate(key, 0L, 10L, ArrayAggregate.MIN));
    assertThat(r, notNullValue());
  }

  @Test
  public void testAropCount() {
    String key = "aropcount-key";
    exec(commandObjects.arinsert(key, "a", "a", "b"));
    Long cnt = exec(commandObjects.aropCount(key, 0L, 10L));
    assertThat(cnt, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testAropCountMatch() {
    String key = "aropcount-match-key";
    exec(commandObjects.arinsert(key, "a", "a", "b"));
    Long cnt = exec(commandObjects.aropCount(key, 0L, 10L, "a"));
    assertThat(cnt, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testAropCountMatchBinary() {
    byte[] key = "aropcount-match-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes()));
    Long cnt = exec(commandObjects.aropCount(key, 0L, 10L, "a".getBytes()));
    assertThat(cnt, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testArring() {
    String key = "arring-key";
    Long last = exec(commandObjects.arring(key, 3L, "x", "y", "z", "w"));
    assertThat(last, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testArringBinary() {
    byte[] key = "arring-key-b".getBytes();
    Long last = exec(commandObjects.arring(key, 2L, "x".getBytes()));
    assertThat(last, greaterThanOrEqualTo(0L));
  }

  @Test
  public void testArscanMissing() {
    List<KeyValue<Long, String>> r = exec(commandObjects.arscan("arscan-missing", 0L, 10L));
    assertThat(r, is(empty()));
  }

  @Test
  public void testArscanLimitBinary() {
    byte[] key = "arscan-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes(), "b".getBytes()));
    List<KeyValue<Long, byte[]>> r = exec(commandObjects.arscan(key, 0L, 10L, 1L));
    assertThat(r, notNullValue());
  }

  @Test
  public void testArseekMissing() {
    Long r = exec(commandObjects.arseek("arseek-missing", 0L));
    assertThat(r, equalTo(0L));
  }

  @Test
  public void testArseekBinary() {
    byte[] key = "arseek-key-b".getBytes();
    exec(commandObjects.arinsert(key, "a".getBytes()));
    Long r = exec(commandObjects.arseek(key, 5L));
    assertThat(r, equalTo(1L));
  }

  @Test
  public void testArset() {
    String key = "arset-key";
    Long created = exec(commandObjects.arset(key, 0L, "x", "y", "z"));
    assertThat(created, equalTo(3L));
  }

  @Test
  public void testArsetBinary() {
    byte[] key = "arset-key-b".getBytes();
    Long created = exec(commandObjects.arset(key, 0L, "x".getBytes()));
    assertThat(created, equalTo(1L));
  }

  // Suppress unused import warning when reformatting.
  @SuppressWarnings("unused")
  private static final List<String> SAMPLE = Arrays.asList("a");
}
