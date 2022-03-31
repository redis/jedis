package redis.clients.jedis.modules.search;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;
import static redis.clients.jedis.search.RediSearchUtil.toStringMap;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.Schema.*;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.util.SafeEncoder;

public class SearchTest extends RedisModuleCommandsTestBase {

  private static final String index = "testindex";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  private void addDocument(String key, Map<String, Object> map) {
    client.hset(key, toStringMap(map));
  }

  private static Map<String, Object> toMap(Object... values) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }

  private static Map<String, String> toMap(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  @Test
  public void creatDefinion() throws Exception {
    Schema sc = new Schema().addTextField("first", 1.0).addTextField("last", 1.0).addNumericField("age");
    IndexDefinition rule = new IndexDefinition()
        .setFilter("@age>16")
        .setPrefixes(new String[]{"student:", "pupil:"});

    try {
      assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), sc));
    } catch (JedisDataException e) {
      // ON was only supported from RediSearch 2.0
      assertEquals("Unknown argument `ON`", e.getMessage());
      return;
    }

    client.hset("profesor:5555", toMap("first", "Albert", "last", "Blue", "age", "55"));
    client.hset("student:1111", toMap("first", "Joe", "last", "Dod", "age", "18"));
    client.hset("pupil:2222", toMap("first", "Jen", "last", "Rod", "age", "14"));
    client.hset("student:3333", toMap("first", "El", "last", "Mark", "age", "17"));
    client.hset("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", "21"));
    client.hset("student:5555", toMap("first", "Joen", "last", "Ko", "age", "20"));
    client.hset("teacher:6666", toMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = client.ftSearch(index, new Query("@first:Jo*"));
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = client.ftSearch(index, new Query("@first:Pat"));
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = client.ftSearch(index, new Query("@last:Rod"));
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void withObjectMap() throws Exception {
    Schema sc = new Schema().addTextField("first", 1.0).addTextField("last", 1.0).addNumericField("age");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    addDocument("student:1111", toMap("first", "Joe", "last", "Dod", "age", 18));
    addDocument("student:3333", toMap("first", "El", "last", "Mark", "age", 17));
    addDocument("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", 21));
    addDocument("student:5555", toMap("first", "Joen", "last", "Ko", "age", 20));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = client.ftSearch(index, new Query("@first:Jo*"));
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = client.ftSearch(index, new Query("@first:Pat"));
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = client.ftSearch(index, new Query("@last:Rod"));
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void createWithFieldNames() throws Exception {
    Schema sc = new Schema().addField(new TextField(FieldName.of("first").as("given")))
        .addField(new TextField(FieldName.of("last")));
    IndexDefinition rule = new IndexDefinition()
        //.setFilter("@age>16")
        .setPrefixes(new String[]{"student:", "pupil:"});

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), sc));

    client.hset("profesor:5555", toMap("first", "Albert", "last", "Blue", "age", "55"));
    client.hset("student:1111", toMap("first", "Joe", "last", "Dod", "age", "18"));
    client.hset("pupil:2222", toMap("first", "Jen", "last", "Rod", "age", "14"));
    client.hset("student:3333", toMap("first", "El", "last", "Mark", "age", "17"));
    client.hset("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", "21"));
    client.hset("student:5555", toMap("first", "Joen", "last", "Ko", "age", "20"));
    client.hset("teacher:6666", toMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asOriginal = client.ftSearch(index, new Query("@first:Jo*"));
    assertEquals(0, asOriginal.getTotalResults());

    SearchResult asAttribute = client.ftSearch(index, new Query("@given:Jo*"));
    assertEquals(2, asAttribute.getTotalResults());

    SearchResult nonAttribute = client.ftSearch(index, new Query("@last:Rod"));
    assertEquals(1, nonAttribute.getTotalResults());
  }

  @Test
  public void search() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");
    for (int i = 0; i < 100; i++) {
//      assertTrue(client.addDocument(String.format("doc%d", i), (double) i / 100.0, fields));
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world").limit(0, 5).setWithScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(5, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
//      assertEquals(
//          String.format(
//              "{\"id\":\"%s\",\"score\":%s,\"properties\":{\"title\":\"hello world\",\"body\":\"lorem ipsum\"}}",
//              d.getId(), Double.toString(d.getScore())),
//          d.toString());
    }

//    assertTrue(client.deleteDocument("doc0", true));
//    assertFalse(client.deleteDocument("doc0"));
    client.del("doc0");

    res = client.ftSearch(index, new Query("hello world"));
    assertEquals(99, res.getTotalResults());

    assertEquals("OK", client.ftDropIndex(index));
    try {
      client.ftSearch(index, new Query("hello world"));
      fail();
    } catch (JedisDataException e) {
    }
  }
