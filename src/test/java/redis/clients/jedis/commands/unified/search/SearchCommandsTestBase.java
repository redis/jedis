package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.Schema.*;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Base test class for Search commands using the UnifiedJedis pattern. Tests FT.CREATE, FT.SEARCH,
 * FT.DROP, etc.
 */
@Tag("search")
public abstract class SearchCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String INDEX = "testindex";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public SearchCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  protected void addDocument(String key, Map<String, Object> map) {
    jedis.hset(key, RediSearchUtil.toStringMap(map));
  }

  protected static Map<String, Object> toMap(Object... values) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }

  protected static Map<String, String> toStringMap(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  @Test
  public void create() throws Exception {
    Schema sc = new Schema().addTextField("first", 1.0).addTextField("last", 1.0)
        .addNumericField("age");
    IndexDefinition rule = new IndexDefinition().setFilter("@age>16")
        .setPrefixes(new String[] { "student:", "pupil:" });

    assertEquals("OK",
      jedis.ftCreate(INDEX, IndexOptions.defaultOptions().setDefinition(rule), sc));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = jedis.ftSearch(INDEX, new Query());
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = jedis.ftSearch(INDEX, new Query("@first:Jo*"));
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = jedis.ftSearch(INDEX, new Query("@first:Pat"));
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = jedis.ftSearch(INDEX, new Query("@last:Rod"));
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void createNoParams() {
    Schema sc = new Schema().addTextField("first", 1.0).addTextField("last", 1.0)
        .addNumericField("age");
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    addDocument("student:1111", toMap("first", "Joe", "last", "Dod", "age", 18));
    addDocument("student:3333", toMap("first", "El", "last", "Mark", "age", 17));
    addDocument("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", 21));
    addDocument("student:5555", toMap("first", "Joen", "last", "Ko", "age", 20));

    SearchResult noFilters = jedis.ftSearch(INDEX, new Query());
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = jedis.ftSearch(INDEX, new Query("@first:Jo*"));
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = jedis.ftSearch(INDEX, new Query("@first:Pat"));
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = jedis.ftSearch(INDEX, new Query("@last:Rod"));
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void createWithFieldNames() {
    Schema sc = new Schema().addField(new TextField(FieldName.of("first").as("given")))
        .addField(new TextField(FieldName.of("last")));
    IndexDefinition rule = new IndexDefinition().setPrefixes(new String[] { "student:", "pupil:" });

    assertEquals("OK",
      jedis.ftCreate(INDEX, IndexOptions.defaultOptions().setDefinition(rule), sc));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = jedis.ftSearch(INDEX, new Query());
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asAttribute = jedis.ftSearch(INDEX, new Query("@given:Jo*"));
    assertEquals(2, asAttribute.getTotalResults());

    SearchResult nonAttribute = jedis.ftSearch(INDEX, new Query("@last:Rod"));
    assertEquals(1, nonAttribute.getTotalResults());
  }

  @Test
  public void alterAdd() {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }
    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", jedis.ftAlter(INDEX, new TagField("tags", ","), new TextField("name", 0.5)));
    for (int i = 0; i < 100; i++) {
      Map<String, Object> fields2 = new HashMap<>();
      fields2.put("name", "name" + i);
      fields2.put("tags", String.format("tagA,tagB,tag%d", i));
      addDocument(String.format("doc%d", i), fields2);
    }
    SearchResult res2 = jedis.ftSearch(INDEX, new Query("@tags:{tagA}"));
    assertEquals(100, res2.getTotalResults());
  }

  @Test
  public void search() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world").limit(0, 5).setWithScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(5, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }

    jedis.del("doc0");

    res = jedis.ftSearch(INDEX, new Query("hello world"));
    assertEquals(99, res.getTotalResults());

    assertEquals("OK", jedis.ftDropIndex(INDEX));
    try {
      jedis.ftSearch(INDEX, new Query("hello world"));
      fail();
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void numericFilter() throws Exception {
    Schema sc = new Schema().addTextField("title", 1.0).addNumericField("price");

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");

    for (int i = 0; i < 100; i++) {
      fields.put("price", i);
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX,
      new Query("hello world").addFilter(new Query.NumericFilter("price", 0, 49)));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 0);
      assertTrue(price <= 49);
    }

    res = jedis.ftSearch(INDEX,
      new Query("hello world").addFilter(new Query.NumericFilter("price", 0, true, 49, true)));
    assertEquals(48, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price > 0);
      assertTrue(price < 49);
    }
    res = jedis.ftSearch(INDEX,
      new Query("hello world").addFilter(new Query.NumericFilter("price", 50, 100)));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 50);
      assertTrue(price <= 100);
    }

    res = jedis.ftSearch(INDEX, new Query("hello world")
        .addFilter(new Query.NumericFilter("price", 20, Double.POSITIVE_INFINITY)));
    assertEquals(80, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    res = jedis.ftSearch(INDEX, new Query("hello world")
        .addFilter(new Query.NumericFilter("price", Double.NEGATIVE_INFINITY, 10)));
    assertEquals(11, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
  }

  @Test
  public void stopwords() {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK",
      jedis.ftCreate(INDEX, IndexOptions.defaultOptions().setStopwords("foo", "bar", "baz"), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    addDocument("doc1", fields);
    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world"));
    assertEquals(1, res.getTotalResults());
    res = jedis.ftSearch(INDEX, new Query("foo bar"));
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void noStopwords() {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions().setNoStopwords(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar to be or not to be");
    addDocument("doc1", fields);

    assertEquals(1, jedis.ftSearch(INDEX, new Query("hello world")).getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, new Query("foo bar")).getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, new Query("to be or not to be")).getTotalResults());
  }

  @Test
  public void geoFilter() {
    Schema sc = new Schema().addTextField("title", 1.0).addGeoField("loc");

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", "-0.441,51.458");
    addDocument("doc1", fields);
    fields.put("loc", "-0.1,51.2");
    addDocument("doc2", fields);

    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world")
        .addFilter(new Query.GeoFilter("loc", -0.44, 51.45, 10, Query.GeoFilter.KILOMETERS)));

    assertEquals(1, res.getTotalResults());
    res = jedis.ftSearch(INDEX, new Query("hello world")
        .addFilter(new Query.GeoFilter("loc", -0.44, 51.45, 100, Query.GeoFilter.KILOMETERS)));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void dropIndex() {
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", jedis.ftDropIndex(INDEX));

    try {
      jedis.ftSearch(INDEX, new Query("hello world"));
      fail("Index should not exist.");
    } catch (JedisDataException de) {
      assertThat(de.getMessage(), anyOf(containsStringIgnoringCase("no such index"), // Redis Search
                                                                                     // <v8.7.90
        containsString("SEARCH_INDEX_NOT_FOUND") // Redis Search v8.7.90+
      ));
    }
    assertEquals(100, jedis.dbSize());
  }

  @Test
  public void dropIndexDD() {
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", jedis.ftDropIndexDD(INDEX));

    Set<String> keys = jedis.keys("*");
    assertTrue(keys.isEmpty());
    assertEquals(0, jedis.dbSize());
  }

  @Test
  public void testHighlightSummarize() {
    Schema sc = new Schema().addTextField("text", 1.0);
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    Map<String, Object> doc = new HashMap<>();
    doc.put("text",
      "Redis is often referred as a data structures server. What this means is that Redis provides access to mutable data structures via a set of commands, which are sent using a server-client model with TCP sockets and a simple protocol. So different processes can query and modify the same data structures in a shared way");
    addDocument("foo", doc);
    Query q = new Query("data").highlightFields().summarizeFields();
    SearchResult res = jedis.ftSearch(INDEX, q);

    assertEquals(
      "is often referred as a <b>data</b> structures server. What this means is that Redis provides... What this means is that Redis provides access to mutable <b>data</b> structures via a set of commands, which are sent using a... So different processes can query and modify the same <b>data</b> structures in a shared... ",
      res.getDocuments().get(0).get("text"));

    q = new Query("data").highlightFields(new Query.HighlightTags("<u>", "</u>")).summarizeFields();
    res = jedis.ftSearch(INDEX, q);

    assertEquals(
      "is often referred as a <u>data</u> structures server. What this means is that Redis provides... What this means is that Redis provides access to mutable <u>data</u> structures via a set of commands, which are sent using a... So different processes can query and modify the same <u>data</u> structures in a shared... ",
      res.getDocuments().get(0).get("text"));
  }

  @Test
  public void alias() {
    Schema sc = new Schema().addTextField("field1", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    addDocument("doc1", doc);

    assertEquals("OK", jedis.ftAliasAdd("ALIAS1", INDEX));
    SearchResult res1 = jedis.ftSearch("ALIAS1", new Query("*").returnFields("field1"));
    assertEquals(1, res1.getTotalResults());
    assertEquals("value", res1.getDocuments().get(0).get("field1"));

    assertEquals("OK", jedis.ftAliasUpdate("ALIAS2", INDEX));
    SearchResult res2 = jedis.ftSearch("ALIAS2", new Query("*").returnFields("field1"));
    assertEquals(1, res2.getTotalResults());
    assertEquals("value", res2.getDocuments().get(0).get("field1"));

    try {
      jedis.ftAliasDel("ALIAS3");
      fail("Should throw JedisDataException");
    } catch (JedisDataException e) {
    }
    assertEquals("OK", jedis.ftAliasDel("ALIAS2"));
    try {
      jedis.ftAliasDel("ALIAS2");
      fail("Should throw JedisDataException");
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void info() throws Exception {
    Schema sc = new Schema().addTextField("title", 5.0).addSortableTextField("plot", 1.0)
        .addSortableTagField("genre", ",").addSortableNumericField("release_year")
        .addSortableNumericField("rating").addSortableNumericField("votes");

    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    Map<String, Object> info = jedis.ftInfo(INDEX);
    assertEquals(INDEX, info.get("index_name"));
    assertEquals(6, ((List) info.get("attributes")).size());
    if (AssertUtil.expectsResp3OnWire(protocol)) {
      assertEquals(0L, ((Map) info.get("cursor_stats")).get("global_idle"));
    } else {
      assertEquals("global_idle", ((List) info.get("cursor_stats")).get(0));
    }
  }

  @Test
  public void synonym() {
    Schema sc = new Schema().addTextField("name", 1.0).addTextField("addr", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    long group1 = 345L;
    long group2 = 789L;
    String group1_str = Long.toString(group1);
    String group2_str = Long.toString(group2);
    assertEquals("OK", jedis.ftSynUpdate(INDEX, group1_str, "girl", "baby"));
    assertEquals("OK", jedis.ftSynUpdate(INDEX, group1_str, "child"));
    assertEquals("OK", jedis.ftSynUpdate(INDEX, group2_str, "child"));

    Map<String, List<String>> dump = jedis.ftSynDump(INDEX);

    Map<String, List<String>> expected = new HashMap<>();
    expected.put("girl", Arrays.asList(group1_str));
    expected.put("baby", Arrays.asList(group1_str));
    expected.put("child", Arrays.asList(group1_str, group2_str));
    assertEquals(expected, dump);
  }

  @Test
  public void testExplain() {
    Schema sc = new Schema().addTextField("f1", 1.0).addTextField("f2", 1.0).addTextField("f3",
      1.0);
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    String res = jedis.ftExplain(INDEX, new Query("@f3:f3_val @f2:f2_val @f1:f1_val"));
    assertNotNull(res);
    assertFalse(res.isEmpty());
  }

  @Test
  public void testNullField() {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("genre", 1.0)
        .addTextField("plot", 1.0).addSortableNumericField("release_year").addTagField("tag")
        .addGeoField("loc");
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    // create a document with a field set to null
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "another test with title ");
    fields.put("genre", "Comedy");
    fields.put("plot", "this is the plot for the test");
    fields.put("tag", "fun");
    fields.put("release_year", 2019);
    fields.put("loc", "-0.1,51.2");

    addDocument("doc1", fields);
    SearchResult res = jedis.ftSearch(INDEX, new Query("title"));
    assertEquals(1, res.getTotalResults());

    fields = new HashMap<>();
    fields.put("title", "another title another test");
    fields.put("genre", "Action");
    fields.put("plot", null);
    fields.put("tag", null);

    try {
      addDocument("doc2", fields);
      fail("Should throw NullPointerException.");
    } catch (NullPointerException e) {
    }

    res = jedis.ftSearch(INDEX, new Query("title"));
    assertEquals(1, res.getTotalResults());

    // Testing with numerical value
    fields = new HashMap<>();
    fields.put("title", "another title another test");
    fields.put("genre", "Action");
    fields.put("release_year", null);
    try {
      addDocument("doc2", fields);
      fail("Should throw NullPointerException.");
    } catch (NullPointerException e) {
    }
    res = jedis.ftSearch(INDEX, new Query("title"));
    assertEquals(1, res.getTotalResults());
  }

  @Test
  public void blobField() {
    assumeFalse(AssertUtil.expectsResp3OnWire(protocol)); // not supporting

    Schema sc = new Schema().addTextField("field1", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    byte[] blob = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", blob);

    // Store it
    addDocument("doc1", doc);

    // Query
    SearchResult res = jedis.ftSearch(SafeEncoder.encode(INDEX), new Query("value"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).getString("field1"));
    assertArrayEquals(blob, (byte[]) res.getDocuments().get(0).get("field2"));
  }
}
