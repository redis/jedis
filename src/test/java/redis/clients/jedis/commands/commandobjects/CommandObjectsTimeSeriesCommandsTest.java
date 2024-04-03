package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.timeseries.AggregationType;
import redis.clients.jedis.timeseries.TSAlterParams;
import redis.clients.jedis.timeseries.TSCreateParams;
import redis.clients.jedis.timeseries.TSElement;
import redis.clients.jedis.timeseries.TSGetParams;
import redis.clients.jedis.timeseries.TSInfo;
import redis.clients.jedis.timeseries.TSMGetElement;
import redis.clients.jedis.timeseries.TSMGetParams;
import redis.clients.jedis.timeseries.TSMRangeElements;
import redis.clients.jedis.timeseries.TSMRangeParams;
import redis.clients.jedis.timeseries.TSRangeParams;

/**
 * Tests related to <a href="https://redis.io/commands/?group=timeseries">Time series</a> commands.
 */
public class CommandObjectsTimeSeriesCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsTimeSeriesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testTsAddAndRange() throws InterruptedException {
    String key = "testTs";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    long currentTime = System.currentTimeMillis();
    double[] values = { 42.0, 43.0, 44.0 };
    for (double value : values) {
      Long add = exec(commandObjects.tsAdd(key, value));
      assertThat(add, notNullValue());

      // short delay to avoid the same timestamp
      Thread.sleep(10);
    }

    List<TSElement> range = exec(commandObjects.tsRange(key, currentTime - 1000, currentTime + 1000));

