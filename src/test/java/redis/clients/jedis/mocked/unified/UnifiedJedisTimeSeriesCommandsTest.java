package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.timeseries.*;

public class UnifiedJedisTimeSeriesCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testTsAdd() {
    String key = "testKey";
    double value = 123.45;
    long expectedResponse = 1582605077000L; // Timestamp of the added value

    when(commandObjects.tsAdd(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsAdd(key, value);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsAdd(key, value);
  }

  @Test
  public void testTsAddWithTimestamp() {
    String key = "testKey";
    long timestamp = 1582605077000L;
    double value = 123.45;
    long expectedResponse = timestamp; // Timestamp of the added value

    when(commandObjects.tsAdd(key, timestamp, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsAdd(key, timestamp, value);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsAdd(key, timestamp, value);
  }

  @Test
  public void testTsAddWithTimestampAndParams() {
    String key = "testKey";
    long timestamp = 1582605077000L;
    double value = 123.45;
    TSCreateParams createParams = new TSCreateParams().retention(86400000L); // 1 day retention
    long expectedResponse = timestamp; // Timestamp of the added value

    when(commandObjects.tsAdd(key, timestamp, value, createParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsAdd(key, timestamp, value, createParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsAdd(key, timestamp, value, createParams);
  }

  @Test
  public void testTsAddWithParams() {
    String key = "testKey";
    long timestamp = 1582605077000L;
    double value = 123.45;
    TSAddParams createParams = mock(TSAddParams.class);
    long expectedResponse = timestamp; // Timestamp of the added value

    when(commandObjects.tsAdd(key, timestamp, value, createParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsAdd(key, timestamp, value, createParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsAdd(key, timestamp, value, createParams);
  }

  @Test
  public void testTsAlter() {
    String key = "testKey";
    TSAlterParams alterParams = new TSAlterParams().retention(86400000L); // 1 day retention
    String expectedResponse = "OK";

    when(commandObjects.tsAlter(key, alterParams)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsAlter(key, alterParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsAlter(key, alterParams);
  }

  @Test
  public void testTsCreate() {
    String key = "testKey";
    String expectedResponse = "OK";

    when(commandObjects.tsCreate(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsCreate(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsCreate(key);
  }

  @Test
  public void testTsCreateWithParams() {
    String key = "testKey";
    TSCreateParams createParams = new TSCreateParams().retention(86400000L); // 1 day retention
    String expectedResponse = "OK";

    when(commandObjects.tsCreate(key, createParams)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsCreate(key, createParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsCreate(key, createParams);
  }

  @Test
  public void testTsCreateRule() {
    String sourceKey = "sourceKey";
    String destKey = "destKey";
    AggregationType aggregationType = AggregationType.AVG;
    long timeBucket = 60000L; // 1 minute
    String expectedResponse = "OK";

    when(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, timeBucket)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsCreateRule(sourceKey, destKey, aggregationType, timeBucket);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsCreateRule(sourceKey, destKey, aggregationType, timeBucket);
  }

  @Test
  public void testTsCreateRuleWithAlignTimestamp() {
    String sourceKey = "sourceKey";
    String destKey = "destKey";
    AggregationType aggregationType = AggregationType.AVG;
    long bucketDuration = 60000L; // 1 minute
    long alignTimestamp = 1582600000000L;
    String expectedResponse = "OK";

    when(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration, alignTimestamp)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration, alignTimestamp);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration, alignTimestamp);
  }

  @Test
  public void testTsDecrBy() {
    String key = "testKey";
    double value = 1.5;
    long expectedResponse = -1L; // Assuming the decrement results in a total of -1

    when(commandObjects.tsDecrBy(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsDecrBy(key, value);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsDecrBy(key, value);
  }

  @Test
  public void testTsDecrByWithTimestamp() {
    String key = "testKey";
    double value = 1.5;
    long timestamp = 1582605077000L;
    long expectedResponse = 5L;

    when(commandObjects.tsDecrBy(key, value, timestamp)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsDecrBy(key, value, timestamp);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsDecrBy(key, value, timestamp);
  }

  @Test
  public void testTsDecrByWithParams() {
    String key = "testKey";
    double value = 1.5;
    TSDecrByParams decrByParams = mock(TSDecrByParams.class);
    long expectedResponse = 5L;

    when(commandObjects.tsDecrBy(key, value, decrByParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsDecrBy(key, value, decrByParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsDecrBy(key, value, decrByParams);
  }

  @Test
  public void testTsDel() {
    String key = "testKey";
    long fromTimestamp = 1582605077000L;
    long toTimestamp = 1582605079000L;
    long expectedResponse = 2L; // Number of deleted entries

    when(commandObjects.tsDel(key, fromTimestamp, toTimestamp)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsDel(key, fromTimestamp, toTimestamp);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsDel(key, fromTimestamp, toTimestamp);
  }

  @Test
  public void testTsDeleteRule() {
    String sourceKey = "sourceKey";
    String destKey = "destKey";
    String expectedResponse = "OK";

    when(commandObjects.tsDeleteRule(sourceKey, destKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tsDeleteRule(sourceKey, destKey);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tsDeleteRule(sourceKey, destKey);
  }

  @Test
  public void testTsGet() {
    String key = "testKey";
    TSElement expectedResponse = new TSElement(1582605077000L, 123.45);

    when(commandObjects.tsGet(key)).thenReturn(tsElementCommandObject);
    when(commandExecutor.executeCommand(tsElementCommandObject)).thenReturn(expectedResponse);

    TSElement result = jedis.tsGet(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(tsElementCommandObject);
    verify(commandObjects).tsGet(key);
  }

  @Test
  public void testTsGetWithParams() {
    String key = "testKey";
    TSGetParams getParams = new TSGetParams().latest();
    TSElement expectedResponse = new TSElement(1582605077000L, 123.45);

    when(commandObjects.tsGet(key, getParams)).thenReturn(tsElementCommandObject);
    when(commandExecutor.executeCommand(tsElementCommandObject)).thenReturn(expectedResponse);

    TSElement result = jedis.tsGet(key, getParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(tsElementCommandObject);
    verify(commandObjects).tsGet(key, getParams);
  }

  @Test
  public void testTsIncrBy() {
    String key = "testKey";
    double value = 2.5;
    long expectedResponse = 5L; // Assuming the increment results in a total of 5

    when(commandObjects.tsIncrBy(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsIncrBy(key, value);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsIncrBy(key, value);
  }

  @Test
  public void testTsIncrByWithTimestamp() {
    String key = "testKey";
    double value = 2.5;
    long timestamp = 1582605077000L;
    long expectedResponse = 5L;

    when(commandObjects.tsIncrBy(key, value, timestamp)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsIncrBy(key, value, timestamp);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsIncrBy(key, value, timestamp);
  }

  @Test
  public void testTsIncrByWithParams() {
    String key = "testKey";
    double value = 2.5;
    TSIncrByParams incrByParams = mock(TSIncrByParams.class);
    long expectedResponse = 5L;

    when(commandObjects.tsIncrBy(key, value, incrByParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.tsIncrBy(key, value, incrByParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).tsIncrBy(key, value, incrByParams);
  }

  @Test
  public void testTsInfo() {
    String key = "testKey";
    TSInfo expectedResponse = mock(TSInfo.class);

    when(commandObjects.tsInfo(key)).thenReturn(tsInfoCommandObject);
    when(commandExecutor.executeCommand(tsInfoCommandObject)).thenReturn(expectedResponse);

    TSInfo result = jedis.tsInfo(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(tsInfoCommandObject);
    verify(commandObjects).tsInfo(key);
  }

  @Test
  public void testTsInfoDebug() {
    String key = "testKey";
    TSInfo expectedResponse = mock(TSInfo.class);

    when(commandObjects.tsInfoDebug(key)).thenReturn(tsInfoCommandObject);
    when(commandExecutor.executeCommand(tsInfoCommandObject)).thenReturn(expectedResponse);

    TSInfo result = jedis.tsInfoDebug(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(tsInfoCommandObject);
    verify(commandObjects).tsInfoDebug(key);
  }

  @Test
  public void testTsMAdd() {
    Map.Entry<String, TSElement> entry1 = new AbstractMap.SimpleEntry<>("key1", new TSElement(1582605077000L, 123.45));
    Map.Entry<String, TSElement> entry2 = new AbstractMap.SimpleEntry<>("key2", new TSElement(1582605078000L, 234.56));
    List<Long> expectedResponse = Arrays.asList(1582605077000L, 1582605078000L); // Timestamps of the added values

    when(commandObjects.tsMAdd(entry1, entry2)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.tsMAdd(entry1, entry2);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).tsMAdd(entry1, entry2);
  }

  @Test
  public void testTsMGet() {
    TSMGetParams multiGetParams = new TSMGetParams().withLabels();
    String[] filters = { "sensor=temperature" };
    Map<String, TSMGetElement> expectedResponse = new HashMap<>();

    when(commandObjects.tsMGet(multiGetParams, filters)).thenReturn(mapStringTsmGetElementCommandObject);
    when(commandExecutor.executeCommand(mapStringTsmGetElementCommandObject)).thenReturn(expectedResponse);

    Map<String, TSMGetElement> result = jedis.tsMGet(multiGetParams, filters);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(mapStringTsmGetElementCommandObject);
    verify(commandObjects).tsMGet(multiGetParams, filters);
  }

  @Test
  public void testTsMRange() {
    long fromTimestamp = 1582600000000L;
    long toTimestamp = 1582605077000L;
    String[] filters = { "sensor=temperature" };
    Map<String, TSMRangeElements> expectedResponse = new HashMap<>();

    when(commandObjects.tsMRange(fromTimestamp, toTimestamp, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);
    when(commandExecutor.executeCommand(mapStringTsmRangeElementsCommandObject)).thenReturn(expectedResponse);

    Map<String, TSMRangeElements> result = jedis.tsMRange(fromTimestamp, toTimestamp, filters);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(mapStringTsmRangeElementsCommandObject);
    verify(commandObjects).tsMRange(fromTimestamp, toTimestamp, filters);
  }

  @Test
  public void testTsMRangeWithParams() {
    TSMRangeParams multiRangeParams = TSMRangeParams.multiRangeParams(1582600000000L, 1582605077000L).filter("sensor=temperature");
    Map<String, TSMRangeElements> expectedResponse = new HashMap<>();

    when(commandObjects.tsMRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);
    when(commandExecutor.executeCommand(mapStringTsmRangeElementsCommandObject)).thenReturn(expectedResponse);

    Map<String, TSMRangeElements> result = jedis.tsMRange(multiRangeParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(mapStringTsmRangeElementsCommandObject);
    verify(commandObjects).tsMRange(multiRangeParams);
  }

  @Test
  public void testTsMRevRange() {
    long fromTimestamp = 1582600000000L;
    long toTimestamp = 1582605077000L;
    String[] filters = { "sensor=temperature" };
    Map<String, TSMRangeElements> expectedResponse = new HashMap<>();

    when(commandObjects.tsMRevRange(fromTimestamp, toTimestamp, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);
    when(commandExecutor.executeCommand(mapStringTsmRangeElementsCommandObject)).thenReturn(expectedResponse);

    Map<String, TSMRangeElements> result = jedis.tsMRevRange(fromTimestamp, toTimestamp, filters);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(mapStringTsmRangeElementsCommandObject);
    verify(commandObjects).tsMRevRange(fromTimestamp, toTimestamp, filters);
  }

  @Test
  public void testTsMRevRangeWithParams() {
    TSMRangeParams multiRangeParams =
        TSMRangeParams.multiRangeParams(1582600000000L, 1582605077000L).filter("sensor=temperature");
    Map<String, TSMRangeElements> expectedResponse = new HashMap<>();

    when(commandObjects.tsMRevRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);
    when(commandExecutor.executeCommand(mapStringTsmRangeElementsCommandObject)).thenReturn(expectedResponse);

    Map<String, TSMRangeElements> result = jedis.tsMRevRange(multiRangeParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(mapStringTsmRangeElementsCommandObject);
    verify(commandObjects).tsMRevRange(multiRangeParams);
  }

  @Test
  public void testTsQueryIndex() {
    String[] filters = { "sensor=temperature", "location=warehouse" };
    List<String> expectedResponse = Arrays.asList("series1", "series2");

    when(commandObjects.tsQueryIndex(filters)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.tsQueryIndex(filters);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).tsQueryIndex(filters);
  }

  @Test
  public void testTsRange() {
    String key = "testKey";
    long fromTimestamp = 1582600000000L;
    long toTimestamp = 1582605077000L;
    List<TSElement> expectedResponse = Collections.singletonList(new TSElement(fromTimestamp, 123.45));

    when(commandObjects.tsRange(key, fromTimestamp, toTimestamp)).thenReturn(listTsElementCommandObject);
    when(commandExecutor.executeCommand(listTsElementCommandObject)).thenReturn(expectedResponse);

    List<TSElement> result = jedis.tsRange(key, fromTimestamp, toTimestamp);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listTsElementCommandObject);
    verify(commandObjects).tsRange(key, fromTimestamp, toTimestamp);
  }

  @Test
  public void testTsRangeWithParams() {
    String key = "testKey";
    TSRangeParams rangeParams = TSRangeParams.rangeParams(1582600000000L, 1582605077000L);
    List<TSElement> expectedResponse = Arrays.asList(
        new TSElement(1582600000000L, 123.45),
        new TSElement(1582605077000L, 234.56));

    when(commandObjects.tsRange(key, rangeParams)).thenReturn(listTsElementCommandObject);
    when(commandExecutor.executeCommand(listTsElementCommandObject)).thenReturn(expectedResponse);

    List<TSElement> result = jedis.tsRange(key, rangeParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listTsElementCommandObject);
    verify(commandObjects).tsRange(key, rangeParams);
  }

  @Test
  public void testTsRevRange() {
    String key = "testKey";
    long fromTimestamp = 1582600000000L;
    long toTimestamp = 1582605077000L;
    List<TSElement> expectedResponse = Collections.singletonList(new TSElement(toTimestamp, 234.56));

    when(commandObjects.tsRevRange(key, fromTimestamp, toTimestamp)).thenReturn(listTsElementCommandObject);
    when(commandExecutor.executeCommand(listTsElementCommandObject)).thenReturn(expectedResponse);

    List<TSElement> result = jedis.tsRevRange(key, fromTimestamp, toTimestamp);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listTsElementCommandObject);
    verify(commandObjects).tsRevRange(key, fromTimestamp, toTimestamp);
  }

  @Test
  public void testTsRevRangeWithParams() {
    String key = "testKey";
    TSRangeParams rangeParams = TSRangeParams.rangeParams(1582600000000L, 1582605077000L);
    List<TSElement> expectedResponse = Arrays.asList(
        new TSElement(1582605077000L, 234.56),
        new TSElement(1582600000000L, 123.45));

    when(commandObjects.tsRevRange(key, rangeParams)).thenReturn(listTsElementCommandObject);
    when(commandExecutor.executeCommand(listTsElementCommandObject)).thenReturn(expectedResponse);

    List<TSElement> result = jedis.tsRevRange(key, rangeParams);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listTsElementCommandObject);
    verify(commandObjects).tsRevRange(key, rangeParams);
  }

}