//
//  @Test
//  public void searchBatch() throws Exception {
//    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);
//
//    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
//    Map<String, Object> fields = new HashMap<>();
//    fields.put("title", "hello world");
//    fields.put("body", "lorem ipsum");
//    for (int i = 0; i < 50; i++) {
//      fields.put("title", "hello world");
////      assertTrue(client.addDocument(String.format("doc%d", i), (double) i / 100.0, fields));
//      addDocument(String.format("doc%d", i), fields);
//    }
//
//    for (int i = 50; i < 100; i++) {
//      fields.put("title", "good night");
////      assertTrue(client.addDocument(String.format("doc%d", i), (double) i / 100.0, fields));
//      addDocument(String.format("doc%d", i), fields);
//    }
//
//    SearchResult[] res = client.searchBatch(
//        new Query("hello world").limit(0, 5).setWithScores(),
//        new Query("good night").limit(0, 5).setWithScores()
//    );
//
//    assertEquals(2, res.length);
//    assertEquals(50, res[0].getTotalResults());
//    assertEquals(50, res[1].getTotalResults());
//    assertEquals(5, res[0].getDocuments().size());
//    for (Document d : res[0].getDocuments()) {
//      assertTrue(d.getId().startsWith("doc"));
//      assertTrue(d.getScore() < 100);
//      assertEquals(
//          String.format(
//              "{\"id\":\"%s\",\"score\":%s,\"properties\":{\"title\":\"hello world\",\"body\":\"lorem ipsum\"}}",
//              d.getId(), Double.toString(d.getScore())),
//          d.toString());
//    }
//  }

  @Test
  public void testNumericFilter() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0).addNumericField("price");

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");

    for (int i = 0; i < 100; i++) {
      fields.put("price", i);
//      assertTrue(client.addDocument(String.format("doc%d", i), fields));
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world").
        addFilter(new Query.NumericFilter("price", 0, 49)));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 0);
      assertTrue(price <= 49);
    }

    res = client.ftSearch(index, new Query("hello world").
        addFilter(new Query.NumericFilter("price", 0, true, 49, true)));
    assertEquals(48, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price > 0);
      assertTrue(price < 49);
    }
    res = client.ftSearch(index, new Query("hello world").
        addFilter(new Query.NumericFilter("price", 50, 100)));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 50);
      assertTrue(price <= 100);
    }

    res = client.ftSearch(index, new Query("hello world").
        addFilter(new Query.NumericFilter("price", 20, Double.POSITIVE_INFINITY)));
    assertEquals(80, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    res = client.ftSearch(index, new Query("hello world").
        addFilter(new Query.NumericFilter("price", Double.NEGATIVE_INFINITY, 10)));
    assertEquals(11, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

  }

  @Test
  public void testStopwords() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index,
        IndexOptions.defaultOptions().setStopwords("foo", "bar", "baz"), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
//    assertTrue(client.addDocument("doc1", fields));
    addDocument("doc1", fields);
    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(1, res.getTotalResults());
    res = client.ftSearch(index, new Query("foo bar"));
    assertEquals(0, res.getTotalResults());
//
//    client.connection().flushDB();
//
//    assertEquals("OK", client.ftCreate(index, sc,
//        IndexOptions.defaultOptions().setNoStopwords()));
//    fields.put("title", "hello world foo bar to be or not to be");
//    assertTrue(client.addDocument("doc1", fields));
//
//    assertEquals(1, client.ftSearch(index, new Query("hello world")).getTotalResults());
//    assertEquals(1, client.ftSearch(index, new Query("foo bar")).getTotalResults());
//    assertEquals(1, client.ftSearch(index, new Query("to be or not to be")).getTotalResults());
  }

  @Test
  public void testStopwordsMore() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index,
        IndexOptions.defaultOptions().setNoStopwords(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    fields.put("title", "hello world foo bar to be or not to be");
    addDocument("doc1", fields);

    assertEquals(1, client.ftSearch(index, new Query("hello world")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("foo bar")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("to be or not to be")).getTotalResults());
  }

  @Test
  public void testGeoFilter() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0).addGeoField("loc");

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", "-0.441,51.458");
//    assertTrue(client.addDocument("doc1", fields));
    addDocument("doc1", fields);
    fields.put("loc", "-0.1,51.2");
//    assertTrue(client.addDocument("doc2", fields));
    addDocument("doc2", fields);

    SearchResult res = client.ftSearch(index, new Query("hello world").
        addFilter(
            new Query.GeoFilter("loc", -0.44, 51.45,
                10, Query.GeoFilter.KILOMETERS)
        ));

    assertEquals(1, res.getTotalResults());
    res = client.ftSearch(index, new Query("hello world").
        addFilter(
            new Query.GeoFilter("loc", -0.44, 51.45,
                100, Query.GeoFilter.KILOMETERS)
        ));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void geoFilterAndGeoCoordinateObject() throws Exception {
    Schema schema = new Schema().addTextField("title", 1.0).addGeoField("loc");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), schema));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", new redis.clients.jedis.GeoCoordinate(-0.441, 51.458));
//    assertTrue(client.addDocument("doc1", fields));
    addDocument("doc1", fields);
    fields.put("loc", new redis.clients.jedis.GeoCoordinate(-0.1, 51.2));
//    assertTrue(client.addDocument("doc2", fields));
    addDocument("doc2", fields);

    SearchResult res = client.ftSearch(index, new Query("hello world").addFilter(
        new Query.GeoFilter("loc", -0.44, 51.45, 10, Query.GeoFilter.KILOMETERS)));
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, new Query("hello world").addFilter(
        new Query.GeoFilter("loc", -0.44, 51.45, 100, Query.GeoFilter.KILOMETERS)));
    assertEquals(2, res.getTotalResults());
  }
