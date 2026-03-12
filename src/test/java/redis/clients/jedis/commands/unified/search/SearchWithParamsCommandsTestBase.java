package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static redis.clients.jedis.util.AssertUtil.assertOK;
import static redis.clients.jedis.util.RedisConditions.ModuleVersion.SEARCH_MOD_VER_80M3;

import java.util.*;
import java.util.stream.Collectors;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.RediSearchUtil;
import redis.clients.jedis.search.schemafields.*;
import redis.clients.jedis.search.schemafields.GeoShapeField.CoordinateSystem;
import redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm;
import redis.clients.jedis.util.RedisConditions;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * Base test class for Search commands with params using the UnifiedJedis pattern.
 */
@Tag("integration")
@Tag("search")
public abstract class SearchWithParamsCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String INDEX = "testindex";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public SearchWithParamsCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @AfterEach
  public void cleanUp() {
    if (jedis.ftList().contains(INDEX)) {
      jedis.ftDropIndex(INDEX);
    }
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
  public void create() {
    assertOK(jedis.ftCreate(INDEX,
      FTCreateParams.createParams().filter("@age>16").prefix("student:", "pupil:"),
      TextField.of("first"), TextField.of("last"), NumericField.of("age")));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = jedis.ftSearch(INDEX);
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = jedis.ftSearch(INDEX, "@first:Jo*");
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = jedis.ftSearch(INDEX, "@first:Pat");
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = jedis.ftSearch(INDEX, "@last:Rod");
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void createNoParams() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("first").weight(1), TextField.of("last").weight(1),
      NumericField.of("age")));

    addDocument("student:1111", toMap("first", "Joe", "last", "Dod", "age", 18));
    addDocument("student:3333", toMap("first", "El", "last", "Mark", "age", 17));
    addDocument("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", 21));
    addDocument("student:5555", toMap("first", "Joen", "last", "Ko", "age", 20));

    SearchResult noFilters = jedis.ftSearch(INDEX);
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = jedis.ftSearch(INDEX, "@first:Jo*");
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = jedis.ftSearch(INDEX, "@first:Pat");
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = jedis.ftSearch(INDEX, "@last:Rod");
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void createWithFieldNames() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams().prefix("student:", "pupil:"),
      TextField.of(FieldName.of("first").as("given")), TextField.of("last")));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    SearchResult noFilters = jedis.ftSearch(INDEX);
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asAttribute = jedis.ftSearch(INDEX, "@given:Jo*");
    assertEquals(2, asAttribute.getTotalResults());

    SearchResult nonAttribute = jedis.ftSearch(INDEX, "@last:Rod");
    assertEquals(1, nonAttribute.getTotalResults());
  }

  @Test
  public void alterAdd() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }
    SearchResult res = jedis.ftSearch(INDEX, "hello world");
    assertEquals(100, res.getTotalResults());

    assertOK(jedis.ftAlter(INDEX, TagField.of("tags"), TextField.of("name").weight(0.5)));

    for (int i = 0; i < 100; i++) {
      Map<String, Object> fields2 = new HashMap<>();
      fields2.put("name", "name" + i);
      fields2.put("tags", String.format("tagA,tagB,tag%d", i));
      addDocument(String.format("doc%d", i), fields2);
    }
    SearchResult res2 = jedis.ftSearch(INDEX, "@tags:{tagA}");
    assertEquals(100, res2.getTotalResults());
  }

  @Test
  public void search() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams(), TextField.of("title"),
      TextField.of("body")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult result = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().limit(0, 5).withScores());
    assertEquals(100, result.getTotalResults());
    assertEquals(5, result.getDocuments().size());
    for (Document d : result.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }

    jedis.del("doc0");

    result = jedis.ftSearch(INDEX, "hello world");
    assertEquals(99, result.getTotalResults());

    assertEquals("OK", jedis.ftDropIndex(INDEX));
    try {
      jedis.ftSearch(INDEX, "hello world");
      fail();
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void numericFilter() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), NumericField.of("price")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");

    for (int i = 0; i < 100; i++) {
      fields.put("price", i);
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().filter("price", 0, 49));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 0);
      assertTrue(price <= 49);
    }

    res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().filter("price", 0, true, 49, true));
    assertEquals(48, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price > 0);
      assertTrue(price < 49);
    }

    res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().filter("price", 50, 100));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 50);
      assertTrue(price <= 100);
    }

    res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().filter("price", 20, Double.POSITIVE_INFINITY));
    assertEquals(80, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().filter("price", Double.NEGATIVE_INFINITY, 10));
    assertEquals(11, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
  }

  @Test
  public void noStopwords() {
    assertOK(
      jedis.ftCreate(INDEX, FTCreateParams.createParams().noStopwords(), TextField.of("title")));
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar to be or not to be");
    addDocument("doc1", fields);

    assertEquals(1, jedis.ftSearch(INDEX, "hello world").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "foo bar").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "to be or not to be").getTotalResults());
  }

  @Test
  public void geoFilter() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), GeoField.of("loc")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", "-0.441,51.458");
    addDocument("doc1", fields);
    fields.put("loc", "-0.1,51.2");
    addDocument("doc2", fields);

    SearchResult res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().geoFilter("loc", -0.44, 51.45, 10, GeoUnit.KM));
    assertEquals(1, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "hello world",
      FTSearchParams.searchParams().geoFilter("loc", -0.44, 51.45, 100, GeoUnit.KM));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void dropIndex() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, "hello world");
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", jedis.ftDropIndex(INDEX));

    try {
      jedis.ftSearch(INDEX, "hello world");
      fail("Index should not exist.");
    } catch (JedisDataException de) {
      assertTrue(de.getMessage().toLowerCase().contains("no such index"));
    }
    assertEquals(100, jedis.dbSize());
  }

  @Test
  public void dropIndexDD() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, "hello world");
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", jedis.ftDropIndexDD(INDEX));

    Set<String> keys = jedis.keys("*");
    assertTrue(keys.isEmpty());
    assertEquals(0, jedis.dbSize());
  }

  @Test
  public void alias() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    addDocument("doc1", doc);

    assertOK(jedis.ftAliasAdd("ALIAS1", INDEX));
    SearchResult res1 = jedis.ftSearch("ALIAS1", "*",
      FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res1.getTotalResults());
    assertEquals("value", res1.getDocuments().get(0).get("field1"));

    assertOK(jedis.ftAliasUpdate("ALIAS2", INDEX));
    SearchResult res2 = jedis.ftSearch("ALIAS2", "*",
      FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res2.getTotalResults());
    assertEquals("value", res2.getDocuments().get(0).get("field1"));

    assertThrows(JedisDataException.class, () -> jedis.ftAliasDel("ALIAS3"));
    assertOK(jedis.ftAliasDel("ALIAS2"));
    assertThrows(JedisDataException.class, () -> jedis.ftAliasDel("ALIAS2"));
  }

  @Test
  public void synonym() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("name"), TextField.of("addr")));

    long group1 = 345L;
    long group2 = 789L;
    String group1_str = Long.toString(group1);
    String group2_str = Long.toString(group2);
    assertOK(jedis.ftSynUpdate(INDEX, group1_str, "girl", "baby"));
    assertOK(jedis.ftSynUpdate(INDEX, group1_str, "child"));
    assertOK(jedis.ftSynUpdate(INDEX, group2_str, "child"));

    Map<String, List<String>> dump = jedis.ftSynDump(INDEX);

    Map<String, List<String>> expected = new HashMap<>();
    expected.put("girl", Arrays.asList(group1_str));
    expected.put("baby", Arrays.asList(group1_str));
    expected.put("child", Arrays.asList(group1_str, group2_str));
    assertEquals(expected, dump);
  }

  @Test
  public void ftExplain() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("f1"), TextField.of("f2"), TextField.of("f3")));

    String res = jedis.ftExplain(INDEX, new Query("@f3:f3_val @f2:f2_val @f1:f1_val"));
    assertThat(res, Matchers.not(Matchers.emptyOrNullString()));
  }

  @Test
  public void ftList() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1")));

    Set<String> indices = jedis.ftList();
    assertTrue(indices.contains(INDEX));
  }
}
