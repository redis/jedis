package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.args.ArrayAggregate;
import redis.clients.jedis.args.ArrayBitwise;
import redis.clients.jedis.args.LongRange;
import redis.clients.jedis.params.ArgrepParams;

public class UnifiedJedisArrayCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testArcount() {
    String key = "array-key";
    long expectedCount = 7L;

    when(commandObjects.arcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.arcount(key);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arcount(key);
  }

  @Test
  public void testArcountBinary() {
    byte[] key = "array-key".getBytes();
    long expectedCount = 7L;

    when(commandObjects.arcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.arcount(key);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arcount(key);
  }

  @Test
  public void testArdel() {
    String key = "k";
    long[] idx = { 0L, 1L };
    when(commandObjects.ardel(key, idx)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.ardel(key, idx), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ardel(key, idx);
  }

  @Test
  public void testArdelBinary() {
    byte[] key = "k".getBytes();
    long[] idx = { 0L };
    when(commandObjects.ardel(key, idx)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.ardel(key, idx), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ardel(key, idx);
  }

  @Test
  public void testArdelrange() {
    String key = "k";
    LongRange[] ranges = { LongRange.of(0L, 1L) };
    when(commandObjects.ardelrange(key, ranges)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.ardelrange(key, ranges), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ardelrange(key, ranges);
  }

  @Test
  public void testArdelrangeBinary() {
    byte[] key = "k".getBytes();
    LongRange[] ranges = { LongRange.of(0L, 1L) };
    when(commandObjects.ardelrange(key, ranges)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.ardelrange(key, ranges), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ardelrange(key, ranges);
  }

  @Test
  public void testArget() {
    String key = "k";
    when(commandObjects.arget(key, 0L)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("v");

    assertThat(jedis.arget(key, 0L), equalTo("v"));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).arget(key, 0L);
  }

  @Test
  public void testArgetBinary() {
    byte[] key = "k".getBytes();
    byte[] v = "v".getBytes();
    when(commandObjects.arget(key, 0L)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(v);

    assertThat(jedis.arget(key, 0L), equalTo(v));
    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).arget(key, 0L);
  }

  @Test
  public void testArgetrange() {
    String key = "k";
    when(commandObjects.argetrange(key, 0L, 1L)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject))
        .thenReturn(java.util.Arrays.asList("a", "b"));

    assertThat(jedis.argetrange(key, 0L, 1L).size(), equalTo(2));
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).argetrange(key, 0L, 1L);
  }

  @Test
  public void testArgetrangeBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.argetrange(key, 0L, 1L)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject))
        .thenReturn(java.util.Arrays.asList("a".getBytes(), "b".getBytes()));

    assertThat(jedis.argetrange(key, 0L, 1L).size(), equalTo(2));
    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).argetrange(key, 0L, 1L);
  }

  @Test
  public void testArgrep() {
    String key = "k";
    ArgrepParams params = ArgrepParams.range(0L, 10L).match("foo");
    when(commandObjects.argrep(key, params)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.argrep(key, params);
    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).argrep(key, params);
  }

  @Test
  public void testArgrepBinary() {
    byte[] key = "k".getBytes();
    ArgrepParams params = ArgrepParams.range(0L, 10L).exact("foo");
    when(commandObjects.argrep(key, params)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.argrep(key, params);
    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).argrep(key, params);
  }

  @Test
  public void testArgrepWithValues() {
    String key = "k";
    ArgrepParams params = ArgrepParams.range(0L, 10L).match("foo");
    when(commandObjects.argrepWithValues(key, params))
        .thenReturn(listKeyValueLongStringCommandObject);
    when(commandExecutor.executeCommand(listKeyValueLongStringCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.argrepWithValues(key, params);
    verify(commandExecutor).executeCommand(listKeyValueLongStringCommandObject);
    verify(commandObjects).argrepWithValues(key, params);
  }

  @Test
  public void testArgrepWithValuesBinary() {
    byte[] key = "k".getBytes();
    ArgrepParams params = ArgrepParams.range(0L, 10L).exact("foo");
    when(commandObjects.argrepWithValues(key, params))
        .thenReturn(listKeyValueLongBytesCommandObject);
    when(commandExecutor.executeCommand(listKeyValueLongBytesCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.argrepWithValues(key, params);
    verify(commandExecutor).executeCommand(listKeyValueLongBytesCommandObject);
    verify(commandObjects).argrepWithValues(key, params);
  }

  @Test
  public void testArinfo() {
    String key = "k";
    when(commandObjects.arinfo(key)).thenReturn(arrayInfoCommandObject);

    jedis.arinfo(key);
    verify(commandExecutor).executeCommand(arrayInfoCommandObject);
    verify(commandObjects).arinfo(key);
  }

  @Test
  public void testArinfoBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arinfo(key)).thenReturn(arrayInfoCommandObject);

    jedis.arinfo(key);
    verify(commandExecutor).executeCommand(arrayInfoCommandObject);
    verify(commandObjects).arinfo(key);
  }

  @Test
  public void testArinfoFull() {
    String key = "k";
    when(commandObjects.arinfoFull(key)).thenReturn(arrayFullInfoCommandObject);

    jedis.arinfoFull(key);
    verify(commandExecutor).executeCommand(arrayFullInfoCommandObject);
    verify(commandObjects).arinfoFull(key);
  }

  @Test
  public void testArinfoFullBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arinfoFull(key)).thenReturn(arrayFullInfoCommandObject);

    jedis.arinfoFull(key);
    verify(commandExecutor).executeCommand(arrayFullInfoCommandObject);
    verify(commandObjects).arinfoFull(key);
  }

  @Test
  public void testArinsert() {
    String key = "k";
    String[] vals = { "a", "b" };
    when(commandObjects.arinsert(key, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.arinsert(key, vals), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arinsert(key, vals);
  }

  @Test
  public void testArinsertBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "a".getBytes() };
    when(commandObjects.arinsert(key, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(0L);

    assertThat(jedis.arinsert(key, vals), equalTo(0L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arinsert(key, vals);
  }

  @Test
  public void testArlastitems() {
    String key = "k";
    when(commandObjects.arlastitems(key, 2L)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject))
        .thenReturn(java.util.Arrays.asList("a", "b"));

    jedis.arlastitems(key, 2L);
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).arlastitems(key, 2L);
  }

  @Test
  public void testArlastitemsRevBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arlastitems(key, 2L, true)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject))
        .thenReturn(java.util.Arrays.asList("a".getBytes()));

    jedis.arlastitems(key, 2L, true);
    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).arlastitems(key, 2L, true);
  }

  @Test
  public void testArlen() {
    String key = "k";
    when(commandObjects.arlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    assertThat(jedis.arlen(key), equalTo(3L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arlen(key);
  }

  @Test
  public void testArlenBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    assertThat(jedis.arlen(key), equalTo(3L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arlen(key);
  }

  @Test
  public void testArmget() {
    String key = "k";
    long[] idx = { 0L, 1L };
    when(commandObjects.armget(key, idx)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject))
        .thenReturn(java.util.Arrays.asList("a", "b"));

    jedis.armget(key, idx);
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).armget(key, idx);
  }

  @Test
  public void testArmgetBinary() {
    byte[] key = "k".getBytes();
    long[] idx = { 0L };
    when(commandObjects.armget(key, idx)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject))
        .thenReturn(java.util.Arrays.asList("a".getBytes()));

    jedis.armget(key, idx);
    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).armget(key, idx);
  }

  @Test
  public void testArmset() {
    String key = "k";
    Map<Long, String> map = new LinkedHashMap<>();
    map.put(0L, "a");
    when(commandObjects.armset(key, map)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.armset(key, map), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).armset(key, map);
  }

  @Test
  public void testArmsetBinary() {
    byte[] key = "k".getBytes();
    Map<Long, byte[]> map = new LinkedHashMap<>();
    map.put(0L, "a".getBytes());
    when(commandObjects.armset(key, map)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.armset(key, map), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).armset(key, map);
  }

  @Test
  public void testArnext() {
    String key = "k";
    when(commandObjects.arnext(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(5L);

    assertThat(jedis.arnext(key), equalTo(5L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arnext(key);
  }

  @Test
  public void testArnextBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arnext(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(5L);

    assertThat(jedis.arnext(key), equalTo(5L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arnext(key);
  }

  @Test
  public void testAropBitwise() {
    String key = "k";
    when(commandObjects.aropBitwise(key, 0L, 10L, ArrayBitwise.AND)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    assertThat(jedis.aropBitwise(key, 0L, 10L, ArrayBitwise.AND), equalTo(3L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropBitwise(key, 0L, 10L, ArrayBitwise.AND);
  }

  @Test
  public void testAropBitwiseBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.aropBitwise(key, 0L, 10L, ArrayBitwise.OR)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(7L);

    assertThat(jedis.aropBitwise(key, 0L, 10L, ArrayBitwise.OR), equalTo(7L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropBitwise(key, 0L, 10L, ArrayBitwise.OR);
  }

  @Test
  public void testAropAggregate() {
    String key = "k";
    when(commandObjects.aropAggregate(key, 0L, 10L, ArrayAggregate.SUM))
        .thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("42");

    assertThat(jedis.aropAggregate(key, 0L, 10L, ArrayAggregate.SUM), equalTo("42"));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).aropAggregate(key, 0L, 10L, ArrayAggregate.SUM);
  }

  @Test
  public void testAropAggregateBinary() {
    byte[] key = "k".getBytes();
    byte[] result = "42".getBytes();
    when(commandObjects.aropAggregate(key, 0L, 10L, ArrayAggregate.MIN))
        .thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(result);

    assertThat(jedis.aropAggregate(key, 0L, 10L, ArrayAggregate.MIN), equalTo(result));
    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).aropAggregate(key, 0L, 10L, ArrayAggregate.MIN);
  }

  @Test
  public void testAropCount() {
    String key = "k";
    when(commandObjects.aropCount(key, 0L, 10L)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.aropCount(key, 0L, 10L), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropCount(key, 0L, 10L);
  }

  @Test
  public void testAropCountBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.aropCount(key, 0L, 10L)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.aropCount(key, 0L, 10L), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropCount(key, 0L, 10L);
  }

  @Test
  public void testAropCountMatch() {
    String key = "k";
    when(commandObjects.aropCount(key, 0L, 10L, "v")).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.aropCount(key, 0L, 10L, "v"), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropCount(key, 0L, 10L, "v");
  }

  @Test
  public void testAropCountMatchBinary() {
    byte[] key = "k".getBytes();
    byte[] val = "v".getBytes();
    when(commandObjects.aropCount(key, 0L, 10L, val)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.aropCount(key, 0L, 10L, val), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).aropCount(key, 0L, 10L, val);
  }

  @Test
  public void testArring() {
    String key = "k";
    String[] vals = { "x", "y" };
    when(commandObjects.arring(key, 3L, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.arring(key, 3L, vals), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arring(key, 3L, vals);
  }

  @Test
  public void testArringBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "x".getBytes() };
    when(commandObjects.arring(key, 2L, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(0L);

    assertThat(jedis.arring(key, 2L, vals), equalTo(0L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arring(key, 2L, vals);
  }

  @Test
  public void testArscan() {
    String key = "k";
    when(commandObjects.arscan(key, 0L, 10L)).thenReturn(listKeyValueLongStringCommandObject);
    when(commandExecutor.executeCommand(listKeyValueLongStringCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.arscan(key, 0L, 10L);
    verify(commandExecutor).executeCommand(listKeyValueLongStringCommandObject);
    verify(commandObjects).arscan(key, 0L, 10L);
  }

  @Test
  public void testArscanLimitBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arscan(key, 0L, 10L, 5L)).thenReturn(listKeyValueLongBytesCommandObject);
    when(commandExecutor.executeCommand(listKeyValueLongBytesCommandObject))
        .thenReturn(java.util.Collections.emptyList());

    jedis.arscan(key, 0L, 10L, 5L);
    verify(commandExecutor).executeCommand(listKeyValueLongBytesCommandObject);
    verify(commandObjects).arscan(key, 0L, 10L, 5L);
  }

  @Test
  public void testArseek() {
    String key = "k";
    when(commandObjects.arseek(key, 0L)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.arseek(key, 0L), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arseek(key, 0L);
  }

  @Test
  public void testArseekBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arseek(key, 0L)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.arseek(key, 0L), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arseek(key, 0L);
  }

  @Test
  public void testArset() {
    String key = "k";
    String[] vals = { "x", "y" };
    when(commandObjects.arset(key, 0L, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    assertThat(jedis.arset(key, 0L, vals), equalTo(2L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arset(key, 0L, vals);
  }

  @Test
  public void testArsetBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "x".getBytes() };
    when(commandObjects.arset(key, 0L, vals)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    assertThat(jedis.arset(key, 0L, vals), equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).arset(key, 0L, vals);
  }
}