//
//  // TODO: This test was broken in master branch
//  @Test
//  public void testPayloads() throws Exception {
//    Schema sc = new Schema().addTextField("title", 1.0);
//
//    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
//
//    Map<String, Object> fields = new HashMap<>();
//    fields.put("title", "hello world");
//    String payload = "foo bar";
////    assertTrue(client.addDocument("doc1", 1.0, fields, false, false, payload.getBytes()));
//    addDocument("doc1", fields);
//
//    SearchResult res = client.ftSearch(index, new Query("hello world").setWithPayload());
//    assertEquals(1, res.getTotalResults());
//    assertEquals(1, res.getDocuments().size());
//
//    assertEquals(payload, new String(res.getDocuments().get(0).getPayload()));
//  }

  @Test
  public void testQueryFlags() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();

    for (int i = 0; i < 100; i++) {
      fields.put("title", i % 2 != 0 ? "hello worlds" : "hello world");
//      assertTrue(client.addDocument(String.format("doc%d", i), (double) i / 100.0, fields));
      addDocument(String.format("doc%d", i), fields);
    }

    Query q = new Query("hello").setWithScores();
    SearchResult res = client.ftSearch(index, q);

    assertEquals(100, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
//      assertNotEquals(1.0, d.getScore());
      assertTrue(((String) d.get("title")).startsWith("hello world"));
    }

    q = new Query("hello").setNoContent();
    res = client.ftSearch(index, q);
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertEquals(1.0, d.getScore(), 0);
      assertNull(d.get("title"));
    }

    // test verbatim vs. stemming
    res = client.ftSearch(index, new Query("hello worlds"));
    assertEquals(100, res.getTotalResults());
    res = client.ftSearch(index, new Query("hello worlds").setVerbatim());
    assertEquals(50, res.getTotalResults());

    res = client.ftSearch(index, new Query("hello a world").setVerbatim());
    assertEquals(50, res.getTotalResults());
    res = client.ftSearch(index, new Query("hello a worlds").setVerbatim());
    assertEquals(50, res.getTotalResults());
    res = client.ftSearch(index, new Query("hello a world").setVerbatim().setNoStopwords());
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void testHNSWVVectorSimilarity() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    Schema sc = new Schema().addHNSWVectorField("v", attr);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    client.hset("a", "v", "aaaaaaaa");
    client.hset("b", "v", "aaaabaaa");
    client.hset("c", "v", "aaaaabaa");

    Query query = new Query("*=>[KNN 2 @v $vec]")
        .addParam("vec", "aaaaaaaa")
        .setSortBy("__v_score", true)
        .returnFields("__v_score")
        .dialect(2);
    Document doc1 = client.ftSearch(index, query).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void testFlatVectorSimilarity() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    Schema sc = new Schema().addFlatVectorField("v", attr);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    client.hset("a", "v", "aaaaaaaa");
    client.hset("b", "v", "aaaabaaa");
    client.hset("c", "v", "aaaaabaa");

    Query query = new Query("*=>[KNN 2 @v $vec]")
        .addParam("vec", "aaaaaaaa")
        .setSortBy("__v_score", true)
        .returnFields("__v_score")
        .dialect(2);
    Document doc1 = client.ftSearch(index, query).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void testDialectConfig() {
    // confirm default
    assertEquals(singletonMap("DEFAULT_DIALECT", "1"), client.ftConfigGet("DEFAULT_DIALECT"));

    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "2"));
    assertEquals(singletonMap("DEFAULT_DIALECT", "2"), client.ftConfigGet("DEFAULT_DIALECT"));

    try {
      client.ftConfigSet("DEFAULT_DIALECT", "0");
      fail();
    } catch (JedisDataException ex) {
    }

    try {
      client.ftConfigSet("DEFAULT_DIALECT", "3");
      fail();
    } catch (JedisDataException ex) {
    }

    // Restore to default
    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "1"));
  }

  @Test
  public void testDialectsWithFTExplain() throws Exception {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    Schema sc = new Schema()
        .addFlatVectorField("v", attr)
        .addTagField("title")
        .addTextField("t1", 1.0)
        .addTextField("t2", 1.0)
        .addNumericField("num");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    client.hset("1", "t1", "hello");

    String q = "(*)";
    Query query = new Query(q).dialect(1);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q).dialect(2);
    assertTrue("Should contain 'WILDCARD'", client.ftExplain(index, query).contains("WILDCARD"));

    q = "$hello";
    query = new Query(q).dialect(1);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q).dialect(2).addParam("hello", "hello");
    assertTrue("Should contain 'UNION {\n  hello\n  +hello(expanded)\n}\n'",
        client.ftExplain(index, query).contains("UNION {\n  hello\n  +hello(expanded)\n}\n"));

    q = "@title:(@num:[0 10])";
    query = new Query(q).dialect(1);
    assertTrue("Should contain 'NUMERIC {0.000000 <= @num <= 10.000000}'",
        client.ftExplain(index, query).contains("NUMERIC {0.000000 <= @num <= 10.000000}"));
    query = new Query(q).dialect(2);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }

    q = "@t1:@t2:@t3:hello";
    query = new Query(q).dialect(1);
    assertTrue("Should contain '@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n'",
        client.ftExplain(index, query).contains("@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n"));
    query = new Query(q).dialect(2);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }

    q = "@title:{foo}}}}}";
    query = new Query(q).dialect(1);
    assertTrue("Should contain 'TAG:@title {\n  foo\n}\n'",
        client.ftExplain(index, query).contains("TAG:@title {\n  foo\n}\n"));
    query = new Query(q).dialect(2);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }

    q = "*=>[KNN 10 @v $BLOB]";
    query = new Query(q).addParam("BLOB", "aaaa").dialect(1);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q).addParam("BLOB", "aaaa").dialect(2);
    assertTrue("Should contain '{K=10 nearest vector'", client.ftExplain(index, query).contains("{K=10 nearest vector"));

    q = "*=>[knn $K @vec_field $BLOB as score]";
    query = new Query(q).addParam("BLOB", "aaaa").addParam("K", "10").dialect(1);
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q).addParam("BLOB", "aaaa").addParam("K", "10").dialect(2);
    assertTrue("Should contain '{K=10 nearest vector'", client.ftExplain(index, query).contains("{K=10 nearest vector"));
  }

  @Test
  public void testQueryParams() {
    Schema sc = new Schema().addNumericField("numval");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    client.hset("1", "numval", "1");
    client.hset("2", "numval", "2");
    client.hset("3", "numval", "3");

    Query query =  new Query("@numval:[$min $max]").addParam("min", 1).addParam("max", 2).dialect(2);
    assertEquals(2, client.ftSearch(index, query).getTotalResults());
  }

  @Test
  public void testSortQueryFlags() throws Exception {
    Schema sc = new Schema().addSortableTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();

    fields.put("title", "b title");
//    client.addDocument("doc1", 1.0, fields, false, true, null);
    addDocument("doc1", fields);

    fields.put("title", "a title");
//    client.addDocument("doc2", 1.0, fields, false, true, null);
    addDocument("doc2", fields);

    fields.put("title", "c title");
//    client.addDocument("doc3", 1.0, fields, false, true, null);
    addDocument("doc3", fields);

    Query q = new Query("title").setSortBy("title", true);
    SearchResult res = client.ftSearch(index, q);

    assertEquals(3, res.getTotalResults());
    Document doc1 = res.getDocuments().get(0);
    assertEquals("a title", doc1.get("title"));

    doc1 = res.getDocuments().get(1);
    assertEquals("b title", doc1.get("title"));

    doc1 = res.getDocuments().get(2);
    assertEquals("c title", doc1.get("title"));
  }

  @Test
  public void testNullField() throws Exception {
    Schema sc = new Schema()
        .addTextField("title", 1.0)
        .addTextField("genre", 1.0)
        .addTextField("plot", 1.0)
        .addSortableNumericField("release_year")
        .addTagField("tag")
        .addGeoField("loc");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    // create a document with a field set to null
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "another test with title ");
    fields.put("genre", "Comedy");
    fields.put("plot", "this is the plot for the test");
    fields.put("tag", "fun");
    fields.put("release_year", 2019);
    fields.put("loc", "-0.1,51.2");

