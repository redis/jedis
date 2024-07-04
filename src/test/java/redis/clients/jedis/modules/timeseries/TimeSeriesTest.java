package redis.clients.jedis.modules.timeseries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.util.AssertUtil.assertEqualsByProtocol;

import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.timeseries.*;
import redis.clients.jedis.util.KeyValue;

@RunWith(Parameterized.class)
public class TimeSeriesTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  public TimeSeriesTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testCreate() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");

    assertEquals("OK", client.tsCreate("series1", TSCreateParams.createParams().retention(10).labels(labels)));
    assertEquals("TSDB-TYPE", client.type("series1"));

    assertEquals("OK", client.tsCreate("series2", TSCreateParams.createParams().labels(labels)));
    assertEquals("TSDB-TYPE", client.type("series2"));

    assertEquals("OK", client.tsCreate("series3", TSCreateParams.createParams().retention(10)));
    assertEquals("TSDB-TYPE", client.type("series3"));

    assertEquals("OK", client.tsCreate("series4"));
    assertEquals("TSDB-TYPE", client.type("series4"));

    assertEquals("OK", client.tsCreate("series5", TSCreateParams.createParams().retention(0).uncompressed().labels(labels)));
    assertEquals("TSDB-TYPE", client.type("series5"));
    assertEquals("OK", client.tsCreate("series6", TSCreateParams.createParams().retention(7898)
        .uncompressed().duplicatePolicy(DuplicatePolicy.MAX).labels(labels)));
    assertEquals("TSDB-TYPE", client.type("series6"));

    try {
      assertEquals("OK", client.tsCreate("series1", TSCreateParams.createParams().retention(10).labels(labels)));
      fail();
    } catch (JedisDataException e) {
    }

    try {
      assertEquals("OK", client.tsCreate("series1", TSCreateParams.createParams().labels(labels)));
      fail();
    } catch (JedisDataException e) {
    }

    try {
      assertEquals("OK", client.tsCreate("series1", TSCreateParams.createParams().retention(10)));
      fail();
    } catch (JedisDataException e) {
    }

    try {
      assertEquals("OK", client.tsCreate("series1"));
      fail();
    } catch (JedisDataException e) {
    }

    try {
      assertEquals("OK", client.tsCreate("series1"));
      fail();
    } catch (JedisDataException e) {
    }

    try {
      assertEquals("OK", client.tsCreate("series7", TSCreateParams.createParams().retention(7898)
          .uncompressed().chunkSize(-10).duplicatePolicy(DuplicatePolicy.MAX).labels(labels)));
      fail();
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void testAlter() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesAlter", TSCreateParams.createParams().retention(60000).labels(labels)));
    assertEquals(Collections.emptyList(), client.tsQueryIndex("l2=v22"));

    labels.put("l1", "v11");
    labels.remove("l2");
    labels.put("l3", "v33");
    assertEquals("OK", client.tsAlter("seriesAlter", TSAlterParams.alterParams().retention(15000).chunkSize(8192)
        .duplicatePolicy(DuplicatePolicy.SUM).labels(labels)));

    TSInfo info = client.tsInfo("seriesAlter");
    assertEquals(Long.valueOf(15000), info.getProperty("retentionTime"));
    assertEquals(Long.valueOf(8192), info.getProperty("chunkSize"));
    assertEquals(DuplicatePolicy.SUM, info.getProperty("duplicatePolicy"));
    assertEquals("v11", info.getLabel("l1"));
    assertNull(info.getLabel("l2"));
    assertEquals("v33", info.getLabel("l3"));
  }

  @Test
  public void createAndAlterParams() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");

    assertEquals("OK", client.tsCreate("ts-params",
        TSCreateParams.createParams().retention(60000).encoding(EncodingFormat.UNCOMPRESSED).chunkSize(4096)
            .duplicatePolicy(DuplicatePolicy.BLOCK).ignore(50, 12.5).labels(labels)));

    labels.put("l1", "v11");
    labels.remove("l2");
    labels.put("l3", "v33");
    assertEquals("OK", client.tsAlter("ts-params", TSAlterParams.alterParams().retention(15000).chunkSize(8192)
        .duplicatePolicy(DuplicatePolicy.SUM).ignore(50, 12.5).labels(labels)));
  }

  @Test
  public void testRule() {
    assertEquals("OK", client.tsCreate("source"));
    assertEquals("OK", client.tsCreate("dest", TSCreateParams.createParams().retention(10)));

    assertEquals("OK", client.tsCreateRule("source", "dest", AggregationType.AVG, 100));

    try {
      client.tsCreateRule("source", "dest", AggregationType.COUNT, 100);
      fail();
    } catch (JedisDataException e) {
      // Error on creating same rule twice
    }

    assertEquals("OK", client.tsDeleteRule("source", "dest"));
    assertEquals("OK", client.tsCreateRule("source", "dest", AggregationType.COUNT, 100));

    try {
      assertEquals("OK", client.tsDeleteRule("source", "dest1"));
      fail();
    } catch (JedisDataException e) {
      // Error on creating same rule twice
    }
  }

  @Test
  public void addParams() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");

    assertEquals(1000L, client.tsAdd("add1", 1000L, 1.1,
        TSAddParams.addParams().retention(10000).encoding(EncodingFormat.UNCOMPRESSED).chunkSize(1000)
            .duplicatePolicy(DuplicatePolicy.FIRST).onDuplicate(DuplicatePolicy.LAST).ignore(50, 12.5).labels(labels)));

    assertEquals(1000L, client.tsAdd("add2", 1000L, 1.1,
        TSAddParams.addParams().retention(10000).encoding(EncodingFormat.COMPRESSED).chunkSize(1000)
            .duplicatePolicy(DuplicatePolicy.MIN).onDuplicate(DuplicatePolicy.MAX).ignore(50, 12.5).labels(labels)));
  }

  @Test
  public void testAdd() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesAdd", TSCreateParams.createParams().retention(10000).labels(labels)));
    assertEquals(0, client.tsRange("seriesAdd", TSRangeParams.rangeParams()).size());

    assertEquals(1000L, client.tsAdd("seriesAdd", 1000L, 1.1, TSCreateParams.createParams().retention(10000).labels(null)));
    assertEquals(2000L, client.tsAdd("seriesAdd", 2000L, 0.9, TSCreateParams.createParams().labels(null)));
    assertEquals(3200L, client.tsAdd("seriesAdd", 3200L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(4500L, client.tsAdd("seriesAdd", 4500L, -1.1));

    TSElement[] rawValues = new TSElement[]{
      new TSElement(1000L, 1.1),
      new TSElement(2000L, 0.9),
      new TSElement(3200L, 1.1),
      new TSElement(4500L, -1.1)
    };
    List<TSElement> values = client.tsRange("seriesAdd", 800L, 3000L);
    assertEquals(2, values.size());
    assertEquals(Arrays.asList(rawValues[0], rawValues[1]), values);
    values = client.tsRange("seriesAdd", 800L, 5000L);
    assertEquals(4, values.size());
    assertEquals(Arrays.asList(rawValues), values);
    assertEquals(Arrays.asList(rawValues), client.tsRange("seriesAdd", TSRangeParams.rangeParams()));

    List<TSElement> expectedCountValues = Arrays.asList(
        new TSElement(2000L, 1), new TSElement(3200L, 1), new TSElement(4500L, 1));
    values = client.tsRange("seriesAdd", TSRangeParams.rangeParams(1200L, 4600L).aggregation(AggregationType.COUNT, 1));
    assertEquals(3, values.size());
    assertEquals(expectedCountValues, values);

    List<TSElement> expectedAvgValues = Arrays.asList(
        new TSElement(0L, 1.1), new TSElement(2000L, 1), new TSElement(4000L, -1.1));
    values = client.tsRange("seriesAdd", TSRangeParams.rangeParams(500L, 4600L).aggregation(AggregationType.AVG, 2000L));
    assertEquals(3, values.size());
    assertEquals(expectedAvgValues, values);

    // ensure zero-based index
    List<TSElement> valuesZeroBased = client.tsRange("seriesAdd",
        TSRangeParams.rangeParams(0L, 4600L).aggregation(AggregationType.AVG, 2000L));
    assertEquals(3, valuesZeroBased.size());
    assertEquals(values, valuesZeroBased);

    List<TSElement> expectedOverallSumValues = Arrays.asList(new TSElement(0L, 2.0));
    values = client.tsRange("seriesAdd", TSRangeParams.rangeParams(0L, 5000L).aggregation(AggregationType.SUM, 5000L));
    assertEquals(1, values.size());
    assertEquals(expectedOverallSumValues, values);

    List<TSElement> expectedOverallMinValues = Arrays.asList(new TSElement(0L, -1.1));
    values = client.tsRange("seriesAdd", TSRangeParams.rangeParams(0L, 5000L).aggregation(AggregationType.MIN, 5000L));
    assertEquals(1, values.size());
    assertEquals(expectedOverallMinValues, values);

    List<TSElement> expectedOverallMaxValues = Arrays.asList(new TSElement(0L, 1.1));
    values = client.tsRange("seriesAdd", TSRangeParams.rangeParams(0L, 5000L).aggregation(AggregationType.MAX, 5000L));
    assertEquals(1, values.size());
    assertEquals(expectedOverallMaxValues, values);

    // MRANGE
    assertEquals(Collections.emptyMap(), client.tsMRange(TSMRangeParams.multiRangeParams().filter("l=v")));
    try {
      client.tsMRange(TSMRangeParams.multiRangeParams(500L, 4600L).aggregation(AggregationType.COUNT, 1));
      fail();
//    } catch (JedisDataException e) {
    } catch (IllegalArgumentException e) {
    }

    try {
      client.tsMRange(TSMRangeParams.multiRangeParams(500L, 4600L).aggregation(AggregationType.COUNT, 1).filter((String) null));
      fail();
//    } catch (JedisDataException e) {
    } catch (IllegalArgumentException e) {
    }

    Map<String, TSMRangeElements> ranges = client.tsMRange(TSMRangeParams.multiRangeParams(500L, 4600L)
        .aggregation(AggregationType.COUNT, 1).filter("l1=v1"));
    assertEquals(1, ranges.size());

    TSMRangeElements range = ranges.values().stream().findAny().get();
    assertEquals("seriesAdd", range.getKey());
    assertEquals(Collections.emptyMap(), range.getLabels());

    List<TSElement> rangeValues = range.getValue();
    assertEquals(4, rangeValues.size());
    assertEquals(new TSElement(1000, 1), rangeValues.get(0));
    assertNotEquals(new TSElement(1000, 1.1), rangeValues.get(0));
    assertEquals(2000L, rangeValues.get(1).getTimestamp());
    assertEquals("(2000:1.0)", rangeValues.get(1).toString());

    // Add with labels
    Map<String, String> labels2 = new HashMap<>();
    labels2.put("l3", "v3");
    labels2.put("l4", "v4");
    assertEquals(1000L, client.tsAdd("seriesAdd2", 1000L, 1.1, TSCreateParams.createParams().retention(10000).labels(labels2)));
    Map<String, TSMRangeElements> ranges2 = client.tsMRange(TSMRangeParams.multiRangeParams(500L, 4600L)
        .aggregation(AggregationType.COUNT, 1).withLabels().filter("l4=v4"));
    assertEquals(1, ranges2.size());
    TSMRangeElements elements2 = ranges2.values().stream().findAny().get();
    assertEquals(labels2, elements2.getLabels());
    assertEqualsByProtocol(protocol, null, Arrays.asList(AggregationType.COUNT), elements2.getAggregators());

    Map<String, String> labels3 = new HashMap<>();
    labels3.put("l3", "v33");
    labels3.put("l4", "v4");
    assertEquals(1000L, client.tsAdd("seriesAdd3", 1000L, 1.1, TSCreateParams.createParams().labels(labels3)));
    assertEquals(2000L, client.tsAdd("seriesAdd3", 2000L, 1.1, TSCreateParams.createParams().labels(labels3)));
    assertEquals(3000L, client.tsAdd("seriesAdd3", 3000L, 1.1, TSCreateParams.createParams().labels(labels3)));
    Map<String, TSMRangeElements> ranges3 = client.tsMRange(TSMRangeParams.multiRangeParams(500L, 4600L)
        .aggregation(AggregationType.AVG, 1L).withLabels(true).count(2).filter("l4=v4"));
    assertEquals(2, ranges3.size());
    ArrayList<TSMRangeElements> ranges3List = new ArrayList<>(ranges3.values());
    assertEquals(1, ranges3List.get(0).getValue().size());
    assertEquals(labels2, ranges3List.get(0).getLabels());
    assertEqualsByProtocol(protocol, null, Arrays.asList(AggregationType.AVG), ranges3List.get(0).getAggregators());
    assertEquals(2, ranges3List.get(1).getValue().size());
    assertEquals(labels3, ranges3List.get(1).getLabels());
    assertEqualsByProtocol(protocol, null, Arrays.asList(AggregationType.AVG), ranges3List.get(1).getAggregators());

    assertEquals(800L, client.tsAdd("seriesAdd", 800L, 1.1));
    assertEquals(700L, client.tsAdd("seriesAdd", 700L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(600L, client.tsAdd("seriesAdd", 600L, 1.1, TSCreateParams.createParams().retention(10000).labels(null)));

    assertEquals(400L, client.tsAdd("seriesAdd4", 400L, 0.4, TSCreateParams.createParams()
        .retention(7898L).uncompressed().chunkSize(1000L).duplicatePolicy(DuplicatePolicy.SUM)
        .labels(labels)));
    assertEquals("TSDB-TYPE", client.type("seriesAdd4"));
    assertEquals(400L, client.tsAdd("seriesAdd4", 400L, 0.3, TSCreateParams.createParams()
        .retention(7898L).uncompressed().chunkSize(1000L).duplicatePolicy(DuplicatePolicy.SUM)
        .labels(labels)));
    assertEquals(Arrays.asList(new TSElement(400L, 0.7)), client.tsRange("seriesAdd4", 0L, Long.MAX_VALUE));

    // Range on none existing key
    try {
      client.tsRange("seriesAdd1", TSRangeParams.rangeParams(500L, 4000L).aggregation(AggregationType.COUNT, 1));
      fail();
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void issue75() {
    client.tsMRange(TSMRangeParams.multiRangeParams().filter("id=1"));
  }

  @Test
  public void del() {
    try {
      client.tsDel("ts-del", 0, 1);
      fail();
    } catch (JedisDataException jde) {
      // expected
    }

    assertEquals("OK", client.tsCreate("ts-del", TSCreateParams.createParams().retention(10000L)));
    assertEquals(0, client.tsDel("ts-del", 0, 1));

    assertEquals(1000L, client.tsAdd("ts-del", 1000L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(2000L, client.tsAdd("ts-del", 2000L, 0.9));
    assertEquals(3200L, client.tsAdd("ts-del", 3200L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(4500L, client.tsAdd("ts-del", 4500L, -1.1));
    assertEquals(4, client.tsRange("ts-del", 0, 5000).size());

    assertEquals(2, client.tsDel("ts-del", 2000, 4000));
    assertEquals(2, client.tsRange("ts-del", 0, 5000).size());
    assertEquals(1, client.tsRange("ts-del", 0, 2500).size());
    assertEquals(1, client.tsRange("ts-del", 2500, 5000).size());
  }

  @Test
  public void testValue() {
    TSElement v = new TSElement(1234, 234.89634);
    assertEquals(1234, v.getTimestamp());
    assertEquals(234.89634, v.getValue(), 0);

    assertEquals(v, new TSElement(1234, 234.89634));
    assertNotEquals(v, new TSElement(1334, 234.89634));
    assertNotEquals(v, new TSElement(1234, 234.8934));
    assertNotEquals(1234, v.getValue());

    assertEquals("(1234:234.89634)", v.toString());
//    assertEquals(-1856758940, v.hashCode());
    assertEquals(-1856719580, v.hashCode());
  }

  @Test
  public void testAddStar() throws InterruptedException {
    Map<String, String> labels = new HashMap<>();
    labels.put("l11", "v11");
    labels.put("l22", "v22");
    assertEquals("OK", client.tsCreate("seriesAdd2", TSCreateParams.createParams().retention(10000L).labels(labels)));

    long startTime = System.currentTimeMillis();
    Thread.sleep(1);
//    long add1 = client.tsAdd("seriesAdd2", 1.1, 10000);
    long add1 = client.tsAdd("seriesAdd2", 1.1);
    assertTrue(add1 > startTime);
    Thread.sleep(1);
    long add2 = client.tsAdd("seriesAdd2", 3.2);
    assertTrue(add2 > add1);
    Thread.sleep(1);
    long add3 = client.tsAdd("seriesAdd2", 3.2);
    assertTrue(add3 > add2);
    Thread.sleep(1);
    long add4 = client.tsAdd("seriesAdd2", -1.2);
    assertTrue(add4 > add3);
    Thread.sleep(1);
    long endTime = System.currentTimeMillis();
    assertTrue(endTime > add4);

    List<TSElement> values = client.tsRange("seriesAdd2", startTime, add3);
    assertEquals(3, values.size());
  }

  @Test
  public void testMadd() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesAdd1", TSCreateParams.createParams().retention(10000L).labels(labels)));
    assertEquals("OK", client.tsCreate("seriesAdd2", TSCreateParams.createParams().retention(10000L).labels(labels)));

    List<Long> result = client.tsMAdd(
        new KeyValue<>("seriesAdd1", new TSElement(1000L, 1.1)),
        new KeyValue<>("seriesAdd2", new TSElement(2000L, 3.2)),
        new KeyValue<>("seriesAdd1", new TSElement(1500L, 2.67)),
        new KeyValue<>("seriesAdd2", new TSElement(3200L, 54.2)),
        new KeyValue<>("seriesAdd2", new TSElement(4300L, 21.2)));

    assertEquals(1000L, result.get(0).longValue());
    assertEquals(2000L, result.get(1).longValue());
    assertEquals(1500L, result.get(2).longValue());
    assertEquals(3200L, result.get(3).longValue());
    assertEquals(4300L, result.get(4).longValue());

    List<TSElement> values1 = client.tsRange("seriesAdd1", 0, Long.MAX_VALUE);
    assertEquals(2, values1.size());
    assertEquals(1.1, values1.get(0).getValue(), 0.001);
    assertEquals(2.67, values1.get(1).getValue(), 0.001);

    List<TSElement> values2 = client.tsRange("seriesAdd2", TSRangeParams.rangeParams(0, Long.MAX_VALUE).count(2));
    assertEquals(2, values2.size());
    assertEquals(3.2, values2.get(0).getValue(), 0.001);
    assertEquals(54.2, values2.get(1).getValue(), 0.001);
  }

  @Test
  public void testIncrByDecrBy() throws InterruptedException {
    assertEquals("OK", client.tsCreate("seriesIncDec",
        TSCreateParams.createParams().retention(100 * 1000 /*100 sec*/)));

    assertEquals(1L, client.tsAdd("seriesIncDec", 1L, 1), 0);
    assertEquals(2L, client.tsIncrBy("seriesIncDec", 3, 2L), 0);
    assertEquals(3L, client.tsDecrBy("seriesIncDec", 2, 3L), 0);
    List<TSElement> values = client.tsRange("seriesIncDec", 1L, 3L);
    assertEquals(3, values.size());
    assertEquals(2, values.get(2).getValue(), 0);

    assertEquals(3L, client.tsDecrBy("seriesIncDec", 2, 3L), 0);
    values = client.tsRange("seriesIncDec", 1L, Long.MAX_VALUE);
    assertEquals(3, values.size());

    client.tsIncrBy("seriesIncDec", 100);
    client.tsDecrBy("seriesIncDec", 33);
  }

  @Test
  public void incrByDecrByParams() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");

    assertEquals(1000L, client.tsIncrBy("incr1", 1.1,
        TSIncrByParams.incrByParams().timestamp(1000).retention(10000).encoding(EncodingFormat.UNCOMPRESSED)
            .chunkSize(1000).duplicatePolicy(DuplicatePolicy.FIRST).ignore(50, 12.5).labels(labels)));

    assertEquals(1000L, client.tsIncrBy("incr2", 1.1,
        TSIncrByParams.incrByParams().timestamp(1000).retention(10000).encoding(EncodingFormat.COMPRESSED)
            .chunkSize(1000).duplicatePolicy(DuplicatePolicy.MIN).ignore(50, 12.5).labels(labels)));

    assertEquals(1000L, client.tsDecrBy("decr1", 1.1,
        TSDecrByParams.decrByParams().timestamp(1000).retention(10000).encoding(EncodingFormat.COMPRESSED)
            .chunkSize(1000).duplicatePolicy(DuplicatePolicy.LAST).ignore(50, 12.5).labels(labels)));

    assertEquals(1000L, client.tsDecrBy("decr2", 1.1,
        TSDecrByParams.decrByParams().timestamp(1000).retention(10000).encoding(EncodingFormat.UNCOMPRESSED)
            .chunkSize(1000).duplicatePolicy(DuplicatePolicy.MAX).ignore(50, 12.5).labels(labels)));
  }

  @Test
  public void align() {
    client.tsAdd("align", 1, 10d);
    client.tsAdd("align", 3, 5d);
    client.tsAdd("align", 11, 10d);
    client.tsAdd("align", 25, 11d);

    List<TSElement> values = client.tsRange("align", TSRangeParams.rangeParams(1L, 30L).aggregation(AggregationType.COUNT, 10));
    assertEquals(Arrays.asList(new TSElement(1, 2), new TSElement(11, 1), new TSElement(21, 1)), values);

    values = client.tsRange("align", TSRangeParams.rangeParams(1L, 30L).alignStart().aggregation(AggregationType.COUNT, 10));
    assertEquals(Arrays.asList(new TSElement(1, 2), new TSElement(11, 1), new TSElement(21, 1)), values);

    values = client.tsRange("align", TSRangeParams.rangeParams(1L, 30L).alignEnd().aggregation(AggregationType.COUNT, 10));
    assertEquals(Arrays.asList(new TSElement(1, 2), new TSElement(11, 1), new TSElement(21, 1)), values);

    values =
        client.tsRange("align", TSRangeParams.rangeParams(1L, 30L).align(5).aggregation(AggregationType.COUNT, 10));
    assertEquals(Arrays.asList(new TSElement(1, 2), new TSElement(11, 1), new TSElement(21, 1)), values);
  }

  @Test
  public void rangeFilterBy() {

    TSElement[] rawValues =
        new TSElement[] {
          new TSElement(1000L, 1.0),
          new TSElement(2000L, 0.9),
          new TSElement(3200L, 1.1),
          new TSElement(4500L, -1.1)
        };

    for (TSElement value : rawValues) {
      client.tsAdd("filterBy", value.getTimestamp(), value.getValue());
    }

    // RANGE
    List<TSElement> values = client.tsRange("filterBy", 0L, 5000L);
    assertEquals(Arrays.asList(rawValues), values);

    values = client.tsRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByTS(1000L, 2000L));
    assertEquals(Arrays.asList(rawValues[0], rawValues[1]), values);

    values = client.tsRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByValues(1.0, 1.2));
    assertEquals(Arrays.asList(rawValues[0], rawValues[2]), values);

    values = client.tsRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByTS(1000L, 2000L).filterByValues(1.0, 1.2));
    assertEquals(Arrays.asList(rawValues[0]), values);

    // REVRANGE
    values = client.tsRevRange("filterBy", 0L, 5000L);
    assertEquals(Arrays.asList(rawValues[3], rawValues[2], rawValues[1], rawValues[0]), values);

    values =
        client.tsRevRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByTS(1000L, 2000L));
    assertEquals(Arrays.asList(rawValues[1], rawValues[0]), values);

    values =
        client.tsRevRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByValues(1.0, 1.2));
    assertEquals(Arrays.asList(rawValues[2], rawValues[0]), values);

    values =
        client.tsRevRange("filterBy", TSRangeParams.rangeParams(0L, 5000L).filterByTS(1000L, 2000L).filterByValues(1.0, 1.2));
    assertEquals(Arrays.asList(rawValues[0]), values);
  }

  @Test
  public void mrangeFilterBy() {

    Map<String, String> labels = Collections.singletonMap("label", "multi");
    client.tsCreate("ts1", TSCreateParams.createParams().labels(labels));
    client.tsCreate("ts2", TSCreateParams.createParams().labels(labels));
    String filter = "label=multi";

    TSElement[] rawValues = new TSElement[]{
      new TSElement(1000L, 1.0),
      new TSElement(2000L, 0.9),
      new TSElement(3200L, 1.1),
      new TSElement(4500L, -1.1)
    };

    client.tsAdd("ts1", rawValues[0].getTimestamp(), rawValues[0].getValue());
    client.tsAdd("ts2", rawValues[1].getTimestamp(), rawValues[1].getValue());
    client.tsAdd("ts2", rawValues[2].getTimestamp(), rawValues[2].getValue());
    client.tsAdd("ts1", rawValues[3].getTimestamp(), rawValues[3].getValue());

    // MRANGE
    Map<String, TSMRangeElements> range = client.tsMRange(0L, 5000L, filter);
    ArrayList<TSMRangeElements> rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[0], rawValues[3]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[1], rawValues[2]), rangeList.get(1).getValue());

    range = client.tsMRange(TSMRangeParams.multiRangeParams(0L, 5000L).filterByTS(1000L, 2000L).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[1]), rangeList.get(1).getValue());

    range = client.tsMRange(TSMRangeParams.multiRangeParams(0L, 5000L).filterByValues(1.0, 1.2).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[2]), rangeList.get(1).getValue());

    range = client.tsMRange(TSMRangeParams.multiRangeParams(0L, 5000L)
        .filterByTS(1000L, 2000L).filterByValues(1.0, 1.2).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());

    // MREVRANGE
    range = client.tsMRevRange(0L, 5000L,  filter);
    rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[3], rawValues[0]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[2], rawValues[1]), rangeList.get(1).getValue());

    range = client.tsMRevRange(TSMRangeParams.multiRangeParams(0L, 5000L).filterByTS(1000L, 2000L).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[1]), rangeList.get(1).getValue());

    range = client.tsMRevRange(TSMRangeParams.multiRangeParams(0L, 5000L).filterByValues(1.0, 1.2).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals("ts1", rangeList.get(0).getKey());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());
    assertEquals("ts2", rangeList.get(1).getKey());
    assertEquals(Arrays.asList(rawValues[2]), rangeList.get(1).getValue());

    range = client.tsMRevRange(TSMRangeParams.multiRangeParams(0L, 5000L)
        .filterByTS(1000L, 2000L).filterByValues(1.0, 1.2).filter(filter));
    rangeList = new ArrayList<>(range.values());
    assertEquals(Arrays.asList(rawValues[0]), rangeList.get(0).getValue());
  }

  @Test
  public void groupByReduce() {
    client.tsCreate("ts1", TSCreateParams.createParams().labels(convertMap("metric", "cpu", "metric_name", "system")));
    client.tsCreate("ts2", TSCreateParams.createParams().labels(convertMap("metric", "cpu", "metric_name", "user")));

    client.tsAdd("ts1", 1L, 90.0);
    client.tsAdd("ts1", 2L, 45.0);
    client.tsAdd("ts2", 2L, 99.0);

//    List<TSElements> range = client.tsMRange(TSMRangeParams.multiGetParams(0L, 100L).withLabels()
//        .groupByReduce("metric_name", "max"), "metric=cpu");
    Map<String, TSMRangeElements> range = client.tsMRange(TSMRangeParams.multiRangeParams(0L, 100L).withLabels()
        .filter("metric=cpu").groupBy("metric_name", "max"));
    assertEquals(2, range.size());
    ArrayList<TSMRangeElements> rangeList = new ArrayList<>(range.values());

    assertEquals("metric_name=system", rangeList.get(0).getKey());
    assertEquals("system", rangeList.get(0).getLabels().get("metric_name"));
    if (protocol != RedisProtocol.RESP3) {
      assertEquals("max", rangeList.get(0).getLabels().get("__reducer__"));
      assertEquals("ts1", rangeList.get(0).getLabels().get("__source__"));
    } else {
      assertEquals(Arrays.asList("max"), rangeList.get(0).getReducers());
      assertEquals(Arrays.asList("ts1"), rangeList.get(0).getSources());
    }
    assertEquals(Arrays.asList(new TSElement(1, 90), new TSElement(2, 45)), rangeList.get(0).getValue());

    assertEquals("metric_name=user", rangeList.get(1).getKey());
    assertEquals("user", rangeList.get(1).getLabels().get("metric_name"));
    if (protocol != RedisProtocol.RESP3) {
      assertEquals("max", rangeList.get(1).getLabels().get("__reducer__"));
      assertEquals("ts2", rangeList.get(1).getLabels().get("__source__"));
    } else {
      assertEquals(Arrays.asList("max"), rangeList.get(1).getReducers());
      assertEquals(Arrays.asList("ts2"), rangeList.get(1).getSources());
    }
    assertEquals(Arrays.asList(new TSElement(2, 99)), rangeList.get(1).getValue());
  }

  private Map<String, String> convertMap(String... array) {
    Map<String, String> map = new HashMap<>(array.length / 2);
    for (int i = 0; i < array.length; i += 2) {
      map.put(array[i], array[i + 1]);
    }
    return map;
  }

  @Test
  public void testGet() {

    // Test for empty result none existing series
    try {
      client.tsGet("seriesGet");
      fail();
    } catch (JedisDataException e) {
    }

    assertEquals("OK", client.tsCreate("seriesGet", TSCreateParams.createParams()
        .retention(100 * 1000 /*100sec retentionTime*/)));

    // Test for empty result
    assertNull(client.tsGet("seriesGet"));

    // Test returned last Value
    client.tsAdd("seriesGet", 2558, 8.7);
    assertEquals(new TSElement(2558, 8.7), client.tsGet("seriesGet"));

    client.tsAdd("seriesGet", 3458, 1.117);
    assertEquals(new TSElement(3458, 1.117), client.tsGet("seriesGet"));
  }

  @Test
  public void testMGet() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesMGet1", TSCreateParams.createParams()
        .retention(100 * 1000 /*100sec retentionTime*/).labels(labels)));
    assertEquals("OK", client.tsCreate("seriesMGet2", TSCreateParams.createParams()
        .retention(100 * 1000 /*100sec retentionTime*/).labels(labels)));

    // Test for empty result
    Map<String, TSMGetElement> ranges1 = client.tsMGet(TSMGetParams.multiGetParams().withLabels(false), "l1=v2");
    assertEquals(0, ranges1.size());

    // Test for empty ranges
    Map<String, TSMGetElement> ranges2 = client.tsMGet(TSMGetParams.multiGetParams().withLabels(true), "l1=v1");
    assertEquals(2, ranges2.size());
    ArrayList<TSMGetElement> ranges2List = new ArrayList<>(ranges2.values());
    assertEquals(labels, ranges2List.get(0).getLabels());
    assertEquals(labels, ranges2List.get(1).getLabels());
    assertNull(ranges2List.get(0).getValue());

    // Test for returned result on MGet
    client.tsAdd("seriesMGet1", 1500, 1.3);
    Map<String, TSMGetElement> ranges3 = client.tsMGet(TSMGetParams.multiGetParams().withLabels(false), "l1=v1");
    assertEquals(2, ranges3.size());
    ArrayList<TSMGetElement> ranges3List = new ArrayList<>(ranges3.values());
    assertEquals(Collections.emptyMap(), ranges3List.get(0).getLabels());
    assertEquals(Collections.emptyMap(), ranges3List.get(1).getLabels());
    assertEquals(new TSElement(1500, 1.3), ranges3List.get(0).getValue());
    assertNull(ranges3List.get(1).getValue());
  }

  @Test
  public void testQueryIndex() {

    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesQueryIndex1", TSCreateParams.createParams()
        .retention(100 * 1000 /*100sec retentionTime*/).labels(labels)));

    labels.put("l2", "v22");
    labels.put("l3", "v33");
    assertEquals("OK", client.tsCreate("seriesQueryIndex2", TSCreateParams.createParams()
        .retention(100 * 1000 /*100sec retentionTime*/).labels(labels)));

    assertEquals(Arrays.<String>asList(), client.tsQueryIndex("l1=v2"));
    assertEquals(Arrays.asList("seriesQueryIndex1", "seriesQueryIndex2"), client.tsQueryIndex("l1=v1"));
    assertEquals(Arrays.asList("seriesQueryIndex2"), client.tsQueryIndex("l2=v22"));
  }

  @Test
  public void testInfo() {
    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("source", TSCreateParams.createParams().retention(10000L).labels(labels)));
    assertEquals("OK", client.tsCreate("dest", TSCreateParams.createParams().retention(20000L)));
    assertEquals("OK", client.tsCreateRule("source", "dest", AggregationType.AVG, 100));

    TSInfo info = client.tsInfo("source");
    assertEquals((Long) 10000L, info.getProperty("retentionTime"));
    assertEquals((Long) 4096L, info.getProperty("chunkSize"));
    assertEquals("v1", info.getLabel("l1"));
    assertEquals("v2", info.getLabel("l2"));
    assertNull(info.getLabel("l3"));

    assertEquals(1, info.getRules().size());
    TSInfo.Rule rule = info.getRule("dest");
    assertEquals("dest", rule.getCompactionKey());
    assertEquals(100L, rule.getBucketDuration());
    assertEquals(AggregationType.AVG, rule.getAggregator());

    try {
      client.tsInfo("none");
      fail();
    } catch (JedisDataException e) {
      // Error on info on none existing series
    }
  }

  @Test
  public void testInfoDebug() {
    assertEquals("OK", client.tsCreate("source", TSCreateParams.createParams()));

    TSInfo info = client.tsInfoDebug("source");
    assertEquals((Long) 0L, info.getProperty("retentionTime"));
    assertEquals(0, info.getLabels().size());
    assertEquals(0, info.getRules().size());

    List<Map<String, Object>> chunks = info.getChunks();
    assertEquals(1, chunks.size());
    Map<String, Object> chunk = chunks.get(0);
    assertEquals(0L, chunk.get("samples"));
    // Don't care what the values are as long as the values are parsed according to types
    assertTrue(chunk.get("size") instanceof Long);
    assertTrue(chunk.get("startTimestamp") instanceof Long);
    assertTrue(chunk.get("endTimestamp") instanceof Long);
    assertTrue(chunk.get("bytesPerSample") instanceof Double);

    try {
      client.tsInfoDebug("none");
      fail();
    } catch (JedisDataException e) {
      // Error on info on none existing series
    }
  }

  @Test
  public void testRevRange() {

    Map<String, String> labels = new HashMap<>();
    labels.put("l1", "v1");
    labels.put("l2", "v2");
    assertEquals("OK", client.tsCreate("seriesAdd", TSCreateParams.createParams().retention(10000L).labels(labels)));
    assertEquals(Collections.emptyList(), client.tsRevRange("seriesAdd", TSRangeParams.rangeParams()));

    assertEquals(1000L, client.tsAdd("seriesRevRange", 1000L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(2000L, client.tsAdd("seriesRevRange", 2000L, 0.9, TSCreateParams.createParams().labels(null)));
    assertEquals(3200L, client.tsAdd("seriesRevRange", 3200L, 1.1, TSCreateParams.createParams().retention(10000)));
    assertEquals(4500L, client.tsAdd("seriesRevRange", 4500L, -1.1));

    TSElement[] rawValues = new TSElement[]{
      new TSElement(4500L, -1.1),
      new TSElement(3200L, 1.1),
      new TSElement(2000L, 0.9),
      new TSElement(1000L, 1.1)
    };
    List<TSElement> values = client.tsRevRange("seriesRevRange", 800L, 3000L);
    assertEquals(2, values.size());
    assertEquals(Arrays.asList(Arrays.copyOfRange(rawValues, 2, 4)), values);
    values = client.tsRevRange("seriesRevRange", 800L, 5000L);
    assertEquals(4, values.size());
    assertEquals(Arrays.asList(rawValues), values);
    assertEquals(Arrays.asList(rawValues), client.tsRevRange("seriesRevRange", TSRangeParams.rangeParams()));

    List<TSElement> expectedCountValues = Arrays.asList(
        new TSElement(4500L, 1), new TSElement(3200L, 1), new TSElement(2000L, 1));
    values = client.tsRevRange("seriesRevRange", TSRangeParams.rangeParams(1200L, 4600L)
        .aggregation(AggregationType.COUNT, 1));
    assertEquals(3, values.size());
    assertEquals(expectedCountValues, values);

    List<TSElement> expectedAvgValues = Arrays.asList(
        new TSElement(4000L, -1.1), new TSElement(2000L, 1), new TSElement(0L, 1.1));
    values = client.tsRevRange("seriesRevRange", TSRangeParams.rangeParams(500L, 4600L)
        .aggregation(AggregationType.AVG, 2000L));
    assertEquals(3, values.size());
    assertEquals(expectedAvgValues, values);
  }

  @Test
  public void testMRevRange() {

    assertEquals(Collections.emptyMap(), client.tsMRevRange(TSMRangeParams.multiRangeParams().filter("l=v")));

    Map<String, String> labels1 = new HashMap<>();
    labels1.put("l3", "v3");
    labels1.put("l4", "v4");
    assertEquals(1000L, client.tsAdd("seriesMRevRange1", 1000L, 1.1,
        TSCreateParams.createParams().retention(10000).labels(labels1)));
    assertEquals(2222L, client.tsAdd("seriesMRevRange1", 2222L, 3.1,
        TSCreateParams.createParams().retention(10000).labels(labels1)));
    Map<String, TSMRangeElements> ranges1 = client.tsMRevRange(TSMRangeParams.multiRangeParams(500L, 4600L)
        .aggregation(AggregationType.COUNT, 1).withLabels().filter("l4=v4"));
    assertEquals(1, ranges1.size());
    ArrayList<TSMRangeElements> ranges1List = new ArrayList<>(ranges1.values());
    assertEquals(labels1, ranges1List.get(0).getLabels());
    assertEquals(Arrays.asList(new TSElement(2222L, 1.0), new TSElement(1000L, 1.0)), ranges1List.get(0).getValue());

    Map<String, String> labels2 = new HashMap<>();
    labels2.put("l3", "v3");
    labels2.put("l4", "v44");
    assertEquals(1000L, client.tsAdd("seriesMRevRange2", 1000L, 8.88,
        TSCreateParams.createParams().retention(10000).labels(labels2)));
    assertEquals(1111L, client.tsAdd("seriesMRevRange2", 1111L, 99.99,
        TSCreateParams.createParams().retention(10000).labels(labels2)));
    Map<String, TSMRangeElements> ranges2 = client.tsMRevRange(500L, 4600L, "l3=v3");
    assertEquals(2, ranges2.size());
    ArrayList<TSMRangeElements> ranges2List = new ArrayList<>(ranges2.values());
    assertEquals(Collections.emptyMap(), ranges2List.get(0).getLabels());
    assertEquals(Arrays.asList(new TSElement(2222L, 3.1), new TSElement(1000L, 1.1)), ranges2List.get(0).getValue());
    assertEquals(Collections.emptyMap(), ranges2List.get(0).getLabels());
    assertEquals(Arrays.asList(new TSElement(1111L, 99.99), new TSElement(1000L, 8.88)), ranges2List.get(1).getValue());

    Map<String, String> labels3 = new HashMap<>();
    labels3.put("l3", "v33");
    labels3.put("l4", "v4");
    assertEquals(2200L, client.tsAdd("seriesMRevRange3", 2200L, -1.1, TSCreateParams.createParams().labels(labels3)));
    assertEquals(2400L, client.tsAdd("seriesMRevRange3", 2400L, 1.1, TSCreateParams.createParams().labels(labels3)));
    assertEquals(3300L, client.tsAdd("seriesMRevRange3", 3300L, -33, TSCreateParams.createParams().labels(labels3)));
    Map<String, TSMRangeElements> ranges3 = client.tsMRevRange(TSMRangeParams.multiRangeParams(500L, 4600L)
        .aggregation(AggregationType.AVG, 500).withLabels().count(5).filter("l4=v4"));
    assertEquals(2, ranges3.size());
    ArrayList<TSMRangeElements> ranges3List = new ArrayList<>(ranges3.values());
    assertEquals(labels1, ranges3List.get(0).getLabels());
    assertEquals(Arrays.asList(new TSElement(2000L, 3.1), new TSElement(1000L, 1.1)), ranges3List.get(0).getValue());
    assertEquals(labels3, ranges3List.get(1).getLabels());
    assertEquals(Arrays.asList(new TSElement(3000L, -33.0), new TSElement(2000L, 0.0)), ranges3List.get(1).getValue());
  }

  @Test
  public void latest() {
    client.tsCreate("ts1");
    client.tsCreate("ts2");
    client.tsCreateRule("ts1", "ts2", AggregationType.SUM, 10);
    client.tsAdd("ts1", 1, 1);
    client.tsAdd("ts1", 2, 3);
    client.tsAdd("ts1", 11, 7);
    client.tsAdd("ts1", 13, 1);
    List<TSElement> range = client.tsRange("ts1", 0, 20);
    assertEquals(4, range.size());

    final TSElement compact = new TSElement(0, 4);
    final TSElement latest = new TSElement(10, 8);

    // get
    assertEquals(compact, client.tsGet("ts2", TSGetParams.getParams()));

    assertEquals(latest, client.tsGet("ts2", TSGetParams.getParams().latest()));

    // range
    assertEquals(Arrays.asList(compact), client.tsRange("ts2", TSRangeParams.rangeParams(0, 10)));

    assertEquals(Arrays.asList(compact, latest), client.tsRange("ts2", TSRangeParams.rangeParams(0, 10).latest()));

    // revrange
    assertEquals(Arrays.asList(compact), client.tsRevRange("ts2", TSRangeParams.rangeParams(0, 10)));

    assertEquals(Arrays.asList(latest, compact), client.tsRevRange("ts2", TSRangeParams.rangeParams(0, 10).latest()));
  }

  @Test
  public void latestMulti() {
    client.tsCreate("ts1");
    client.tsCreate("ts2", TSCreateParams.createParams().label("compact", "true"));
    client.tsCreateRule("ts1", "ts2", AggregationType.SUM, 10);
    client.tsAdd("ts1", 1, 1);
    client.tsAdd("ts1", 2, 3);
    client.tsAdd("ts1", 11, 7);
    client.tsAdd("ts1", 13, 1);
    List<TSElement> range = client.tsRange("ts1", 0, 20);
    assertEquals(4, range.size());

    final TSElement compact = new TSElement(0, 4);
    final TSElement latest = new TSElement(10, 8);

    // mget
    assertEquals(makeSingletonMap(new TSMGetElement("ts2", null, compact)),
        client.tsMGet(TSMGetParams.multiGetParams(), "compact=true"));

    assertEquals(makeSingletonMap(new TSMGetElement("ts2", null, latest)),
        client.tsMGet(TSMGetParams.multiGetParams().latest(), "compact=true"));

    // mrange
    assertEquals(makeSingletonMap(new TSMRangeElements("ts2", null, Arrays.asList(compact))),
        client.tsMRange(TSMRangeParams.multiRangeParams().filter("compact=true")));

    assertEquals(makeSingletonMap(new TSMRangeElements("ts2", null, Arrays.asList(compact, latest))),
        client.tsMRange(TSMRangeParams.multiRangeParams().latest().filter("compact=true")));

    // mrevrange
    assertEquals(makeSingletonMap(new TSMRangeElements("ts2", null, Arrays.asList(compact))),
        client.tsMRevRange(TSMRangeParams.multiRangeParams().filter("compact=true")));

    assertEquals(makeSingletonMap(new TSMRangeElements("ts2", null, Arrays.asList(latest, compact))),
        client.tsMRevRange(TSMRangeParams.multiRangeParams().latest().filter("compact=true")));
  }

  private Map<String, TSMGetElement> makeSingletonMap(TSMGetElement value) {
    return Collections.singletonMap(value.getKey(), value);
  }

  private Map<String, TSMRangeElements> makeSingletonMap(TSMRangeElements value) {
    return Collections.singletonMap(value.getKey(), value);
  }

  @Test
  public void empty() {
    client.tsCreate("ts", TSCreateParams.createParams().label("l", "v"));
    client.tsAdd("ts", 1, 1);
    client.tsAdd("ts", 2, 3);
    client.tsAdd("ts", 11, 7);
    client.tsAdd("ts", 13, 1);

    // range
    List<TSElement> range = client.tsRange("ts", TSRangeParams.rangeParams().aggregation(AggregationType.MAX, 5));
    assertEquals(2, range.size());
    range = client.tsRange("ts", TSRangeParams.rangeParams().aggregation(AggregationType.MAX, 5).empty());
    assertEquals(3, range.size());
    assertNotNull(range.get(1).getValue()); // any parsable value

    // revrange
    range = client.tsRevRange("ts", TSRangeParams.rangeParams().aggregation(AggregationType.MIN, 5));
    assertEquals(2, range.size());
    range = client.tsRevRange("ts", TSRangeParams.rangeParams().aggregation(AggregationType.MIN, 5).empty());
    assertEquals(3, range.size());
    assertNotNull(range.get(1).getValue()); // any parsable value

    // mrange
    Map<String, TSMRangeElements> mrange = client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.MIN, 5).filter("l=v"));
    assertEquals(1, mrange.size());
    ArrayList<TSMRangeElements> mrangeList = new ArrayList<>(mrange.values());
    assertEquals(2, mrangeList.get(0).getValue().size());
    mrange = client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.MIN, 5).empty().filter("l=v"));
    assertEquals(1, mrange.size());
    mrangeList = new ArrayList<>(mrange.values());
    assertEquals(3, mrangeList.get(0).getValue().size());
    assertNotNull(mrangeList.get(0).getValue().get(1).getValue()); // any parsable value

    // mrevrange
    mrange = client.tsMRevRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.MAX, 5).filter("l=v"));
    assertEquals(1, mrange.size());
    mrangeList = new ArrayList<>(mrange.values());
    assertEquals(2, mrangeList.get(0).getValue().size());
    mrange = client.tsMRevRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.MAX, 5).empty().filter("l=v"));
    assertEquals(1, mrange.size());
    mrangeList = new ArrayList<>(mrange.values());
    assertEquals(3, mrangeList.get(0).getValue().size());
    assertNotNull(mrangeList.get(0).getValue().get(1).getValue()); // any parsable value
  }

  @Test
  public void bucketTimestamp() {
    client.tsCreate("ts", TSCreateParams.createParams().label("l", "v"));
    client.tsAdd("ts", 1, 1);
    client.tsAdd("ts", 2, 3);

    // range / revrange
    assertEquals(0, client.tsRange("ts", TSRangeParams.rangeParams()
        .aggregation(AggregationType.FIRST, 10).bucketTimestampLow()).get(0).getTimestamp());
    assertEquals(10, client.tsRange("ts", TSRangeParams.rangeParams()
        .aggregation(AggregationType.LAST, 10).bucketTimestampHigh()).get(0).getTimestamp());
    assertEquals(5, client.tsRange("ts", TSRangeParams.rangeParams()
        .aggregation(AggregationType.RANGE, 10).bucketTimestampMid()).get(0).getTimestamp());
    assertEquals(5, client.tsRevRange("ts", TSRangeParams.rangeParams()
        .aggregation(AggregationType.TWA, 10).bucketTimestampMid()).get(0).getTimestamp());
    assertEquals(5, client.tsRevRange("ts", TSRangeParams.rangeParams()
        .aggregation(AggregationType.TWA, 10).bucketTimestamp("mid")).get(0).getTimestamp());

    // mrange / mrevrange
    assertEquals(0, client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.STD_P, 10).bucketTimestampLow().filter("l=v"))
        .values().stream().findAny().get().getValue().get(0).getTimestamp());
    assertEquals(10, client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.STD_S, 10).bucketTimestampHigh().filter("l=v"))
        .values().stream().findAny().get().getValue().get(0).getTimestamp());
    assertEquals(5, client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.TWA, 10).bucketTimestampMid().filter("l=v"))
        .values().stream().findAny().get().getValue().get(0).getTimestamp());
    assertEquals(5, client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.VAR_P, 10).bucketTimestampMid().filter("l=v"))
        .values().stream().findAny().get().getValue().get(0).getTimestamp());
    assertEquals(5, client.tsMRange(TSMRangeParams.multiRangeParams()
        .aggregation(AggregationType.VAR_S, 10).bucketTimestamp("~").filter("l=v"))
        .values().stream().findAny().get().getValue().get(0).getTimestamp());
  }

  @Test
  public void alignTimestamp() {
    client.tsCreate("ts1");
    client.tsCreate("ts2");
    client.tsCreate("ts3");
    client.tsCreateRule("ts1", "ts2", AggregationType.COUNT, 10, 0);
    client.tsCreateRule("ts1", "ts3", AggregationType.COUNT, 10, 1);
    client.tsAdd("ts1", 1, 1);
    client.tsAdd("ts1", 10, 3);
    client.tsAdd("ts1", 21, 7);
    assertEquals(2, client.tsRange("ts2", TSRangeParams.rangeParams().aggregation(AggregationType.COUNT, 10)).size());
    assertEquals(1, client.tsRange("ts3", TSRangeParams.rangeParams().aggregation(AggregationType.COUNT, 10)).size());
  }
}
