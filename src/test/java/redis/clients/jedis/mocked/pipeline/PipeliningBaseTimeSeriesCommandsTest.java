package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.timeseries.*;

public class PipeliningBaseTimeSeriesCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testTsAdd() {
    when(commandObjects.tsAdd("myTimeSeries", 42.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 42.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAddWithTimestamp() {
    when(commandObjects.tsAdd("myTimeSeries", 1000L, 42.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 1000L, 42.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAddWithTimestampAndParams() {
    TSCreateParams createParams = TSCreateParams.createParams();

    when(commandObjects.tsAdd("myTimeSeries", 1000L, 42.0, createParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 1000L, 42.0, createParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAddWithParams() {
    TSAddParams addParams = mock(TSAddParams.class);

    when(commandObjects.tsAdd("myTimeSeries", 1000L, 42.0, addParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 1000L, 42.0, addParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAlter() {
    TSAlterParams alterParams = TSAlterParams.alterParams();

    when(commandObjects.tsAlter("myTimeSeries", alterParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsAlter("myTimeSeries", alterParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreate() {
    when(commandObjects.tsCreate("myTimeSeries")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreate("myTimeSeries");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateWithParams() {
    TSCreateParams createParams = TSCreateParams.createParams();

    when(commandObjects.tsCreate("myTimeSeries", createParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreate("myTimeSeries", createParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateRule() {
    AggregationType aggregationType = AggregationType.AVG;
    long timeBucket = 60;

    when(commandObjects.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, timeBucket)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, timeBucket);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateRuleWithAlignTimestamp() {
    AggregationType aggregationType = AggregationType.AVG;
    long bucketDuration = 60;
    long alignTimestamp = 0;

    when(commandObjects.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, bucketDuration, alignTimestamp)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, bucketDuration, alignTimestamp);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDecrBy() {
    when(commandObjects.tsDecrBy("myTimeSeries", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDecrBy("myTimeSeries", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDecrByWithTimestamp() {
    when(commandObjects.tsDecrBy("myTimeSeries", 1.0, 1000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDecrBy("myTimeSeries", 1.0, 1000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDecrByWithParams() {
    TSDecrByParams decrByParams = mock(TSDecrByParams.class);
    when(commandObjects.tsDecrBy("myTimeSeries", 1.0, decrByParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDecrBy("myTimeSeries", 1.0, decrByParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDel() {
    when(commandObjects.tsDel("myTimeSeries", 1000L, 2000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDel("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDeleteRule() {
    when(commandObjects.tsDeleteRule("sourceTimeSeries", "destTimeSeries")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsDeleteRule("sourceTimeSeries", "destTimeSeries");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsGet() {
    when(commandObjects.tsGet("myTimeSeries")).thenReturn(tsElementCommandObject);

    Response<TSElement> response = pipeliningBase.tsGet("myTimeSeries");

    assertThat(commands, contains(tsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsGetWithParams() {
    TSGetParams getParams = TSGetParams.getParams();

    when(commandObjects.tsGet("myTimeSeries", getParams)).thenReturn(tsElementCommandObject);

    Response<TSElement> response = pipeliningBase.tsGet("myTimeSeries", getParams);

    assertThat(commands, contains(tsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsIncrBy() {
    when(commandObjects.tsIncrBy("myTimeSeries", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsIncrBy("myTimeSeries", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsIncrByWithTimestamp() {
    when(commandObjects.tsIncrBy("myTimeSeries", 1.0, 1000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsIncrBy("myTimeSeries", 1.0, 1000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsIncrByWithParams() {
    TSIncrByParams incrByParams = mock(TSIncrByParams.class);
    when(commandObjects.tsIncrBy("myTimeSeries", 1.0, incrByParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsIncrBy("myTimeSeries", 1.0, incrByParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsInfo() {
    when(commandObjects.tsInfo("myTimeSeries")).thenReturn(tsInfoCommandObject);

    Response<TSInfo> response = pipeliningBase.tsInfo("myTimeSeries");

    assertThat(commands, contains(tsInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsInfoDebug() {
    when(commandObjects.tsInfoDebug("myTimeSeries")).thenReturn(tsInfoCommandObject);

    Response<TSInfo> response = pipeliningBase.tsInfoDebug("myTimeSeries");

    assertThat(commands, contains(tsInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMAdd() {
    Map.Entry<String, TSElement> entry1 = new AbstractMap.SimpleEntry<>("ts1", new TSElement(1000L, 1.0));
    Map.Entry<String, TSElement> entry2 = new AbstractMap.SimpleEntry<>("ts2", new TSElement(2000L, 2.0));

    when(commandObjects.tsMAdd(entry1, entry2)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tsMAdd(entry1, entry2);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMGet() {
    TSMGetParams multiGetParams = TSMGetParams.multiGetParams();
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMGet(multiGetParams, filters)).thenReturn(mapStringTsmGetElementCommandObject);

    Response<Map<String, TSMGetElement>> response = pipeliningBase.tsMGet(multiGetParams, filters);

    assertThat(commands, contains(mapStringTsmGetElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRange() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMRange(1000L, 2000L, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRange(1000L, 2000L, filters);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRangeWithParams() {
    TSMRangeParams multiRangeParams = TSMRangeParams.multiRangeParams();

    when(commandObjects.tsMRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRange(multiRangeParams);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRevRange() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMRevRange(1000L, 2000L, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRevRange(1000L, 2000L, filters);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRevRangeWithParams() {
    TSMRangeParams multiRangeParams = TSMRangeParams.multiRangeParams();

    when(commandObjects.tsMRevRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRevRange(multiRangeParams);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsQueryIndex() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsQueryIndex(filters)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.tsQueryIndex(filters);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRange() {
    when(commandObjects.tsRange("myTimeSeries", 1000L, 2000L)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRange("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRangeWithParams() {
    TSRangeParams rangeParams = TSRangeParams.rangeParams();

    when(commandObjects.tsRange("myTimeSeries", rangeParams)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRange("myTimeSeries", rangeParams);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRevRange() {
    when(commandObjects.tsRevRange("myTimeSeries", 1000L, 2000L)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRevRange("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRevRangeWithParams() {
    TSRangeParams rangeParams = TSRangeParams.rangeParams();

    when(commandObjects.tsRevRange("myTimeSeries", rangeParams)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRevRange("myTimeSeries", rangeParams);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