//    client.addDocument("doc1", fields);
    addDocument("doc1", fields);
    SearchResult res = client.ftSearch(index, new Query("title"));
    assertEquals(1, res.getTotalResults());

    fields = new HashMap<>();
    fields.put("title", "another title another test");
    fields.put("genre", "Action");
    fields.put("plot", null);
    fields.put("tag", null);

    try {
//      client.addDocument("doc2", fields);
      addDocument("doc2", fields);
      fail("Should throw NullPointerException.");
    } catch (NullPointerException e) {
//      assertEquals("Document attribute 'tag' is null. (Remove it, or set a value)", e.getMessage());
    }

    res = client.ftSearch(index, new Query("title"));
    assertEquals(1, res.getTotalResults());

    // Testing with numerical value
    fields = new HashMap<>();
    fields.put("title", "another title another test");
    fields.put("genre", "Action");
    fields.put("release_year", null);
    try {
//      client.addDocument("doc2", fields);
      addDocument("doc2", fields);
      fail("Should throw NullPointerException.");
    } catch (NullPointerException e) {
//      assertEquals("Document attribute 'release_year' is null. (Remove it, or set a value)", e.getMessage());
    }
    res = client.ftSearch(index, new Query("title"));
    assertEquals(1, res.getTotalResults());
  }

  @Test
  public void dropIndex() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", client.ftDropIndex(index));

    try {
      client.ftSearch(index, new Query("hello world"));
      fail("Index should not exist.");
    } catch (JedisDataException de) {
      assertTrue(de.getMessage().contains("no such index"));
    }
    assertEquals(100, client.dbSize());
  }

  @Test
  public void dropIndexDD() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", client.ftDropIndexDD(index));

    Set<String> keys = client.keys("*");
    assertTrue(keys.isEmpty());
    assertEquals(0, client.dbSize());
  }

  @Test
  public void testDropMissing() throws Exception {
    try {
      client.ftDropIndex(index);
      fail();
    } catch (JedisDataException ex) {
    }
  }

  @Test
  public void alterAdd() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", client.ftAlter(index, new TagField("tags", ","), new TextField("name", 0.5)));
    for (int i = 0; i < 100; i++) {
      Map<String, Object> fields2 = new HashMap<>();
      fields2.put("name", "name" + i);
      fields2.put("tags", String.format("tagA,tagB,tag%d", i));
//      assertTrue(client.updateDocument(String.format("doc%d", i), 1.0, fields2));
      addDocument(String.format("doc%d", i), fields2);
    }
    SearchResult res2 = client.ftSearch(index, new Query("@tags:{tagA}"));
    assertEquals(100, res2.getTotalResults());

    Map<String, Object> info = client.ftInfo(index);
    assertEquals(index, info.get("index_name"));
    assertEquals("identifier", ((List) ((List) info.get("attributes")).get(1)).get(0));
    assertEquals("attribute", ((List) ((List) info.get("attributes")).get(1)).get(2));
  }

  @Test
  public void noStem() throws Exception {
    Schema sc = new Schema().addTextField("stemmed", 1.0).addField(new Schema.TextField("notStemmed", 1.0, false, true));
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("stemmed", "located");
    doc.put("notStemmed", "located");
    // Store it
//    assertTrue(client.addDocument("doc", doc));
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, new Query("@stemmed:location"));
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, new Query("@notStemmed:location"));
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void phoneticMatch() throws Exception {
    Schema sc = new Schema()
        .addTextField("noPhonetic", 1.0)
        .addField(new Schema.TextField("withPhonetic", 1.0, false, false, false, "dm:en"));

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("noPhonetic", "morfix");
    doc.put("withPhonetic", "morfix");

    // Store it
//    assertTrue(client.addDocument("doc", doc));
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, new Query("@withPhonetic:morphix=>{$phonetic:true}"));
    assertEquals(1, res.getTotalResults());

    try {
      client.ftSearch(index, new Query("@noPhonetic:morphix=>{$phonetic:true}"));
      fail();
    } catch (JedisDataException e) {/*field does not support phonetics*/
    }

    SearchResult res3 = client.ftSearch(index, new Query("@withPhonetic:morphix=>{$phonetic:false}"));
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void info() throws Exception {
    String MOVIE_ID = "movie_id";
    String TITLE = "title";
    String GENRE = "genre";
    String VOTES = "votes";
    String RATING = "rating";
    String RELEASE_YEAR = "release_year";
    String PLOT = "plot";
    String POSTER = "poster";

    Schema sc = new Schema()
        .addTextField(TITLE, 5.0)
        .addSortableTextField(PLOT, 1.0)
        .addSortableTagField(GENRE, ",")
        .addSortableNumericField(RELEASE_YEAR)
        .addSortableNumericField(RATING)
        .addSortableNumericField(VOTES);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> info = client.ftInfo(index);
    assertEquals(index, info.get("index_name"));

    assertEquals(6, ((List) info.get("attributes")).size());
    assertEquals("global_idle", ((List) info.get("cursor_stats")).get(0));
    assertEquals(0L, ((List) info.get("cursor_stats")).get(1));
  }

  @Test
  public void noIndex() throws Exception {
    Schema sc = new Schema()
        .addField(new Schema.TextField("f1", 1.0, true, false, true))
        .addField(new Schema.TextField("f2", 1.0));
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    Map<String, Object> mm = new HashMap<>();

    mm.put("f1", "MarkZZ");
    mm.put("f2", "MarkZZ");
//    client.addDocument("doc1", mm);
    addDocument("doc1", mm);

    mm.clear();
    mm.put("f1", "MarkAA");
    mm.put("f2", "MarkBB");
//    client.addDocument("doc2", mm);
    addDocument("doc2", mm);

    SearchResult res = client.ftSearch(index, new Query("@f1:Mark*"));
    assertEquals(0, res.getTotalResults());

    res = client.ftSearch(index, new Query("@f2:Mark*"));
    assertEquals(2, res.getTotalResults());

    Document[] docs = new Document[2];

    res = client.ftSearch(index, new Query("@f2:Mark*").setSortBy("f1", false));
    assertEquals(2, res.getTotalResults());

    res.getDocuments().toArray(docs);
    assertEquals("doc1", docs[0].getId());

    res = client.ftSearch(index, new Query("@f2:Mark*").setSortBy("f1", true));
    res.getDocuments().toArray(docs);
    assertEquals("doc2", docs[0].getId());
  }
