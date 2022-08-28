package redis.clients.jedis.modules.search;

import static org.junit.Assert.*;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.*;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class SearchWithParamsTest extends RedisModuleCommandsTestBase {

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
    client.hset(key, RediSearchUtil.toStringMap(map));
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
  public void create() {
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams()
            .filter("@age>16")
            .prefix("student:", "pupil:"),
        TextField.of("first"), TextField.of("last"), NumericField.of("age")));

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
  public void createNoParams() {
    assertOK(client.ftCreate(index,
        TextField.of("first").weight(1),
        TextField.of("last").weight(1),
        NumericField.of("age")));

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
  public void createWithFieldNames() {
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams()
            .addPrefix("student:").addPrefix("pupil:"),
        TextField.of("first").as("given"),
        TextField.of(FieldName.of("last").as("family"))));

    client.hset("profesor:5555", toMap("first", "Albert", "last", "Blue", "age", "55"));
    client.hset("student:1111", toMap("first", "Joe", "last", "Dod", "age", "18"));
    client.hset("pupil:2222", toMap("first", "Jen", "last", "Rod", "age", "14"));
    client.hset("student:3333", toMap("first", "El", "last", "Mark", "age", "17"));
    client.hset("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", "21"));
    client.hset("student:5555", toMap("first", "Joen", "last", "Ko", "age", "20"));
    client.hset("teacher:6666", toMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asFirst = client.ftSearch(index, new Query("@first:Jo*"));
    assertEquals(0, asFirst.getTotalResults());

    SearchResult asGiven = client.ftSearch(index, new Query("@given:Jo*"));
    assertEquals(2, asGiven.getTotalResults());

    SearchResult nonLast = client.ftSearch(index, new Query("@last:Rod"));
    assertEquals(0, nonLast.getTotalResults());

    SearchResult asFamily = client.ftSearch(index, new Query("@family:Rod"));
    assertEquals(1, asFamily.getTotalResults());
  }

  @Test
  public void alterAdd() {
    assertOK(client.ftCreate(index, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }
    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertOK(client.ftAlter(index,
        TagField.of("tags"),
        TextField.of("name").weight(0.5)));

    for (int i = 0; i < 100; i++) {
      Map<String, Object> fields2 = new HashMap<>();
      fields2.put("name", "name" + i);
      fields2.put("tags", String.format("tagA,tagB,tag%d", i));
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
  public void search() {
    assertOK(client.ftCreate(index, FTCreateParams.createParams(),
        TextField.of("title"), TextField.of("body")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, new Query("hello world").limit(0, 5).setWithScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(5, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }

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

  @Test
  public void numericFilter() {
    assertOK(client.ftCreate(index, TextField.of("title"), NumericField.of("price")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");

    for (int i = 0; i < 100; i++) {
      fields.put("price", i);
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
  public void stopwords() {
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams()
            .stopwords("foo", "bar", "baz"),
        TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    addDocument("doc1", fields);
    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(1, res.getTotalResults());
    res = client.ftSearch(index, new Query("foo bar"));
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void noStopwords() {
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams().noStopwords(),
        TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    fields.put("title", "hello world foo bar to be or not to be");
    addDocument("doc1", fields);

    assertEquals(1, client.ftSearch(index, new Query("hello world")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("foo bar")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("to be or not to be")).getTotalResults());
  }

  @Test
  public void geoFilter() {
    assertOK(client.ftCreate(index, TextField.of("title"), GeoField.of("loc")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", "-0.441,51.458");
    addDocument("doc1", fields);

    fields.put("loc", "-0.1,51.2");
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
  public void geoFilterAndGeoCoordinateObject() {
    assertOK(client.ftCreate(index, TextField.of("title"), GeoField.of("loc")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", new redis.clients.jedis.GeoCoordinate(-0.441, 51.458));
    addDocument("doc1", fields);

    fields.put("loc", new redis.clients.jedis.GeoCoordinate(-0.1, 51.2));
    addDocument("doc2", fields);

    SearchResult res = client.ftSearch(index, new Query("hello world").addFilter(
        new Query.GeoFilter("loc", -0.44, 51.45, 10, Query.GeoFilter.KILOMETERS)));
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, new Query("hello world").addFilter(
        new Query.GeoFilter("loc", -0.44, 51.45, 100, Query.GeoFilter.KILOMETERS)));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void getTagField() {
    assertOK(client.ftCreate(index, TextField.of("title"), TagField.of("category")));

    Map<String, Object> fields1 = new HashMap<>();
    fields1.put("title", "hello world");
    fields1.put("category", "red");
    addDocument("foo", fields1);

    Map<String, Object> fields2 = new HashMap<>();
    fields2.put("title", "hello world");
    fields2.put("category", "blue");
    addDocument("bar", fields2);

    Map<String, Object> fields3 = new HashMap<>();
    fields3.put("title", "hello world");
    fields3.put("category", "green,yellow");
    addDocument("baz", fields3);

    Map<String, Object> fields4 = new HashMap<>();
    fields4.put("title", "hello world");
    fields4.put("category", "orange;purple");
    addDocument("qux", fields4);

    assertEquals(1, client.ftSearch(index, new Query("@category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{yellow}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{purple}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{orange\\;purple}")).getTotalResults());
    assertEquals(4, client.ftSearch(index, new Query("hello")).getTotalResults());

    assertEquals(new HashSet<>(Arrays.asList("red", "blue", "green", "yellow", "orange;purple")),
        client.ftTagVals(index, "category"));
  }

  @Test
  public void testGetTagFieldWithNonDefaultSeparator() {
    assertOK(client.ftCreate(index,
        TextField.of("title"),
        TagField.of("category").separator(';')));

    Map<String, Object> fields1 = new HashMap<>();
    fields1.put("title", "hello world");
    fields1.put("category", "red");
    addDocument("foo", fields1);

    Map<String, Object> fields2 = new HashMap<>();
    fields2.put("title", "hello world");
    fields2.put("category", "blue");
    addDocument("bar", fields2);

    Map<String, Object> fields3 = new HashMap<>();
    fields3.put("title", "hello world");
    fields3.put("category", "green;yellow");
    addDocument("baz", fields3);

    Map<String, Object> fields4 = new HashMap<>();
    fields4.put("title", "hello world");
    fields4.put("category", "orange,purple");
    addDocument("qux", fields4);

    assertEquals(1, client.ftSearch(index, new Query("@category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{red}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{blue}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello @category:{yellow}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{purple}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{orange\\,purple}")).getTotalResults());
    assertEquals(4, client.ftSearch(index, new Query("hello")).getTotalResults());

    assertEquals(new HashSet<>(Arrays.asList("red", "blue", "green", "yellow", "orange,purple")),
        client.ftTagVals(index, "category"));
  }

  @Test
  public void caseSensitiveTagField() {
    assertOK(client.ftCreate(index,
        TextField.of("title"),
        TagField.of("category").caseSensitive()));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("category", "RedX");
    addDocument("foo", fields);

    assertEquals(0, client.ftSearch(index, new Query("@category:{redx}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{redX}")).getTotalResults());
    assertEquals(0, client.ftSearch(index, new Query("@category:{Redx}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("@category:{RedX}")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("hello")).getTotalResults());
  }

  @Test
  public void testHNSWVVectorSimilarity() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    assertOK(client.ftCreate(index, VectorField.builder().fieldName("v")
        .algorithm(VectorField.VectorAlgorithm.HNSW).attributes(attr).build()));

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

    assertOK(client.ftCreate(index,
        VectorField.builder().fieldName("v")
            .algorithm(VectorField.VectorAlgorithm.FLAT)
            .addAttribute("TYPE", "FLOAT32")
            .addAttribute("DIM", 2)
            .addAttribute("DISTANCE_METRIC", "L2")
            .build()
    ));

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
}