    assertThat(range, hasSize(values.length));
    for (int i = 0; i < values.length; i++) {
      assertThat(range.get(i).getValue(), equalTo(values[i]));
    }
  }

  @Test
  public void testTsAddWithTimestampDelAndRangeWithPreDeleteAssert() {
    String key = "testTs";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    long timestamp1 = 1000;
    double value1 = 42.0;

    long timestamp2 = 2000;
    double value2 = 43.0;

    Long add1 = exec(commandObjects.tsAdd(key, timestamp1, value1));
    assertThat(add1, notNullValue());

    Long add2 = exec(commandObjects.tsAdd(key, timestamp2, value2));
    assertThat(add2, notNullValue());

    List<TSElement> preDelRange = exec(commandObjects.tsRange(key, timestamp1 - 500, timestamp2 + 500));

    assertThat(preDelRange, hasSize(2));
    assertThat(preDelRange.get(0).getValue(), equalTo(value1));
    assertThat(preDelRange.get(1).getValue(), equalTo(value2));

    Long del = exec(commandObjects.tsDel(key, timestamp1 - 500, timestamp1 + 500));
    assertThat(del, equalTo(1L));

    List<TSElement> postDelRange = exec(commandObjects.tsRange(key, timestamp1 - 500, timestamp2 + 500));

    assertThat(postDelRange, hasSize(1));
    assertThat(postDelRange.get(0).getValue(), equalTo(value2));
  }

  @Test
  public void testTsAddWithParams() {
    String key = "testTs";

    long timestamp = System.currentTimeMillis();
    double value = 42.0;

    TSCreateParams createParams = new TSCreateParams()
        .uncompressed().retention(86400000);

    Long add = exec(commandObjects.tsAdd(key, timestamp, value, createParams));
    assertThat(add, notNullValue());

    List<TSElement> range = exec(commandObjects.tsRange(key, timestamp - 1000, timestamp + 1000));

    assertThat(range, hasSize(1));
    assertThat(range.get(0).getTimestamp(), equalTo(timestamp));
    assertThat(range.get(0).getValue(), equalTo(value));
  }

  @Test
  public void testTsMAdd() {
    String key1 = "testTsMAdd1";
    String key2 = "testTsMAdd2";

    String create1 = exec(commandObjects.tsCreate(key1));
    assertThat(create1, equalTo("OK"));

    String create2 = exec(commandObjects.tsCreate(key2));
    assertThat(create2, equalTo("OK"));

    long timestamp1 = 2000;
    long timestamp2 = 3000;

    Map.Entry<String, TSElement> entry1 =
        new AbstractMap.SimpleEntry<>(key1, new TSElement(timestamp1, 42.0));

    Map.Entry<String, TSElement> entry2 =
        new AbstractMap.SimpleEntry<>(key2, new TSElement(timestamp2, 43.0));

    List<Long> mAdd = exec(commandObjects.tsMAdd(entry1, entry2));
    assertThat(mAdd, contains(timestamp1, timestamp2));

    List<TSElement> range1 = exec(commandObjects.tsRange(key1, timestamp1 - 1000, timestamp1 + 1000));

    assertThat(range1, hasSize(1));
    assertThat(range1.get(0).getTimestamp(), equalTo(timestamp1));
    assertThat(range1.get(0).getValue(), equalTo(42.0));

    List<TSElement> range2 = exec(commandObjects.tsRange(key2, timestamp2 - 1000, timestamp2 + 1000));

    assertThat(range2, hasSize(1));
    assertThat(range2.get(0).getTimestamp(), equalTo(timestamp2));
    assertThat(range2.get(0).getValue(), equalTo(43.0));
  }

  @Test
  public void testTsIncrByAndDecrBy() throws InterruptedException {
    String key = "testTs";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    double initialValue = 10.0;
    double incrementValue = 5.0;
    double decrementValue = 3.0;

    Long initialAdd = exec(commandObjects.tsAdd(key, System.currentTimeMillis(), initialValue));
    assertThat(initialAdd, notNullValue());

    Thread.sleep(50);

    Long incr = exec(commandObjects.tsIncrBy(key, incrementValue));
    assertThat(incr, notNullValue());

    Thread.sleep(50);

    Long decr = exec(commandObjects.tsDecrBy(key, decrementValue));
    assertThat(decr, notNullValue());

    TSElement latestElement = exec(commandObjects.tsGet(key));
    double expectedValue = initialValue + incrementValue - decrementValue;
    assertThat(latestElement.getValue(), equalTo(expectedValue));

    List<TSElement> range = exec(commandObjects.tsRange(
        key, latestElement.getTimestamp() - 1000, latestElement.getTimestamp() + 1000));

    assertThat(range.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(initialValue, 0.001),
        closeTo(initialValue + incrementValue, 0.001),
        closeTo(expectedValue, 0.001)));
  }

  @Test
  public void testTsIncrByAndDecrByWithTimestamp() {
    String key = "testTs";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    double initialValue = 10.0;
    double incrementValue = 5.0;
    double decrementValue = 3.0;

    long initialTimestamp = System.currentTimeMillis();

    Long initialAdd = exec(commandObjects.tsAdd(key, initialTimestamp, initialValue));
    assertThat(initialAdd, equalTo(initialTimestamp));

    long incrementTimestamp = initialTimestamp + 1000;

    Long incr = exec(commandObjects.tsIncrBy(key, incrementValue, incrementTimestamp));
    assertThat(incr, equalTo(incrementTimestamp));

    long decrementTimestamp = incrementTimestamp + 1000;

    Long decr = exec(commandObjects.tsDecrBy(key, decrementValue, decrementTimestamp));
    assertThat(decr, equalTo(decrementTimestamp));

    List<TSElement> range = exec(commandObjects.tsRange(
        key, initialTimestamp - 1000, decrementTimestamp + 1000));

    assertThat(range.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(initialValue, 0.001),
        closeTo(initialValue + incrementValue, 0.001),
        closeTo(initialValue + incrementValue - decrementValue, 0.001)));
  }

  @Test
  public void testTsRange() {
    String key = "tsKey";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    long fromTimestamp = 1000L;
    long toTimestamp = 2000L;

    List<TSElement> initialRange = exec(commandObjects.tsRange(key, fromTimestamp - 100, toTimestamp + 100));
    assertThat(initialRange, hasSize(0));

    exec(commandObjects.tsAdd(key, fromTimestamp, 1.0));
    exec(commandObjects.tsAdd(key, toTimestamp, 2.0));

    List<TSElement> elementsByTimestamp = exec(commandObjects.tsRange(key, fromTimestamp - 100, toTimestamp + 100));

    assertThat(elementsByTimestamp.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(1.0, 0.001),
        closeTo(2.0, 0.001)));

    TSRangeParams rangeParams = new TSRangeParams(fromTimestamp - 100, toTimestamp + 100);

    List<TSElement> elementsByParams = exec(commandObjects.tsRange(key, rangeParams));

    assertThat(elementsByParams.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(1.0, 0.001),
        closeTo(2.0, 0.001)));
  }

  @Test
  public void testTsRevRange() {
    String key = "tsRevKey";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    long fromTimestamp = 1000L;
    long toTimestamp = 2000L;

    List<TSElement> initialRevRange = exec(commandObjects.tsRevRange(key, fromTimestamp - 100, toTimestamp + 100));
    assertThat(initialRevRange, hasSize(0));

    exec(commandObjects.tsAdd(key, fromTimestamp, 1.0));
    exec(commandObjects.tsAdd(key, toTimestamp, 2.0));

    List<TSElement> elementsByTimestamp = exec(commandObjects.tsRevRange(key, fromTimestamp - 100, toTimestamp + 100));

    assertThat(elementsByTimestamp.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(2.0, 0.001),
        closeTo(1.0, 0.001)));

    TSRangeParams rangeParams = new TSRangeParams(fromTimestamp - 100, toTimestamp + 100);

    List<TSElement> elementsByParams = exec(commandObjects.tsRevRange(key, rangeParams));

    assertThat(elementsByParams.stream().map(TSElement::getValue).collect(Collectors.toList()), contains(
        closeTo(2.0, 0.001),
        closeTo(1.0, 0.001)));
  }

  @Test
  public void testTsMRangeCommands() {
    String key1 = "tsMRangeKey1";
    String key2 = "tsMRangeKey2";

    long fromTimestamp = 1000L;
    long toTimestamp = 3000L;

    String filter = "sensor_id=1234";

    Map<String, TSMRangeElements> initialMRange = exec(commandObjects.tsMRange(
        fromTimestamp - 100, toTimestamp + 100, filter));
    assertThat(initialMRange.entrySet(), hasSize(0));

    TSCreateParams createParams = new TSCreateParams()
        .uncompressed().label("sensor_id", "1234");

    exec(commandObjects.tsAdd(key1, fromTimestamp, 1.0, createParams));
    exec(commandObjects.tsAdd(key1, fromTimestamp + 500, 1.5, createParams));
    exec(commandObjects.tsAdd(key2, toTimestamp - 500, 2.5, createParams));
    exec(commandObjects.tsAdd(key2, toTimestamp, 2.0, createParams));

    Map<String, TSMRangeElements> range = exec(commandObjects.tsMRange(
        fromTimestamp - 100, toTimestamp + 100, filter));

    assertThat(range.keySet(), hasItems(key1, key2));
    assertThat(range.get(key1).getElements().stream().map(TSElement::getValue).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(1.0, 0.001), closeTo(1.5, 0.001)));
    assertThat(range.get(key2).getElements().stream().map(TSElement::getValue).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(2.5, 0.001), closeTo(2.0, 0.001)));

    Map<String, TSMRangeElements> revRangeResult = exec(commandObjects.tsMRevRange(
        fromTimestamp - 100, toTimestamp + 100, filter));

    assertThat(revRangeResult.keySet(), hasItems(key1, key2));
    assertThat(revRangeResult.get(key1).getElements().stream().map(TSElement::getValue).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(1.5, 0.001), closeTo(1.0, 0.001)));
    assertThat(revRangeResult.get(key2).getElements().stream().map(TSElement::getValue).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(2.0, 0.001), closeTo(2.5, 0.001)));

    TSMRangeParams multiRangeParamsA =
        new TSMRangeParams(fromTimestamp - 100, toTimestamp + 100).filter(filter);

    Map<String, TSMRangeElements> rangeResultWithParams = exec(commandObjects.tsMRange(multiRangeParamsA));

    assertThat(rangeResultWithParams.keySet(), hasItems(key1, key2));
    assertThat(rangeResultWithParams, equalTo(range));

    TSMRangeParams multiRangeParams = new TSMRangeParams(fromTimestamp - 100, toTimestamp + 100).filter(filter);

    Map<String, TSMRangeElements> revRangeResultWithParams = exec(commandObjects.tsMRevRange(multiRangeParams));

    assertThat(revRangeResultWithParams.keySet(), hasItems(key1, key2));
    assertThat(revRangeResultWithParams, equalTo(revRangeResult));
  }

  @Test
  public void testTsGet() {
    String key = "tsGetKey";

    String create = exec(commandObjects.tsCreate(key));
    assertThat(create, equalTo("OK"));

    long timestamp = 1000L;

    double firstValue = 2.5;
    double secondValue = 3.5;

    TSElement initialGet = exec(commandObjects.tsGet(key));
    assertThat(initialGet, nullValue());

    exec(commandObjects.tsAdd(key, timestamp, firstValue));
    exec(commandObjects.tsAdd(key, timestamp + 100, secondValue));

    TSElement getLastValue = exec(commandObjects.tsGet(key));
    assertThat(getLastValue, notNullValue());
    assertThat(getLastValue.getValue(), closeTo(secondValue, 0.001));

    TSElement getWithParams = exec(commandObjects.tsGet(key, new TSGetParams().latest()));
    assertThat(getWithParams, notNullValue());
    assertThat(getWithParams.getValue(), closeTo(secondValue, 0.001));
  }

  @Test
  public void testTsMGet() {
    String key1 = "tsMGetKey1";
    String key2 = "tsMGetKey2";

    long timestamp1 = 1000L;
    long timestamp2 = 2000L;

    double value1 = 1.0;
    double value2 = 2.0;

    String filter = "sensor_id=1234";

    TSCreateParams createParams = new TSCreateParams()
        .uncompressed().label("sensor_id", "1234");

    exec(commandObjects.tsAdd(key1, timestamp1, value1, createParams));
    exec(commandObjects.tsAdd(key2, timestamp2, value2, createParams));

    TSMGetParams multiGetParams = new TSMGetParams().withLabels();

    Map<String, TSMGetElement> elements = exec(commandObjects.tsMGet(multiGetParams, filter));

    assertThat(elements.keySet(), hasItems(key1, key2));

    TSMGetElement element1 = elements.get(key1);
    assertThat(element1, notNullValue());
    assertThat(element1.getElement().getTimestamp(), equalTo(timestamp1));
    assertThat(element1.getElement().getValue(), closeTo(value1, 0.001));

    TSMGetElement element2 = elements.get(key2);
    assertThat(element2, notNullValue());
    assertThat(element2.getElement().getTimestamp(), equalTo(timestamp2));
    assertThat(element2.getElement().getValue(), closeTo(value2, 0.001));

    assertThat(element1.getLabels(), hasEntry("sensor_id", "1234"));
    assertThat(element2.getLabels(), hasEntry("sensor_id", "1234"));
  }

  @Test
  public void testTsCreateRule() {
    String sourceKey = "tsSourceKey";
    String destKey = "tsDestKey";

    AggregationType aggregationType = AggregationType.AVG;

    long timeBucket = 60000; // 1 minute

    exec(commandObjects.tsCreate(sourceKey));
    exec(commandObjects.tsCreate(destKey));

    String createRule = exec(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, timeBucket));
    assertThat(createRule, equalTo("OK"));

    long timestamp1 = 1000L; // 1 second
    double value1 = 10.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp1, value1));

    long timestamp2 = 30000L; // 30 seconds
    double value2 = 20.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp2, value2));

    long timestamp3 = 100000L; // 100 seconds, should be in the second aggregation bucket
    double value3 = 30.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp3, value3));

    long timestamp4 = 200000L; // 200 seconds, should be in the fourth aggregation bucket
    double value4 = 1.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp4, value4));

    // Verify that aggregated data appears in the destination key
    // We only check the first three buckets, i.e. 180 seconds
    // The average of value1 and value2 should be in the first bucket, value3 in the second
    List<TSElement> destElements = exec(commandObjects.tsRange(destKey, 0, 180000));

    assertThat(destElements.size(), equalTo(2));

    double expectedAvgFirstBucket = (value1 + value2) / 2.0;
    assertThat(destElements.get(0).getValue(), closeTo(expectedAvgFirstBucket, 0.001));

    assertThat(destElements.get(1).getValue(), closeTo(value3, 0.001));
  }

  @Test
  public void testTsCreateRuleWithAlign() {
    String sourceKey = "tsSourceKey";
    String destKey = "tsDestKey";

    AggregationType aggregationType = AggregationType.AVG;

    long timeBucket = 60000; // 1 minute

    exec(commandObjects.tsCreate(sourceKey));
    exec(commandObjects.tsCreate(destKey));

    String createRule = exec(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, timeBucket, 2000));
    assertThat(createRule, equalTo("OK"));

    long timestamp1 = 1000L; // 1 second
    double value1 = 10.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp1, value1));

    long timestamp2 = 30000L; // 30 seconds
    double value2 = 20.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp2, value2));

    long timestamp3 = 100000L; // 100 seconds, should be in the second aggregation bucket
    double value3 = 30.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp3, value3));

    long timestamp4 = 200000L; // 200 seconds, should be in the fourth aggregation bucket
    double value4 = 1.0;
    exec(commandObjects.tsAdd(sourceKey, timestamp4, value4));

    // Verify that aggregated data appears in the destination key
    // We only check the first three buckets, i.e. 180 seconds
    // The average of value1 and value2 should be in the first bucket, value3 in the second
    List<TSElement> destElements = exec(commandObjects.tsRange(destKey, 2000, 182000));

    assertThat(destElements.size(), equalTo(2));
    assertThat(destElements.get(0).getValue(), closeTo(value2, 0.001));
    assertThat(destElements.get(1).getValue(), closeTo(value3, 0.001));
  }


  @Test
  public void testTsDeleteRule() {
    String sourceKey = "tsSourceKeyForDeletionWithData";
    String destKey = "tsDestKeyForDeletionWithData";

    AggregationType aggregationType = AggregationType.SUM;

    long bucketDuration = 60000; // 1 minute

    exec(commandObjects.tsCreate(sourceKey));
    exec(commandObjects.tsCreate(destKey));

    exec(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration));

    long initialTimestamp = 1000;
    exec(commandObjects.tsAdd(sourceKey, initialTimestamp, 10.0));

    // This will force aggregation of the first bucket
    exec(commandObjects.tsAdd(sourceKey, initialTimestamp + bucketDuration, 20.0));

    List<TSElement> initialAggregatedData = exec(commandObjects.tsRange(destKey, 0, bucketDuration));

    assertThat(initialAggregatedData.stream().map(TSElement::getValue).collect(Collectors.toList()),
        contains(closeTo(10.0, 0.001)));

    List<TSElement> initialAggregatedDataSecondBucket = exec(commandObjects.tsRange(destKey, bucketDuration, 2 * bucketDuration));

    assertThat(initialAggregatedDataSecondBucket.stream().map(TSElement::getValue).collect(Collectors.toList()),
        empty());

    // Delete the rule
    String deleteRule = exec(commandObjects.tsDeleteRule(sourceKey, destKey));
    assertThat(deleteRule, equalTo("OK"));

    // Add more data to the source key after the rule has been deleted
    long postDeletionTimestamp = initialTimestamp + bucketDuration + 10;

    exec(commandObjects.tsAdd(sourceKey, postDeletionTimestamp, 20));

    // This should force the aggregation of the second bucket, if there was a rule
    exec(commandObjects.tsAdd(sourceKey, postDeletionTimestamp + bucketDuration, 20));

    // Make sure that the data in the destination key has not changed
    List<TSElement> postDeletionAggregatedData = exec(commandObjects.tsRange(destKey, 0, bucketDuration));

    assertThat(postDeletionAggregatedData.stream().map(TSElement::getValue).collect(Collectors.toList()),
        contains(closeTo(10.0, 0.001)));

    List<TSElement> postDeletionAggregatedDataSecondBucket = exec(commandObjects.tsRange(destKey, bucketDuration, 2 * bucketDuration));

    assertThat(postDeletionAggregatedDataSecondBucket.stream().map(TSElement::getValue).collect(Collectors.toList()),
        empty());
  }

  @Test
  public void testTsQueryIndexWithKeyCreation() {
    String key1 = "temperature:sensor:1";
    String key2 = "temperature:sensor:2";
    String key3 = "humidity:sensor:1";

    TSCreateParams paramsTempSensor1 = new TSCreateParams()
        .label("type", "temperature").label("sensor_id", "1");

    exec(commandObjects.tsCreate(key1, paramsTempSensor1));

    TSCreateParams paramsTempSensor2 = new TSCreateParams()
        .label("type", "temperature").label("sensor_id", "2");

    exec(commandObjects.tsCreate(key2, paramsTempSensor2));

    TSCreateParams paramsHumiditySensor1 = new TSCreateParams()
        .label("type", "humidity").label("sensor_id", "1");

    exec(commandObjects.tsCreate(key3, paramsHumiditySensor1));

    String[] filters = new String[]{ "type=temperature" };
    List<String> matchingKeys = exec(commandObjects.tsQueryIndex(filters));

    assertThat(matchingKeys, containsInAnyOrder(key1, key2));
  }

  @Test
  public void testTsAlterAndInfo() {
    String key = "tsKey";

    TSCreateParams createParams = new TSCreateParams()
        .label("sensor", "temperature");

    TSAlterParams alterParams = new TSAlterParams()
        .label("sensor", "humidity");

    String create = exec(commandObjects.tsCreate(key, createParams));
    assertThat(create, equalTo("OK"));

    TSInfo info = exec(commandObjects.tsInfo(key));

    assertThat(info, notNullValue());
    assertThat(info.getLabels().get("sensor"), equalTo("temperature"));
    assertThat(info.getChunks(), nullValue());

    TSInfo debugInfo = exec(commandObjects.tsInfoDebug(key));

    assertThat(debugInfo, notNullValue());
    assertThat(debugInfo.getLabels().get("sensor"), equalTo("temperature"));
    assertThat(debugInfo.getChunks(), notNullValue());

    String alter = exec(commandObjects.tsAlter(key, alterParams));

    assertThat(alter, equalTo("OK"));

    TSInfo infoAfter = exec(commandObjects.tsInfo(key));

    assertThat(infoAfter, notNullValue());
    assertThat(infoAfter.getLabels().get("sensor"), equalTo("humidity"));
    assertThat(infoAfter.getChunks(), nullValue());

    TSInfo debugInfoAfter = exec(commandObjects.tsInfoDebug(key));

    assertThat(debugInfoAfter, notNullValue());
    assertThat(debugInfoAfter.getLabels().get("sensor"), equalTo("humidity"));
    assertThat(debugInfoAfter.getChunks(), notNullValue());
  }
}