//
//  @Test
//  public void testReplacePartial() throws Exception {
//    Schema sc = new Schema()
//        .addTextField("f1", 1.0)
//        .addTextField("f2", 1.0)
//        .addTextField("f3", 1.0);
//    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
//
//    Map<String, Object> mm = new HashMap<>();
//    mm.put("f1", "f1_val");
//    mm.put("f2", "f2_val");
//
////    assertTrue(client.addDocument("doc1", mm));
//    addDocument("doc1", mm);
////    assertTrue(client.addDocument("doc2", mm));
//    addDocument("doc2", mm);
//
//    mm.clear();
//    mm.put("f3", "f3_val");
//
////    assertTrue(client.updateDocument("doc1", 1.0, mm));
//    addDocument("doc1", mm);
////    assertTrue(client.replaceDocument("doc2", 1.0, mm));
//    addDocument("doc2", mm);
//
//    // Search for f3 value. All documents should have it.
//    SearchResult res = client.ftSearch(index, new Query(("@f3:f3_Val")));
//    assertEquals(2, res.getTotalResults());
//
//    res = client.ftSearch(index, new Query("@f3:f3_val @f2:f2_val @f1:f1_val"));
//    assertEquals(1, res.getTotalResults());
//  }
//
//  @Test
//  public void testReplaceIf() throws Exception {
//    Schema sc = new Schema()
//        .addTextField("f1", 1.0)
//        .addTextField("f2", 1.0)
//        .addTextField("f3", 1.0);
//    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
//
//    Map<String, Object> mm = new HashMap<>();
//    mm.put("f1", "v1_val");
//    mm.put("f2", "v2_val");
//
//    assertTrue(client.addDocument("doc1", mm));
//    assertTrue(client.addDocument("doc2", mm));
//
//    mm.clear();
//    mm.put("f3", "v3_val");
//
//    assertFalse(client.updateDocument("doc1", 1.0, mm, "@f1=='vv1_val'"));
//    // Search for f3 value. No documents should not have it.
//    SearchResult res1 = client.ftSearch(index, new Query(("@f3:f3_Val")));
//    assertEquals(0, res1.getTotalResults());
//
//    assertTrue(client.updateDocument("doc1", 1.0, mm, "@f2=='v2_val'"));
//    // Search for f3 value. All documents should have it.
//    SearchResult res2 = client.ftSearch(index, new Query(("@f3:v3_Val")));
//    assertEquals(1, res2.getTotalResults());
//
//    assertFalse(client.replaceDocument("doc2", 1.0, mm, "@f1=='vv3_Val'"));
//
//    // Search for f3 value. Only one document should have it.
//    SearchResult res3 = client.ftSearch(index, new Query(("@f3:v3_Val")));
//    assertEquals(1, res3.getTotalResults());
//
//    assertTrue(client.replaceDocument("doc2", 1.0, mm, "@f1=='v1_val'"));
//
//    // Search for f3 value. All documents should have it.
//    SearchResult res4 = client.ftSearch(index, new Query(("@f3:v3_Val")));
//    assertEquals(2, res4.getTotalResults());
//  }

  @Test
  public void testExplain() throws Exception {
    Schema sc = new Schema()
        .addTextField("f1", 1.0)
        .addTextField("f2", 1.0)
        .addTextField("f3", 1.0);
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    String res = client.ftExplain(index, new Query("@f3:f3_val @f2:f2_val @f1:f1_val"));
    assertNotNull(res);
    assertFalse(res.isEmpty());
  }

  @Test
  public void testHighlightSummarize() throws Exception {
    Schema sc = new Schema().addTextField("text", 1.0);
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    Map<String, Object> doc = new HashMap<>();
    doc.put("text", "Redis is often referred as a data structures server. What this means is that Redis provides access to mutable data structures via a set of commands, which are sent using a server-client model with TCP sockets and a simple protocol. So different processes can query and modify the same data structures in a shared way");
    // Add a document
//    client.addDocument("foo", 1.0, doc);
    addDocument("foo", doc);
    Query q = new Query("data").highlightFields().summarizeFields();
    SearchResult res = client.ftSearch(index, q);

    assertEquals("is often referred as a <b>data</b> structures server. What this means is that Redis provides... What this means is that Redis provides access to mutable <b>data</b> structures via a set of commands, which are sent using a... So different processes can query and modify the same <b>data</b> structures in a shared... ",
        res.getDocuments().get(0).get("text"));

    q = new Query("data").highlightFields(new Query.HighlightTags("<u>", "</u>")).summarizeFields();
    res = client.ftSearch(index, q);

    assertEquals("is often referred as a <u>data</u> structures server. What this means is that Redis provides... What this means is that Redis provides access to mutable <u>data</u> structures via a set of commands, which are sent using a... So different processes can query and modify the same <u>data</u> structures in a shared... ",
        res.getDocuments().get(0).get("text"));

  }
