package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.ArrayAggregate;
import redis.clients.jedis.args.ArrayBitwise;
import redis.clients.jedis.args.LongRange;
import redis.clients.jedis.params.ArgrepParams;

public class PipeliningBaseArrayCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testArcount() {
    when(commandObjects.arcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.arcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testArcountBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.arcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.arcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testArdel() {
    long[] idx = { 0L };
    when(commandObjects.ardel("k", idx)).thenReturn(longCommandObject);
    Response<Long> r = pipeliningBase.ardel("k", idx);
    assertThat(commands, contains(longCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArdelBinary() {
    byte[] key = "k".getBytes();
    long[] idx = { 0L };
    when(commandObjects.ardel(key, idx)).thenReturn(longCommandObject);
    Response<Long> r = pipeliningBase.ardel(key, idx);
    assertThat(commands, contains(longCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArdelrange() {
    LongRange[] ranges = { LongRange.of(0L, 1L) };
    when(commandObjects.ardelrange("k", ranges)).thenReturn(longCommandObject);
    Response<Long> r = pipeliningBase.ardelrange("k", ranges);
    assertThat(commands, contains(longCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArdelrangeBinary() {
    byte[] key = "k".getBytes();
    LongRange[] ranges = { LongRange.of(0L, 1L) };
    when(commandObjects.ardelrange(key, ranges)).thenReturn(longCommandObject);
    Response<Long> r = pipeliningBase.ardelrange(key, ranges);
    assertThat(commands, contains(longCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArget() {
    when(commandObjects.arget("k", 0L)).thenReturn(stringCommandObject);
    Response<String> r = pipeliningBase.arget("k", 0L);
    assertThat(commands, contains(stringCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArgetBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arget(key, 0L)).thenReturn(bytesCommandObject);
    Response<byte[]> r = pipeliningBase.arget(key, 0L);
    assertThat(commands, contains(bytesCommandObject));
    assertThat(r, is(predefinedResponse));
  }

  @Test
  public void testArgetrange() {
    when(commandObjects.argetrange("k", 0L, 1L)).thenReturn(listStringCommandObject);
    pipeliningBase.argetrange("k", 0L, 1L);
    assertThat(commands, contains(listStringCommandObject));
  }

  @Test
  public void testArgetrangeBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.argetrange(key, 0L, 1L)).thenReturn(listBytesCommandObject);
    pipeliningBase.argetrange(key, 0L, 1L);
    assertThat(commands, contains(listBytesCommandObject));
  }

  @Test
  public void testArgrep() {
    ArgrepParams params = ArgrepParams.argrepParams().match("x");
    when(commandObjects.argrep("k", 0L, 10L, params)).thenReturn(listLongCommandObject);
    pipeliningBase.argrep("k", 0L, 10L, params);
    assertThat(commands, contains(listLongCommandObject));
  }

  @Test
  public void testArgrepBinary() {
    byte[] key = "k".getBytes();
    ArgrepParams params = ArgrepParams.argrepParams().exact("x");
    when(commandObjects.argrep(key, 0L, 10L, params)).thenReturn(listLongCommandObject);
    pipeliningBase.argrep(key, 0L, 10L, params);
    assertThat(commands, contains(listLongCommandObject));
  }

  @Test
  public void testArgrepWithValues() {
    ArgrepParams params = ArgrepParams.argrepParams().match("x");
    when(commandObjects.argrepWithValues("k", 0L, 10L, params))
        .thenReturn(listKeyValueLongStringCommandObject);
    pipeliningBase.argrepWithValues("k", 0L, 10L, params);
    assertThat(commands, contains(listKeyValueLongStringCommandObject));
  }

  @Test
  public void testArgrepWithValuesBinary() {
    byte[] key = "k".getBytes();
    ArgrepParams params = ArgrepParams.argrepParams().exact("x");
    when(commandObjects.argrepWithValues(key, 0L, 10L, params))
        .thenReturn(listKeyValueLongBytesCommandObject);
    pipeliningBase.argrepWithValues(key, 0L, 10L, params);
    assertThat(commands, contains(listKeyValueLongBytesCommandObject));
  }

  @Test
  public void testArinfo() {
    when(commandObjects.arinfo("k")).thenReturn(arrayInfoCommandObject);
    pipeliningBase.arinfo("k");
    assertThat(commands, contains(arrayInfoCommandObject));
  }

  @Test
  public void testArinfoBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arinfo(key)).thenReturn(arrayInfoCommandObject);
    pipeliningBase.arinfo(key);
    assertThat(commands, contains(arrayInfoCommandObject));
  }

  @Test
  public void testArinfoFull() {
    when(commandObjects.arinfoFull("k")).thenReturn(arrayFullInfoCommandObject);
    pipeliningBase.arinfoFull("k");
    assertThat(commands, contains(arrayFullInfoCommandObject));
  }

  @Test
  public void testArinfoFullBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arinfoFull(key)).thenReturn(arrayFullInfoCommandObject);
    pipeliningBase.arinfoFull(key);
    assertThat(commands, contains(arrayFullInfoCommandObject));
  }

  @Test
  public void testArinsert() {
    String[] vals = { "a" };
    when(commandObjects.arinsert("k", vals)).thenReturn(longCommandObject);
    pipeliningBase.arinsert("k", vals);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArinsertBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "a".getBytes() };
    when(commandObjects.arinsert(key, vals)).thenReturn(longCommandObject);
    pipeliningBase.arinsert(key, vals);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArlastitems() {
    when(commandObjects.arlastitems("k", 2L)).thenReturn(listStringCommandObject);
    pipeliningBase.arlastitems("k", 2L);
    assertThat(commands, contains(listStringCommandObject));
  }

  @Test
  public void testArlastitemsRevBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arlastitems(key, 2L, true)).thenReturn(listBytesCommandObject);
    pipeliningBase.arlastitems(key, 2L, true);
    assertThat(commands, contains(listBytesCommandObject));
  }

  @Test
  public void testArlen() {
    when(commandObjects.arlen("k")).thenReturn(longCommandObject);
    pipeliningBase.arlen("k");
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArlenBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arlen(key)).thenReturn(longCommandObject);
    pipeliningBase.arlen(key);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArmget() {
    long[] idx = { 0L, 1L };
    when(commandObjects.armget("k", idx)).thenReturn(listStringCommandObject);
    pipeliningBase.armget("k", idx);
    assertThat(commands, contains(listStringCommandObject));
  }

  @Test
  public void testArmgetBinary() {
    byte[] key = "k".getBytes();
    long[] idx = { 0L };
    when(commandObjects.armget(key, idx)).thenReturn(listBytesCommandObject);
    pipeliningBase.armget(key, idx);
    assertThat(commands, contains(listBytesCommandObject));
  }

  @Test
  public void testArmset() {
    Map<Long, String> map = new LinkedHashMap<>();
    map.put(0L, "a");
    when(commandObjects.armset("k", map)).thenReturn(longCommandObject);
    pipeliningBase.armset("k", map);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArmsetBinary() {
    byte[] key = "k".getBytes();
    Map<Long, byte[]> map = new LinkedHashMap<>();
    map.put(0L, "a".getBytes());
    when(commandObjects.armset(key, map)).thenReturn(longCommandObject);
    pipeliningBase.armset(key, map);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArnext() {
    when(commandObjects.arnext("k")).thenReturn(optionalLongCommandObject);
    pipeliningBase.arnext("k");
    assertThat(commands, contains(optionalLongCommandObject));
  }

  @Test
  public void testArnextBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arnext(key)).thenReturn(optionalLongCommandObject);
    pipeliningBase.arnext(key);
    assertThat(commands, contains(optionalLongCommandObject));
  }

  @Test
  public void testAropBitwise() {
    when(commandObjects.aropBitwise("k", 0L, 10L, ArrayBitwise.AND)).thenReturn(longCommandObject);
    pipeliningBase.aropBitwise("k", 0L, 10L, ArrayBitwise.AND);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testAropBitwiseBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.aropBitwise(key, 0L, 10L, ArrayBitwise.OR)).thenReturn(longCommandObject);
    pipeliningBase.aropBitwise(key, 0L, 10L, ArrayBitwise.OR);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testAropAggregate() {
    when(commandObjects.aropAggregate("k", 0L, 10L, ArrayAggregate.SUM))
        .thenReturn(stringCommandObject);
    pipeliningBase.aropAggregate("k", 0L, 10L, ArrayAggregate.SUM);
    assertThat(commands, contains(stringCommandObject));
  }

  @Test
  public void testAropAggregateBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.aropAggregate(key, 0L, 10L, ArrayAggregate.MIN))
        .thenReturn(bytesCommandObject);
    pipeliningBase.aropAggregate(key, 0L, 10L, ArrayAggregate.MIN);
    assertThat(commands, contains(bytesCommandObject));
  }

  @Test
  public void testAropCount() {
    when(commandObjects.aropCount("k", 0L, 10L)).thenReturn(longCommandObject);
    pipeliningBase.aropCount("k", 0L, 10L);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testAropCountBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.aropCount(key, 0L, 10L)).thenReturn(longCommandObject);
    pipeliningBase.aropCount(key, 0L, 10L);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testAropCountMatch() {
    when(commandObjects.aropCount("k", 0L, 10L, "v")).thenReturn(longCommandObject);
    pipeliningBase.aropCount("k", 0L, 10L, "v");
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testAropCountMatchBinary() {
    byte[] key = "k".getBytes();
    byte[] val = "v".getBytes();
    when(commandObjects.aropCount(key, 0L, 10L, val)).thenReturn(longCommandObject);
    pipeliningBase.aropCount(key, 0L, 10L, val);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArring() {
    String[] vals = { "x" };
    when(commandObjects.arring("k", 2L, vals)).thenReturn(longCommandObject);
    pipeliningBase.arring("k", 2L, vals);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArringBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "x".getBytes() };
    when(commandObjects.arring(key, 2L, vals)).thenReturn(longCommandObject);
    pipeliningBase.arring(key, 2L, vals);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArscan() {
    when(commandObjects.arscan("k", 0L, 10L)).thenReturn(listKeyValueLongStringCommandObject);
    pipeliningBase.arscan("k", 0L, 10L);
    assertThat(commands, contains(listKeyValueLongStringCommandObject));
  }

  @Test
  public void testArscanLimitBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arscan(key, 0L, 10L, 5L)).thenReturn(listKeyValueLongBytesCommandObject);
    pipeliningBase.arscan(key, 0L, 10L, 5L);
    assertThat(commands, contains(listKeyValueLongBytesCommandObject));
  }

  @Test
  public void testArseek() {
    when(commandObjects.arseek("k", 0L)).thenReturn(longCommandObject);
    pipeliningBase.arseek("k", 0L);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArseekBinary() {
    byte[] key = "k".getBytes();
    when(commandObjects.arseek(key, 0L)).thenReturn(longCommandObject);
    pipeliningBase.arseek(key, 0L);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArset() {
    String[] vals = { "x" };
    when(commandObjects.arset("k", 0L, vals)).thenReturn(longCommandObject);
    pipeliningBase.arset("k", 0L, vals);
    assertThat(commands, contains(longCommandObject));
  }

  @Test
  public void testArsetBinary() {
    byte[] key = "k".getBytes();
    byte[][] vals = { "x".getBytes() };
    when(commandObjects.arset(key, 0L, vals)).thenReturn(longCommandObject);
    pipeliningBase.arset(key, 0L, vals);
    assertThat(commands, contains(longCommandObject));
  }
}
