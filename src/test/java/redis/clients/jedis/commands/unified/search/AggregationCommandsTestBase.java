package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.util.RedisConditions.ModuleVersion.SEARCH_MOD_VER_80M3;
import static redis.clients.jedis.util.RedisConditions.ModuleVersion.SEARCH_MOD_VER_84RC1;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.util.RedisConditions;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * Base test class for Aggregation commands using the UnifiedJedis pattern. Tests FT.AGGREGATE,
 * FT.CURSOR, etc.
 */
@Tag("search")
public abstract class AggregationCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String INDEX = "aggbindex";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public AggregationCommandsTestBase(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  protected void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    jedis.hset(key, map);
  }

  protected void addDocument(String key, Map<String, Object> objMap) {
    Map<String, String> strMap = new HashMap<>();
    objMap.entrySet()
        .forEach(entry -> strMap.put(entry.getKey(), String.valueOf(entry.getValue())));
    jedis.hset(key, strMap);
  }

  @Test
  public void testAggregations() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"));

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(2, res.getTotalResults());

    Row r1 = res.getRow(0);
    assertNotNull(r1);
    assertEquals("def", r1.getString("name"));
    assertEquals(30, r1.getLong("sum"));
    assertEquals(30., r1.getDouble("sum"), 0);

    assertEquals(0L, r1.getLong("nosuchcol"));
    assertEquals(0.0, r1.getDouble("nosuchcol"), 0);
    assertEquals("", r1.getString("nosuchcol"));

    Row r2 = res.getRow(1);
    assertNotNull(r2);
    assertEquals("abc", r2.getString("name"));
    assertEquals(10, r2.getLong("sum"));
  }

  @Test
  public void testAggregations2() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"));

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(2, res.getTotalResults());

    List<Row> rows = res.getRows();
    assertEquals("def", rows.get(0).get("name"));
    assertEquals("30", rows.get(0).get("sum"));
    assertNull(rows.get(0).get("nosuchcol"));

    assertEquals("abc", rows.get(1).get("name"));
    assertEquals("10", rows.get(1).get("sum"));
  }

  @Test
  public void testAggregationBuilderVerbatim() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "hello kitty"));

    AggregationBuilder r = new AggregationBuilder("kitti");

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(1, res.getTotalResults());

    r = new AggregationBuilder("kitti").verbatim();

    res = jedis.ftAggregate(INDEX, r);
    assertEquals(0, res.getTotalResults());
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "ADDSCORES")
  public void testAggregationBuilderAddScores() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("age");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "Adam").set("age", 33));
    addDocument(new Document("data2").set("name", "Sara").set("age", 44));

    AggregationBuilder r = new AggregationBuilder("sara").addScores()
        .apply("@__score * 100", "normalized_score").dialect(3);

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    if (RedisConditions.of(jedis).moduleVersionIsGreaterThanOrEqual(SEARCH_MOD_VER_80M3)) {
      assertEquals(0.6931, res.getRow(0).getDouble("__score"), 0.0001);
      assertEquals(69.31, res.getRow(0).getDouble("normalized_score"), 0.01);
    } else {
      assertEquals(2, res.getRow(0).getLong("__score"));
      assertEquals(200, res.getRow(0).getLong("normalized_score"));
    }
  }

  @Test
  public void testAggregationBuilderTimeout() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).timeout(5000);

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void testAggregationBuilderParamsDialect() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    Map<String, Object> params = new HashMap<>();
    params.put("name", "abc");

    AggregationBuilder r = new AggregationBuilder("$name")
        .groupBy("@name", Reducers.sum("@count").as("sum")).params(params).dialect(2);

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(1, res.getTotalResults());

    Row r1 = res.getRow(0);
    assertNotNull(r1);
    assertEquals("abc", r1.getString("name"));
    assertEquals(10, r1.getLong("sum"));
  }

  @Test
  public void testApplyAndFilterAggregations() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("subj1");
    sc.addSortableNumericField("subj2");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));
    addDocument(new Document("data3").set("name", "ghi").set("subj1", 50).set("subj2", 80));
    addDocument(new Document("data4").set("name", "abc").set("subj1", 30).set("subj2", 20));
    addDocument(new Document("data5").set("name", "def").set("subj1", 65).set("subj2", 45));
    addDocument(new Document("data6").set("name", "ghi").set("subj1", 70).set("subj2", 70));

    AggregationBuilder r = new AggregationBuilder().apply("(@subj1+@subj2)/2", "attemptavg")
        .groupBy("@name", Reducers.avg("@attemptavg").as("avgscore")).filter("@avgscore>=50")
        .sortBy(10, SortedField.asc("@name"));

    AggregationResult res = jedis.ftAggregate(INDEX, r);

    if (RedisConditions.of(jedis).moduleVersionIsGreaterThanOrEqual(SEARCH_MOD_VER_84RC1)) {
      assertEquals(2, res.getTotalResults());
    }

    Row r1 = res.getRow(0);
    assertNotNull(r1);
    assertEquals("def", r1.getString("name"));
    assertEquals(52.5, r1.getDouble("avgscore"), 0);

    Row r2 = res.getRow(1);
    assertNotNull(r2);
    assertEquals("ghi", r2.getString("name"));
    assertEquals(67.5, r2.getDouble("avgscore"), 0);
  }

  @Test
  public void load() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("subj1");
    sc.addSortableNumericField("subj2");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));

    AggregationBuilder builder = new AggregationBuilder()
        .load(FieldName.of("@subj1").as("a"), FieldName.of("@subj2").as("b"))
        .apply("(@a+@b)/2", "avg").sortByDesc("@avg");

    AggregationResult result = jedis.ftAggregate(INDEX, builder);
    assertEquals(50.0, result.getRow(0).getDouble("avg"), 0d);
    assertEquals(45.0, result.getRow(1).getDouble("avg"), 0d);
  }

  @Test
  public void loadAll() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("subj1");
    sc.addSortableNumericField("subj2");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));

    AggregationBuilder builder = new AggregationBuilder().loadAll()
        .apply("(@subj1+@subj2)/2", "avg").sortByDesc("@avg");

    AggregationResult result = jedis.ftAggregate(INDEX, builder);
    assertEquals(50.0, result.getRow(0).getDouble("avg"), 0d);
    assertEquals(45.0, result.getRow(1).getDouble("avg"), 0d);
  }

  @Test
  public void cursor() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(1, 3000);

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(2, res.getTotalResults());

    Row row = res.getRow(0);
    assertNotNull(row);
    assertEquals("def", row.getString("name"));
    assertEquals(30, row.getLong("sum"));
    assertEquals(30., row.getDouble("sum"), 0);

    assertEquals(0L, row.getLong("nosuchcol"));
    assertEquals(0.0, row.getDouble("nosuchcol"), 0);
    assertEquals("", row.getString("nosuchcol"));

    res = jedis.ftCursorRead(INDEX, res.getCursorId(), 1);
    Row row2 = res.getRow(0);
    assertNotNull(row2);
    assertEquals("abc", row2.getString("name"));
    assertEquals(10, row2.getLong("sum"));

    assertEquals("OK", jedis.ftCursorDel(INDEX, res.getCursorId()));

    try {
      jedis.ftCursorRead(INDEX, res.getCursorId(), 1);
      fail();
    } catch (JedisDataException e) {
      // ignore
    }
  }

  @Test
  public void aggregateIteration() {
    jedis.ftCreate(INDEX, TextField.of("name").sortable(), NumericField.of("count"));

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    AggregationBuilder agg = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000);

    FtAggregateIteration rr = jedis.ftAggregateIteration(INDEX, agg);
    int total = 0;
    while (!rr.isIterationCompleted()) {
      AggregationResult res = rr.nextBatch();
      int count = res.getRows().size();
      assertThat(count, Matchers.lessThanOrEqualTo(2));
      total += count;
    }
    assertEquals(4, total);
  }

  @Test
  public void aggregateIterationCollect() {
    jedis.ftCreate(INDEX, TextField.of("name").sortable(), NumericField.of("count"));

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    AggregationBuilder agg = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000);

    assertEquals(4, jedis.ftAggregateIteration(INDEX, agg).collect(new ArrayList<>()).size());
  }

  @Test
  public void testWrongAggregation() throws InterruptedException {
    Schema sc = new Schema().addTextField("title", 5.0).addTextField("body", 1.0)
        .addTextField("state", 1.0).addNumericField("price");

    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("state", "NY");
    fields.put("body", "lorem ipsum");
    fields.put("price", "1337");
    addDocument("doc1", fields);

    AggregationBuilder builder = new AggregationBuilder("hello").apply("@price/1000", "k")
        .groupBy("@state", Reducers.avg("@k").as("avgprice")).filter("@avgprice>=2")
        .sortBy(10, SortedField.asc("@state"));

    try {
      jedis.ftAggregate(INDEX, builder);
      fail();
    } catch (JedisDataException e) {
      // should throw JedisDataException on wrong aggregation query
    }
  }

  @Test
  public void testAggregations2Profile() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"));

    Map.Entry<AggregationResult, ProfilingInfo> reply = jedis.ftProfileAggregate(INDEX,
      FTProfileParams.profileParams(), aggr);

    AggregationResult result = reply.getKey();
    assertEquals(2, result.getTotalResults());

    List<Row> rows = result.getRows();
    assertEquals("def", rows.get(0).get("name"));
    assertEquals("30", rows.get(0).get("sum"));
    assertNull(rows.get(0).get("nosuchcol"));

    assertEquals("abc", rows.get(1).get("name"));
    assertEquals("10", rows.get(1).get("sum"));

    Object profileObject = reply.getValue().getProfilingInfo();
    if (protocol != RedisProtocol.RESP3) {
      assertThat(profileObject, Matchers.isA(List.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0_PRE)) {
        assertThat((List<Object>) profileObject, Matchers.hasItems("Shards", "Coordinator"));
      }
    } else {
      assertThat(profileObject, Matchers.isA(Map.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0_PRE)) {
        assertThat(((Map<String, Object>) profileObject).keySet(),
          Matchers.hasItems("Shards", "Coordinator"));
      }
    }
  }
}