//
//  @Test
//  public void testLanguage() throws Exception {
//    Schema sc = new Schema().addTextField("text", 1.0);
//    client.ftCreate(index, IndexOptions.defaultOptions(), sc);
//
//    Document d = new Document("doc1").set("text", "hello");
//    AddOptions options = new AddOptions().setLanguage("spanish");
//    assertTrue(client.addDocument(d, options));
//    boolean caught = false;
//
//    options.setLanguage("ybreski");
//    client.deleteDocument(d.getId());
//
//    try {
//      client.addDocument(d, options);
//    } catch (JedisDataException t) {
//      caught = true;
//    }
//    assertTrue(caught);
//  }
//
//  @Test
//  public void testGet() throws Exception {
//    client.ftCreate(index, IndexOptions.defaultOptions(), new Schema().addTextField("txt1", 1.0));
//    client.addDocument(new Document("doc1").set("txt1", "Hello World!"), new AddOptions());
//    Document d = client.getDocument("doc1");
//    assertNotNull(d);
//    assertEquals("Hello World!", d.get("txt1"));
//
//    // Get something that does not exist. Shouldn't explode
//    assertNull(client.getDocument("nonexist"));
//
//    // Test decode=false mode
//    d = client.getDocument("doc1", false);
//    assertNotNull(d);
//    assertTrue(Arrays.equals(SafeEncoder.encode("Hello World!"), (byte[]) d.get("txt1")));
//  }
//
//  @Test
//  public void testMGet() throws Exception {
//    client.ftCreate(index, IndexOptions.defaultOptions(), new Schema().addTextField("txt1", 1.0));
//    client.addDocument(new Document("doc1").set("txt1", "Hello World!1"), new AddOptions());
//    client.addDocument(new Document("doc2").set("txt1", "Hello World!2"), new AddOptions());
//    client.addDocument(new Document("doc3").set("txt1", "Hello World!3"), new AddOptions());
//
//    List<Document> docs = client.getDocuments();
//    assertEquals(0, docs.size());
//
//    docs = client.getDocuments("doc1", "doc3", "doc4");
//    assertEquals(3, docs.size());
//    assertEquals("Hello World!1", docs.get(0).get("txt1"));
//    assertEquals("Hello World!3", docs.get(1).get("txt1"));
//    assertNull(docs.get(2));
//
//    // Test decode=false mode
//    docs = client.getDocuments(false, "doc2");
//    assertEquals(1, docs.size());
//    assertTrue(Arrays.equals(SafeEncoder.encode("Hello World!2"), (byte[]) docs.get(0).get("txt1")));
//  }
//
//  private static void assertUnknownIndex(JedisDataException jde) {
//    assertTrue(jde.getMessage().toLowerCase().contains("unknown index"));
//  }
//
//  @Test
//  public void testAddSuggestionGetSuggestionFuzzy() throws Exception {
//    Suggestion suggestion = Suggestion.builder().str("TOPIC OF WORDS").score(1).build();
//    // test can add a suggestion string
//    assertTrue(suggestion.toString() + " insert should of returned at least 1", client.addSuggestion(suggestion, true) > 0);
//    // test that the partial part of that string will be returned using fuzzy
//
//    assertEquals(suggestion.toString() + " suppose to be returned", suggestion, client.getSuggestion(suggestion.getString().substring(0, 3), SuggestionOptions.builder().build()).get(0));
//  }
//
//  @Test
//  public void testAddSuggestionGetSuggestion() throws Exception {
//    try {
//      Suggestion.builder().str("ANOTHER_WORD").score(3).build();
//      fail("Illegal score");
//    } catch (IllegalStateException e) {
//    }
//
//    try {
//      Suggestion.builder().score(1).build();
//      fail("Missing required string");
//    } catch (IllegalStateException e) {
//    }
//
//    Suggestion suggestion = Suggestion.builder().str("ANOTHER_WORD").score(1).build();
//    Suggestion noMatch = Suggestion.builder().str("_WORD MISSED").score(1).build();
//
//    assertTrue(suggestion.toString() + " should of inserted at least 1", client.addSuggestion(suggestion, false) > 0);
//    assertTrue(noMatch.toString() + " should of inserted at least 1", client.addSuggestion(noMatch, false) > 0);
//
//    // test that with a partial part of that string will have the entire word returned SuggestionOptions.builder().build()
//    assertEquals(suggestion.getString() + " did not get a match with 3 characters", 1, client.getSuggestion(suggestion.getString().substring(0, 3), SuggestionOptions.builder().fuzzy().build()).size());
//
//    // turn off fuzzy start at second word no hit
//    assertEquals(noMatch.getString() + " no fuzzy and starting at 1, should not match", 0, client.getSuggestion(noMatch.getString().substring(1, 6), SuggestionOptions.builder().build()).size());
//    // my attempt to trigger the fuzzy by 1 character
//    assertEquals(noMatch.getString() + " fuzzy is on starting at 1 position should match", 1, client.getSuggestion(noMatch.getString().substring(1, 6), SuggestionOptions.builder().fuzzy().build()).size());
//  }
//
//  @Test
//  public void testAddSuggestionGetSuggestionPayloadScores() throws Exception {
//    Suggestion suggestion = Suggestion.builder().str("COUNT_ME TOO").payload("PAYLOADS ROCK ").score(0.2).build();
//    assertTrue(suggestion.toString() + " insert should of at least returned 1", client.addSuggestion(suggestion, false) > 0);
//    assertTrue("Count single added should return more than 1", client.addSuggestion(suggestion.toBuilder().str("COUNT").payload("My PAYLOAD is better").build(), false) > 1);
//    assertTrue("Count single added should return more than 1", client.addSuggestion(suggestion.toBuilder().str("COUNT_ANOTHER").score(1).payload(null).build(), false) > 1);
//
//    Suggestion noScoreOrPayload = Suggestion.builder().str("COUNT NO PAYLOAD OR COUNT").build();
//    assertTrue("Count single added should return more than 1", client.addSuggestion(noScoreOrPayload, true) > 1);
//
//    List<Suggestion> payloads = client.getSuggestion(suggestion.getString().substring(0, 3), SuggestionOptions.builder().with(SuggestionOptions.With.PAYLOAD_AND_SCORES).build());
//    assertEquals("4 suggestions with scores and payloads ", 4, payloads.size());
//    assertTrue("Assert that a suggestion has a payload ", payloads.get(2).getPayload().length() > 0);
//    assertTrue("Assert that a suggestion has a score not default 1 ", payloads.get(1).getScore() < .299);
//
//  }
//
//  @Test
//  public void testAddSuggestionGetSuggestionPayload() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("COUNT_ME TOO").payload("PAYLOADS ROCK ").build(), false);
//    client.addSuggestion(Suggestion.builder().str("COUNT").payload("ANOTHER PAYLOAD ").build(), false);
//    client.addSuggestion(Suggestion.builder().str("COUNTNO PAYLOAD OR COUNT").build(), false);
//
//    // test that with a partial part of that string will have the entire word returned
//    List<Suggestion> payloads = client.getSuggestion("COU", SuggestionOptions.builder().max(3).fuzzy().with(SuggestionOptions.With.PAYLOAD).build());
//    assertEquals("3 suggestions payloads ", 3, payloads.size());
//
//  }
//
//  @Test
//  public void testGetSuggestionNoPayloadTwoOnly() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("DIFF_WORD").score(0.4).payload("PAYLOADS ROCK ").build(), false);
//    client.addSuggestion(Suggestion.builder().str("DIFF wording").score(0.5).payload("ANOTHER PAYLOAD ").build(), false);
//    client.addSuggestion(Suggestion.builder().str("DIFFERENT").score(0.7).payload("I am a payload").build(), false);
//
//    List<Suggestion> payloads = client.getSuggestion("DIF", SuggestionOptions.builder().max(2).build());
//    assertEquals("3 suggestions should match but only asking for 2 and payloads should have 2 items in array", 2, payloads.size());
//
//    List<Suggestion> three = client.getSuggestion("DIF", SuggestionOptions.builder().max(3).build());
//    assertEquals("3 suggestions and payloads should have 3 items in array", 3, three.size());
//
//  }
//
//  @Test
//  public void testGetSuggestionWithScore() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("DIFF_WORD").score(0.4).payload("PAYLOADS ROCK ").build(), true);
//    List<Suggestion> list = client.getSuggestion("DIF", SuggestionOptions.builder().max(2).with(SuggestionOptions.With.SCORES).build());
//    assertTrue(list.get(0).getScore() <= .2);
//  }
//
//  @Test
//  public void testGetSuggestionAllNoHit() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("NO WORD").score(0.4).build(), false);
//
//    List<Suggestion> none = client.getSuggestion("DIF", SuggestionOptions.builder().max(3).with(SuggestionOptions.With.SCORES).build());
//    assertEquals("Empty list not hit in index for partial word", 0, none.size());
//  }
//
//  @Test
//  public void testAddSuggestionDeleteSuggestionLength() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("TOPIC OF WORDS").score(1).build(), true);
//    client.addSuggestion(Suggestion.builder().str("ANOTHER ENTRY").score(1).build(), true);
//
//    long result = client.deleteSuggestion("ANOTHER ENTRY");
//    assertEquals("The delete of the suggestion should return 1", 1, result);
//    assertEquals(1L, client.getSuggestionLength().longValue());
//
//    result = client.deleteSuggestion("ANOTHER ENTRY THAT IS NOT PRESENT");
//    assertEquals("The delete of the suggestion should return 0", 0, result);
//    assertEquals(1L, client.getSuggestionLength().longValue());
//  }
//
//  @Test
//  public void testAddSuggestionGetSuggestionLength() throws Exception {
//    client.addSuggestion(Suggestion.builder().str("TOPIC OF WORDS").score(1).build(), true);
//    client.addSuggestion(Suggestion.builder().str("ANOTHER ENTRY").score(1).build(), true);
//    assertEquals(2L, client.getSuggestionLength().longValue());
//
//    client.addSuggestion(Suggestion.builder().str("FINAL ENTRY").score(1).build(), true);
//    assertEquals(3L, client.getSuggestionLength().longValue());
//  }

  @Test
  public void getTagField() {
    Schema sc = new Schema()
        .addTextField("title", 1.0)
        .addTagField("category");

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields1 = new HashMap<>();
    fields1.put("title", "hello world");
    fields1.put("category", "red");
//    assertTrue(client.addDocument("foo", fields1));
    addDocument("foo", fields1);
    Map<String, Object> fields2 = new HashMap<>();
    fields2.put("title", "hello world");
    fields2.put("category", "blue");
//    assertTrue(client.addDocument("bar", fields2));
    addDocument("bar", fields2);
    Map<String, Object> fields3 = new HashMap<>();
    fields3.put("title", "hello world");
    fields3.put("category", "green,yellow");
//    assertTrue(client.addDocument("baz", fields3));
    addDocument("baz", fields3);
    Map<String, Object> fields4 = new HashMap<>();
    fields4.put("title", "hello world");
    fields4.put("category", "orange;purple");
//    assertTrue(client.addDocument("qux", fields4));
    addDocument("qux", fields4);

    assertEquals(1, client.ftSearch(index, new Query("@category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{yellow}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{purple}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{orange\\;purple}")).getTotalResults());
    assertEquals(4, client.ftSearch(index, new Query("hello")).getTotalResults());
  }

  @Test
  public void testGetTagFieldWithNonDefaultSeparator() {
    Schema sc = new Schema()
        .addTextField("title", 1.0)
        .addTagField("category", ";");

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields1 = new HashMap<>();
    fields1.put("title", "hello world");
    fields1.put("category", "red");
//    assertTrue(client.addDocument("foo", fields1));
    addDocument("foo", fields1);
    Map<String, Object> fields2 = new HashMap<>();
    fields2.put("title", "hello world");
    fields2.put("category", "blue");
//    assertTrue(client.addDocument("bar", fields2));
    addDocument("bar", fields2);
    Map<String, Object> fields3 = new HashMap<>();
    fields3.put("title", "hello world");
    fields3.put("category", "green;yellow");
    addDocument("baz", fields3);
//    assertTrue(client.addDocument("baz", fields3));
    Map<String, Object> fields4 = new HashMap<>();
    fields4.put("title", "hello world");
    fields4.put("category", "orange,purple");
//    assertTrue(client.addDocument("qux", fields4));
    addDocument("qux", fields4);

    assertEquals(1, client.ftSearch(index, new Query("@category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{yellow}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{purple}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{orange\\,purple}")).getTotalResults());
    assertEquals(4, client.ftSearch(index, new Query("hello")).getTotalResults());
  }
