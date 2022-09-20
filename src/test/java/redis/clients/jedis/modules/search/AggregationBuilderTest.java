package redis.clients.jedis.modules.search;

import redis.clients.jedis.exceptions.JedisDataException;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.search.aggr.SortedField;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class AggregationBuilderTest extends RedisModuleCommandsTestBase {

  private static final String index = "aggbindex";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  private void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    client.hset(key, map);
  }

  private void addDocument(String key, Map<String, Object> objMap) {
    Map<String, String> strMap = new HashMap<>();
    objMap.entrySet().forEach(entry -> strMap.put(entry.getKey(), String.valueOf(entry.getValue())));
    client.hset(key, strMap);
  }

  @Test
  public void testAggregations() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
//    client.addDocument(new Document("data1").set("name", "abc").set("count", 10));
//    client.addDocument(new Document("data2").set("name", "def").set("count", 5));
//    client.addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum"))
        .sortBy(10, SortedField.desc("@sum"));

    // actual search
    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(2, res.totalResults);

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
  public void testAggregationBuilderVerbatim() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "hello kitty"));

    AggregationBuilder r = new AggregationBuilder("kitti");

    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(1, res.totalResults);

    r = new AggregationBuilder("kitti")
            .verbatim();

    res = client.ftAggregate(index, r);
    assertEquals(0, res.totalResults);
  }

  @Test
  public void testAggregationBuilderTimeout() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
            .groupBy("@name", Reducers.sum("@count").as("sum"))
            .timeout(5000);

    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(2, res.totalResults);
  }

  @Test
  public void testAggregationBuilderParamsDialect() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    Map<String, Object> params = new HashMap<>();
    params.put("name", "abc");

    AggregationBuilder r = new AggregationBuilder("$name")
            .groupBy("@name", Reducers.sum("@count").as("sum"))
            .params(params)
            .dialect(2); // From documentation - To use PARAMS, DIALECT must be set to 2

    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(1, res.totalResults);

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
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
//    client.addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
//    client.addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));
//    client.addDocument(new Document("data3").set("name", "ghi").set("subj1", 50).set("subj2", 80));
//    client.addDocument(new Document("data4").set("name", "abc").set("subj1", 30).set("subj2", 20));
//    client.addDocument(new Document("data5").set("name", "def").set("subj1", 65).set("subj2", 45));
//    client.addDocument(new Document("data6").set("name", "ghi").set("subj1", 70).set("subj2", 70));
    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));
    addDocument(new Document("data3").set("name", "ghi").set("subj1", 50).set("subj2", 80));
    addDocument(new Document("data4").set("name", "abc").set("subj1", 30).set("subj2", 20));
    addDocument(new Document("data5").set("name", "def").set("subj1", 65).set("subj2", 45));
    addDocument(new Document("data6").set("name", "ghi").set("subj1", 70).set("subj2", 70));

    AggregationBuilder r = new AggregationBuilder().apply("(@subj1+@subj2)/2", "attemptavg")
        .groupBy("@name", Reducers.avg("@attemptavg").as("avgscore"))
        .filter("@avgscore>=50")
        .sortBy(10, SortedField.asc("@name"));

    // actual search
    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(3, res.totalResults);

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
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
//    client.addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
//    client.addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));
    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));

    AggregationBuilder builder = new AggregationBuilder()
        .load(FieldName.of("@subj1").as("a"), FieldName.of("@subj2").as("b"))
        .apply("(@a+@b)/2", "avg").sortByDesc("@avg");

    AggregationResult result = client.ftAggregate(index, builder);
    assertEquals(50.0, result.getRow(0).getDouble("avg"), 0d);
    assertEquals(45.0, result.getRow(1).getDouble("avg"), 0d);
  }

  @Test
  public void loadAll() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("subj1");
    sc.addSortableNumericField("subj2");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("subj1", 20).set("subj2", 70));
    addDocument(new Document("data2").set("name", "def").set("subj1", 60).set("subj2", 40));

    AggregationBuilder builder = new AggregationBuilder()
        .loadAll()
        .apply("(@subj1+@subj2)/2", "avg").sortByDesc("@avg");

    AggregationResult result = client.ftAggregate(index, builder);
    assertEquals(50.0, result.getRow(0).getDouble("avg"), 0d);
    assertEquals(45.0, result.getRow(1).getDouble("avg"), 0d);
  }

  @Test
  public void testCursor() throws InterruptedException {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
//    client.addDocument(new Document("data1").set("name", "abc").set("count", 10));
//    client.addDocument(new Document("data2").set("name", "def").set("count", 5));
//    client.addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder r = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum"))
        .sortBy(10, SortedField.desc("@sum"))
        .cursor(1, 3000);

    // actual search
    AggregationResult res = client.ftAggregate(index, r);
    assertEquals(2, res.totalResults);

    Row row = res.getRow(0);
    assertNotNull(row);
    assertEquals("def", row.getString("name"));
    assertEquals(30, row.getLong("sum"));
    assertEquals(30., row.getDouble("sum"), 0);

    assertEquals(0L, row.getLong("nosuchcol"));
    assertEquals(0.0, row.getDouble("nosuchcol"), 0);
    assertEquals("", row.getString("nosuchcol"));

    res = client.ftCursorRead(index, res.getCursorId(), 1);
    Row row2 = res.getRow(0);
    assertNotNull(row2);
    assertEquals("abc", row2.getString("name"));
    assertEquals(10, row2.getLong("sum"));

    assertEquals("OK", client.ftCursorDel(index, res.getCursorId()));

    try {
      client.ftCursorRead(index, res.getCursorId(), 1);
      fail();
    } catch (JedisDataException e) {
    }

    AggregationBuilder r2 = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum"))
        .sortBy(10, SortedField.desc("@sum"))
        .cursor(1, 1000);

    Thread.sleep(1000);

    try {
      client.ftCursorRead(index, res.getCursorId(), 1);
      fail();
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void testWrongAggregation() throws InterruptedException {
    Schema sc = new Schema()
        .addTextField("title", 5.0)
        .addTextField("body", 1.0)
        .addTextField("state", 1.0)
        .addNumericField("price");

    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    // insert document(s)
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("state", "NY");
    fields.put("body", "lorem ipsum");
    fields.put("price", "1337");
//    client.addDocument("doc1", fields);
    addDocument("doc1", fields);

    // wrong aggregation query
    AggregationBuilder builder = new AggregationBuilder("hello")
        .apply("@price/1000", "k")
        .groupBy("@state", Reducers.avg("@k").as("avgprice"))
        .filter("@avgprice>=2")
        .sortBy(10, SortedField.asc("@state"));

    try {
      client.ftAggregate(index, builder);
      fail();
    } catch (JedisDataException e) {
      // should throw JedisDataException on wrong aggregation query 
    }

  }
}