//
//  @Test
//  public void testMultiDocuments() {
//    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);
//
//    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
//
//    Map<String, Object> fields = new HashMap<>();
//    fields.put("title", "hello world");
//    fields.put("body", "lorem ipsum");
//
//    boolean[] results = client.addDocuments(new Document("doc1", fields), new Document("doc2", fields), new Document("doc3", fields));
//
//    assertArrayEquals(new boolean[]{true, true, true}, results);
//
//    assertEquals(3, client.ftSearch(index, new Query("hello world")).getTotalResults());
//
//    results = client.addDocuments(new Document("doc4", fields), new Document("doc2", fields), new Document("doc5", fields));
//    assertArrayEquals(new boolean[]{true, false, true}, results);
//
//    results = client.deleteDocuments(true, "doc1", "doc2", "doc36");
//    assertArrayEquals(new boolean[]{true, true, false}, results);
//  }

  @Test
  public void testReturnFields() throws Exception {
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value1");
    doc.put("field2", "value2");
    // Store it
//    assertTrue(client.addDocument("doc", doc));
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, new Query("*").returnFields("field1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("value1", res.getDocuments().get(0).get("field1"));
    assertNull(res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void returnWithFieldNames() throws Exception {
    Schema sc = new Schema().addTextField("a", 1).addTextField("b", 1).addTextField("c", 1);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> map = new HashMap<>();
    map.put("a", "value1");
    map.put("b", "value2");
    map.put("c", "value3");
//    assertTrue(client.addDocument("doc", map));
    addDocument("doc", map);

    // Query
    SearchResult res = client.ftSearch(index,
        new Query().returnFields(FieldName.of("a"), FieldName.of("b").as("d")));
    assertEquals(1, res.getTotalResults());
    Document doc = res.getDocuments().get(0);
    assertEquals("value1", doc.get("a"));
    assertNull(doc.get("b"));
    assertEquals("value2", doc.get("d"));
    assertNull(doc.get("c"));
  }

  @Test
  public void inKeys() throws Exception {
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", "not");

    // Store it
//    assertTrue(client.addDocument("doc1", doc));
    addDocument("doc1", doc);
//    assertTrue(client.addDocument("doc2", doc));
    addDocument("doc2", doc);

    // Query
    SearchResult res = client.ftSearch(index, new Query("value").limitKeys("doc1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals(null, res.getDocuments().get(0).get("value"));
  }

  @Test
  public void blobField() throws Exception {
    Schema sc = new Schema().addTextField("field1", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    byte[] blob = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", blob);

    // Store it
//    assertTrue(client.addDocument("doc1", doc));
    addDocument("doc1", doc);

    // Query
//    SearchResult res = client.ftSearch(index, new Query("value"), false);
    SearchResult res = client.ftSearch(SafeEncoder.encode(index), new Query("value"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).getString("field1"));
    assertArrayEquals(blob, (byte[]) res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void config() throws Exception {
    assertEquals("OK", client.ftConfigSet("timeout", "100"));
    Map<String, String> configMap = client.ftConfigGet("*");
    assertEquals("100", configMap.get("TIMEOUT"));
  }

  @Test
  public void configOnTimeout() throws Exception {
    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "fail"));
    assertEquals(singletonMap("ON_TIMEOUT", "fail"), client.ftConfigGet("ON_TIMEOUT"));

    try {
      client.ftConfigSet("ON_TIMEOUT", "null");
      fail("null is not valid value for ON_TIMEOUT");
    } catch (JedisDataException e) {
      // Should throw an exception after RediSearch 2.2
    }
  }

  @Test
  public void alias() throws Exception {
    Schema sc = new Schema().addTextField("field1", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
//    assertTrue(client.addDocument("doc1", doc));
    addDocument("doc1", doc);

    assertEquals("OK", client.ftAliasAdd("ALIAS1", index));
    SearchResult res1 = client.ftSearch("ALIAS1", new Query("*").returnFields("field1"));
    assertEquals(1, res1.getTotalResults());
    assertEquals("value", res1.getDocuments().get(0).get("field1"));

    assertEquals("OK", client.ftAliasUpdate("ALIAS2", index));
    SearchResult res2 = client.ftSearch("ALIAS2", new Query("*").returnFields("field1"));
    assertEquals(1, res2.getTotalResults());
    assertEquals("value", res2.getDocuments().get(0).get("field1"));

    try {
      client.ftAliasDel("ALIAS3");
      fail("Should throw JedisDataException");
    } catch (JedisDataException e) {
      // Alias does not exist
    }
    assertEquals("OK", client.ftAliasDel("ALIAS2"));
    try {
      client.ftAliasDel("ALIAS2");
      fail("Should throw JedisDataException");
    } catch (JedisDataException e) {
      // Alias does not exist
    }
  }

  @Test
  public void syn() throws Exception {
    Schema sc = new Schema().addTextField("name", 1.0).addTextField("addr", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    long group1 = 345L;
    long group2 = 789L;
    String group1_str = Long.toString(group1);
    String group2_str = Long.toString(group2);
    assertEquals("OK", client.ftSynUpdate(index, group1_str, "girl", "baby"));
    assertEquals("OK", client.ftSynUpdate(index, group1_str, "child"));
    assertEquals("OK", client.ftSynUpdate(index, group2_str, "child"));

    Map<String, List<String>> dump = client.ftSynDump(index);

    Map<String, List<String>> expected = new HashMap<>();
    expected.put("girl", Arrays.asList(group1_str));
    expected.put("baby", Arrays.asList(group1_str));
    expected.put("child", Arrays.asList(group1_str, group2_str));
    assertEquals(expected, dump);
  }
}
