package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
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
    assertThrows(JedisDataException.class, () -> jedis.ftSearch(INDEX, "hello world"));
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

  @Test
  public void textFieldParams() {
    assertOK(jedis.ftCreate("testindex",
      TextField.of("title").weight(2.5).noStem().phonetic("dm:en").withSuffixTrie().sortable()));

    assertOK(jedis.ftCreate("testunfindex",
      TextField.of("title").weight(2.5).noStem().phonetic("dm:en").withSuffixTrie().sortableUNF()));

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertOK(jedis.ftCreate("testindex-missing", TextField.of("title").indexMissing().indexEmpty()
          .weight(2.5).noStem().phonetic("dm:en").withSuffixTrie().sortable()));

      assertOK(jedis.ftCreate("testunfindex-missing", TextField.of("title").indexMissing()
          .indexEmpty().weight(2.5).noStem().phonetic("dm:en").withSuffixTrie().sortableUNF()));
    }

    assertOK(jedis.ftCreate("testnoindex", TextField.of("title").sortable().noIndex()));

    assertOK(jedis.ftCreate("testunfnoindex", TextField.of("title").sortableUNF().noIndex()));
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "optional params since 7.4.0 are being tested")
  public void searchTextFieldsCondition() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams(), TextField.of("title"),
      TextField.of("body").indexMissing().indexEmpty()));

    Map<String, String> regular = new HashMap<>();
    regular.put("title", "hello world");
    regular.put("body", "lorem ipsum");
    jedis.hset("regular-doc", regular);

    Map<String, String> empty = new HashMap<>();
    empty.put("title", "hello world");
    empty.put("body", "");
    jedis.hset("empty-doc", empty);

    Map<String, String> missing = new HashMap<>();
    missing.put("title", "hello world");
    jedis.hset("missing-doc", missing);

    SearchResult result = jedis.ftSearch(INDEX, "", FTSearchParams.searchParams().dialect(2));
    assertEquals(0, result.getTotalResults());
    assertEquals(0, result.getDocuments().size());

    result = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().dialect(2));
    assertEquals(3, result.getTotalResults());
    assertEquals(3, result.getDocuments().size());

    result = jedis.ftSearch(INDEX, "@body:''", FTSearchParams.searchParams().dialect(2));
    assertEquals(1, result.getTotalResults());
    assertEquals(1, result.getDocuments().size());
    assertEquals("empty-doc", result.getDocuments().get(0).getId());

    result = jedis.ftSearch(INDEX, "ismissing(@body)", FTSearchParams.searchParams().dialect(2));
    assertEquals(1, result.getTotalResults());
    assertEquals(1, result.getDocuments().size());
    assertEquals("missing-doc", result.getDocuments().get(0).getId());
  }

  @Test
  public void numericFieldParams() {
    assertOK(jedis.ftCreate("testindex", TextField.of("title"),
      NumericField.of("price").as("px").sortable()));

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertOK(jedis.ftCreate("testindex-missing", TextField.of("title"),
        NumericField.of("price").as("px").indexMissing().sortable()));
    }

    assertOK(jedis.ftCreate("testnoindex", TextField.of("title"),
      NumericField.of("price").as("px").sortable().noIndex()));
  }

  @Test
  public void stopwords() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams().stopwords("foo", "bar", "baz"),
      TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    addDocument("doc1", fields);
    SearchResult res = jedis.ftSearch(INDEX, "hello world");
    assertEquals(1, res.getTotalResults());
    res = jedis.ftSearch(INDEX, "foo bar");
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void geoFilterAndGeoCoordinateObject() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), GeoField.of("loc")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", new GeoCoordinate(-0.441, 51.458));
    addDocument("doc1", fields);

    fields.put("loc", new GeoCoordinate(-0.1, 51.2));
    addDocument("doc2", fields);

    SearchResult res = jedis.ftSearch(INDEX, "hello world", FTSearchParams.searchParams()
        .geoFilter(new FTSearchParams.GeoFilter("loc", -0.44, 51.45, 10, GeoUnit.KM)));
    assertEquals(1, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "hello world", FTSearchParams.searchParams()
        .geoFilter(new FTSearchParams.GeoFilter("loc", -0.44, 51.45, 100, GeoUnit.KM)));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void geoFieldParams() {
    assertOK(jedis.ftCreate("testindex", TextField.of("title"),
      GeoField.of("location").as("loc").sortable()));

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertOK(jedis.ftCreate("testindex-missing", TextField.of("title"),
        GeoField.of("location").as("loc").indexMissing().sortable()));
    }

    assertOK(jedis.ftCreate("testnoindex", TextField.of("title"),
      GeoField.of("location").as("loc").sortable().noIndex()));
  }

  @Test
  public void geoShapeFilterSpherical() throws ParseException {
    final WKTReader reader = new WKTReader();
    final GeometryFactory factory = new GeometryFactory();

    assertOK(jedis.ftCreate(INDEX, GeoShapeField.of("geom", CoordinateSystem.SPHERICAL)));

    // polygon type
    final Polygon small = factory.createPolygon(new Coordinate[] { new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7100), new Coordinate(34.9100, 29.7100),
        new Coordinate(34.9100, 29.7001), new Coordinate(34.9001, 29.7001) });
    jedis.hsetObject("small", "geom", small);

    final Polygon large = factory.createPolygon(new Coordinate[] { new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7200), new Coordinate(34.9200, 29.7200),
        new Coordinate(34.9200, 29.7001), new Coordinate(34.9001, 29.7001) });
    jedis.hsetObject("large", "geom", large);

    // within condition
    final Polygon within = factory
        .createPolygon(new Coordinate[] { new Coordinate(34.9000, 29.7000),
            new Coordinate(34.9000, 29.7150), new Coordinate(34.9150, 29.7150),
            new Coordinate(34.9150, 29.7000), new Coordinate(34.9000, 29.7000) });

    SearchResult result = jedis.ftSearch(INDEX, "@geom:[within $poly]",
      FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(1, result.getTotalResults());
    assertEquals(1, result.getDocuments().size());
    assertEquals(small, reader.read(result.getDocuments().get(0).getString("geom")));

    // contains condition
    final Polygon contains = factory
        .createPolygon(new Coordinate[] { new Coordinate(34.9002, 29.7002),
            new Coordinate(34.9002, 29.7050), new Coordinate(34.9050, 29.7050),
            new Coordinate(34.9050, 29.7002), new Coordinate(34.9002, 29.7002) });

    result = jedis.ftSearch(INDEX, "@geom:[contains $poly]",
      FTSearchParams.searchParams().addParam("poly", contains).dialect(3));
    assertEquals(2, result.getTotalResults());
    assertEquals(2, result.getDocuments().size());

    // point type
    final Point point = factory.createPoint(new Coordinate(34.9010, 29.7010));
    jedis.hset("point", "geom", point.toString());

    result = jedis.ftSearch(INDEX, "@geom:[within $poly]",
      FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(2, result.getTotalResults());
    assertEquals(2, result.getDocuments().size());
  }

  @Test
  public void geoShapeFilterFlat() throws ParseException {
    final WKTReader reader = new WKTReader();
    final GeometryFactory factory = new GeometryFactory();

    assertOK(jedis.ftCreate(INDEX, GeoShapeField.of("geom", CoordinateSystem.FLAT)));

    // polygon type
    final Polygon small = factory
        .createPolygon(new Coordinate[] { new Coordinate(20, 20), new Coordinate(20, 100),
            new Coordinate(100, 100), new Coordinate(100, 20), new Coordinate(20, 20) });
    jedis.hsetObject("small", "geom", small);

    final Polygon large = factory
        .createPolygon(new Coordinate[] { new Coordinate(10, 10), new Coordinate(10, 200),
            new Coordinate(200, 200), new Coordinate(200, 10), new Coordinate(10, 10) });
    jedis.hsetObject("large", "geom", large);

    // within condition
    final Polygon within = factory
        .createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 150),
            new Coordinate(150, 150), new Coordinate(150, 0), new Coordinate(0, 0) });

    SearchResult result = jedis.ftSearch(INDEX, "@geom:[within $poly]",
      FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(1, result.getTotalResults());
    assertEquals(1, result.getDocuments().size());
    assertEquals(small, reader.read(result.getDocuments().get(0).getString("geom")));

    // contains condition
    final Polygon contains = factory
        .createPolygon(new Coordinate[] { new Coordinate(25, 25), new Coordinate(25, 50),
            new Coordinate(50, 50), new Coordinate(50, 25), new Coordinate(25, 25) });

    result = jedis.ftSearch(INDEX, "@geom:[contains $poly]",
      FTSearchParams.searchParams().addParam("poly", contains).dialect(3));
    assertEquals(2, result.getTotalResults());
    assertEquals(2, result.getDocuments().size());

    // intersects and disjoint
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      final Polygon disjointersect = factory
          .createPolygon(new Coordinate[] { new Coordinate(150, 150), new Coordinate(150, 250),
              new Coordinate(250, 250), new Coordinate(250, 150), new Coordinate(150, 150) });

      result = jedis.ftSearch(INDEX, "@geom:[intersects $poly]",
        FTSearchParams.searchParams().addParam("poly", disjointersect).dialect(3));
      assertEquals(1, result.getTotalResults());
      assertEquals(1, result.getDocuments().size());
      assertEquals(large, reader.read(result.getDocuments().get(0).getString("geom")));

      result = jedis.ftSearch(INDEX, "@geom:[disjoint $poly]",
        FTSearchParams.searchParams().addParam("poly", disjointersect).dialect(3));
      assertEquals(1, result.getTotalResults());
      assertEquals(1, result.getDocuments().size());
      assertEquals(small, reader.read(result.getDocuments().get(0).getString("geom")));
    }

    // point type
    final Point point = factory.createPoint(new Coordinate(30, 30));
    jedis.hsetObject("point", "geom", point);

    result = jedis.ftSearch(INDEX, "@geom:[within $poly]",
      FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(2, result.getTotalResults());
    assertEquals(2, result.getDocuments().size());
  }

  @Test
  public void geoShapeFieldParams() {
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertOK(jedis.ftCreate("testindex-missing",
        GeoShapeField.of("geometry", CoordinateSystem.SPHERICAL).as("geom").indexMissing()));
    }

    assertOK(jedis.ftCreate("testnoindex",
      GeoShapeField.of("geometry", CoordinateSystem.SPHERICAL).as("geom").noIndex()));
  }

  @Test
  public void testQueryFlags() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      fields.put("title", i % 2 != 0 ? "hello worlds" : "hello world");
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = jedis.ftSearch(INDEX, "hello", FTSearchParams.searchParams().withScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(((String) d.get("title")).startsWith("hello world"));
    }

    res = jedis.ftSearch(INDEX, "hello", FTSearchParams.searchParams().noContent());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertEquals(0.0, d.getScore(), 0);
      assertNull(d.get("title"));
    }

    // test verbatim vs. stemming
    res = jedis.ftSearch(INDEX, "hello worlds");
    assertEquals(100, res.getTotalResults());
    res = jedis.ftSearch(INDEX, "hello worlds", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = jedis.ftSearch(INDEX, "hello a world", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = jedis.ftSearch(INDEX, "hello a worlds", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = jedis.ftSearch(INDEX, "hello a world",
      FTSearchParams.searchParams().verbatim().noStopwords());
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void testQueryParams() {
    assertOK(jedis.ftCreate(INDEX, NumericField.of("numval")));

    jedis.hset("1", "numval", "1");
    jedis.hset("2", "numval", "2");
    jedis.hset("3", "numval", "3");

    assertEquals(2,
      jedis
          .ftSearch(INDEX, "@numval:[$min $max]",
            FTSearchParams.searchParams().addParam("min", 1).addParam("max", 2).dialect(2))
          .getTotalResults());

    Map<String, Object> paramValues = new HashMap<>();
    paramValues.put("min", 1);
    paramValues.put("max", 2);
    assertEquals(2, jedis.ftSearch(INDEX, "@numval:[$min $max]",
      FTSearchParams.searchParams().params(paramValues).dialect(2)).getTotalResults());

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertEquals(1, jedis.ftSearch(INDEX, "@numval:[$eq]",
        FTSearchParams.searchParams().addParam("eq", 2).dialect(4)).getTotalResults());
    }
  }

  @Test
  public void testSortQueryFlags() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title").sortable()));

    Map<String, Object> fields = new HashMap<>();

    fields.put("title", "b title");
    addDocument("doc1", fields);

    fields.put("title", "a title");
    addDocument("doc2", fields);

    fields.put("title", "c title");
    addDocument("doc3", fields);

    SearchResult res = jedis.ftSearch(INDEX, "title",
      FTSearchParams.searchParams().sortBy("title", SortingOrder.ASC));

    assertEquals(3, res.getTotalResults());
    Document doc1 = res.getDocuments().get(0);
    assertEquals("a title", doc1.get("title"));

    doc1 = res.getDocuments().get(1);
    assertEquals("b title", doc1.get("title"));

    doc1 = res.getDocuments().get(2);
    assertEquals("c title", doc1.get("title"));
  }

  @Test
  public void testJsonWithAlias() {
    assertOK(
      jedis.ftCreate(INDEX, FTCreateParams.createParams().on(IndexDataType.JSON).prefix("king:"),
        TextField.of("$.name").as("name"), NumericField.of("$.num").as("num")));

    Map<String, Object> king1 = new HashMap<>();
    king1.put("name", "henry");
    king1.put("num", 42);
    jedis.jsonSet("king:1", Path.ROOT_PATH, king1);

    Map<String, Object> king2 = new HashMap<>();
    king2.put("name", "james");
    king2.put("num", 3.14);
    jedis.jsonSet("king:2", Path.ROOT_PATH, king2);

    SearchResult res = jedis.ftSearch(INDEX, "@name:henry");
    assertEquals(1, res.getTotalResults());
    assertEquals("king:1", res.getDocuments().get(0).getId());

    res = jedis.ftSearch(INDEX, "@num:[0 10]");
    assertEquals(1, res.getTotalResults());
    assertEquals("king:2", res.getDocuments().get(0).getId());

    res = jedis.ftSearch(INDEX, "@num:[42 42]", FTSearchParams.searchParams());
    assertEquals(1, res.getTotalResults());
    assertEquals("king:1", res.getDocuments().get(0).getId());

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      res = jedis.ftSearch(INDEX, "@num:[42]", FTSearchParams.searchParams().dialect(4));
      assertEquals(1, res.getTotalResults());
      assertEquals("king:1", res.getDocuments().get(0).getId());
    }
  }

  @Test
  public void noStem() {
    assertOK(jedis.ftCreate(INDEX, new TextField("stemmed").weight(1.0),
      new TextField("notStemmed").weight(1.0).noStem()));

    Map<String, Object> doc = new HashMap<>();
    doc.put("stemmed", "located");
    doc.put("notStemmed", "located");
    addDocument("doc", doc);

    // Query
    SearchResult res = jedis.ftSearch(INDEX, "@stemmed:location");
    assertEquals(1, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "@notStemmed:location");
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void phoneticMatch() {
    assertOK(jedis.ftCreate(INDEX, new TextField("noPhonetic").weight(1.0),
      new TextField("withPhonetic").weight(1.0).phonetic("dm:en")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("noPhonetic", "morfix");
    doc.put("withPhonetic", "morfix");
    addDocument("doc", doc);

    // Query
    SearchResult res = jedis.ftSearch(INDEX, "@withPhonetic:morphix=>{$phonetic:true}");
    assertEquals(1, res.getTotalResults());

    try {
      jedis.ftSearch(INDEX, "@noPhonetic:morphix=>{$phonetic:true}");
      fail();
    } catch (JedisDataException e) {/* field does not support phonetics */
    }

    SearchResult res3 = jedis.ftSearch(INDEX, "@withPhonetic:morphix=>{$phonetic:false}");
    assertEquals(0, res3.getTotalResults());
  }

  @Test
  public void info() {
    Collection<SchemaField> sc = new ArrayList<>();
    sc.add(TextField.of("title").weight(5));
    sc.add(TextField.of("plot").sortable());
    sc.add(TagField.of("genre").separator(',').sortable());
    sc.add(NumericField.of("release_year").sortable());
    sc.add(NumericField.of("rating").sortable());
    sc.add(NumericField.of("votes").sortable());

    assertOK(jedis.ftCreate(INDEX, sc));

    Map<String, Object> info = jedis.ftInfo(INDEX);
    assertEquals(INDEX, info.get("index_name"));
    assertEquals(6, ((List) info.get("attributes")).size());
    if (RedisProtocol.canResolveToResp3(protocol)) {
      assertEquals(0L, ((Map) info.get("cursor_stats")).get("global_idle"));
    } else {
      assertEquals("global_idle", ((List) info.get("cursor_stats")).get(0));
      assertEquals(0L, ((List) info.get("cursor_stats")).get(1));
    }
  }

  @Test
  public void noIndexAndSortBy() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("f1").sortable().noIndex(), TextField.of("f2")));

    Map<String, Object> mm = new HashMap<>();

    mm.put("f1", "MarkZZ");
    mm.put("f2", "MarkZZ");
    addDocument("doc1", mm);

    mm.clear();
    mm.put("f1", "MarkAA");
    mm.put("f2", "MarkBB");
    addDocument("doc2", mm);

    SearchResult res = jedis.ftSearch(INDEX, "@f1:Mark*");
    assertEquals(0, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "@f2:Mark*");
    assertEquals(2, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "@f2:Mark*",
      FTSearchParams.searchParams().sortBy("f1", SortingOrder.DESC));
    assertEquals(2, res.getTotalResults());

    assertEquals("doc1", res.getDocuments().get(0).getId());

    res = jedis.ftSearch(INDEX, "@f2:Mark*",
      FTSearchParams.searchParams().sortBy("f1", SortingOrder.ASC));
    assertEquals("doc2", res.getDocuments().get(0).getId());
  }

  @Test
  public void testHighlightSummarize() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("text").weight(1)));

    Map<String, Object> doc = new HashMap<>();
    doc.put("text", "Redis is often referred as a data structures server. What this means is that "
        + "Redis provides access to mutable data structures via a set of commands, which are sent "
        + "using a server-client model with TCP sockets and a simple protocol. So different "
        + "processes can query and modify the same data structures in a shared way");
    // Add a document
    addDocument("foo", doc);

    SearchResult res = jedis.ftSearch(INDEX, "data",
      FTSearchParams.searchParams().highlight().summarize());
    assertEquals("is often referred as a <b>data</b> structures server. What this means is that "
        + "Redis provides... What this means is that Redis provides access to mutable <b>data</b> "
        + "structures via a set of commands, which are sent using a... So different processes can "
        + "query and modify the same <b>data</b> structures in a shared... ",
      res.getDocuments().get(0).get("text"));

    res = jedis.ftSearch(INDEX, "data", FTSearchParams.searchParams()
        .highlight(FTSearchParams.highlightParams().tags("<u>", "</u>")).summarize());
    assertEquals("is often referred as a <u>data</u> structures server. What this means is that "
        + "Redis provides... What this means is that Redis provides access to mutable <u>data</u> "
        + "structures via a set of commands, which are sent using a... So different processes can "
        + "query and modify the same <u>data</u> structures in a shared... ",
      res.getDocuments().get(0).get("text"));
  }

  @Test
  public void getTagField() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), TagField.of("category")));

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

    assertEquals(1, jedis.ftSearch(INDEX, "@category:{red}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{blue}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello @category:{red}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello @category:{blue}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{yellow}").getTotalResults());
    assertEquals(0, jedis.ftSearch(INDEX, "@category:{purple}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{orange\\;purple}").getTotalResults());
    assertEquals(4, jedis.ftSearch(INDEX, "hello").getTotalResults());

    assertEquals(new HashSet<>(Arrays.asList("red", "blue", "green", "yellow", "orange;purple")),
      jedis.ftTagVals(INDEX, "category"));
  }

  @Test
  public void testGetTagFieldWithNonDefaultSeparator() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), TagField.of("category").separator(';')));

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

    assertEquals(1, jedis.ftSearch(INDEX, "@category:{red}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{blue}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello @category:{red}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello @category:{blue}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello @category:{yellow}").getTotalResults());
    assertEquals(0, jedis.ftSearch(INDEX, "@category:{purple}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{orange\\,purple}").getTotalResults());
    assertEquals(4, jedis.ftSearch(INDEX, "hello").getTotalResults());

    assertEquals(new HashSet<>(Arrays.asList("red", "blue", "green", "yellow", "orange,purple")),
      jedis.ftTagVals(INDEX, "category"));
  }

  @Test
  public void caseSensitiveTagField() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("title"), TagField.of("category").caseSensitive()));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("category", "RedX");
    addDocument("foo", fields);

    assertEquals(0, jedis.ftSearch(INDEX, "@category:{redx}").getTotalResults());
    assertEquals(0, jedis.ftSearch(INDEX, "@category:{redX}").getTotalResults());
    assertEquals(0, jedis.ftSearch(INDEX, "@category:{Redx}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "@category:{RedX}").getTotalResults());
    assertEquals(1, jedis.ftSearch(INDEX, "hello").getTotalResults());
  }

  @Test
  public void tagFieldParams() {
    assertOK(jedis.ftCreate("testindex", TextField.of("title"), TagField.of("category").as("cat")
        .separator(',').caseSensitive().withSuffixTrie().sortable()));

    assertOK(jedis.ftCreate("testunfindex", TextField.of("title"), TagField.of("category").as("cat")
        .separator(',').caseSensitive().withSuffixTrie().sortableUNF()));

    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4)) {
      assertOK(jedis.ftCreate("testindex-missing", TextField.of("title"),
        TagField.of("category").as("cat").indexMissing().indexEmpty().separator(',').caseSensitive()
            .withSuffixTrie().sortable()));

      assertOK(jedis.ftCreate("testunfindex-missing", TextField.of("title"),
        TagField.of("category").as("cat").indexMissing().indexEmpty().separator(',').caseSensitive()
            .withSuffixTrie().sortableUNF()));
    }

    assertOK(jedis.ftCreate("testnoindex", TextField.of("title"),
      TagField.of("category").as("cat").sortable().noIndex()));

    assertOK(jedis.ftCreate("testunfnoindex", TextField.of("title"),
      TagField.of("category").as("cat").sortableUNF().noIndex()));
  }

  @Test
  public void returnFields() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value1");
    doc.put("field2", "value2");
    addDocument("doc", doc);

    // Query
    SearchResult res = jedis.ftSearch(INDEX, "*",
      FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res.getTotalResults());
    Document ret = res.getDocuments().get(0);
    assertEquals("value1", ret.get("field1"));
    assertNull(ret.get("field2"));

    res = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnField("field1", true));
    assertEquals("value1", res.getDocuments().get(0).get("field1"));

    res = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnField("field1", false));
    assertArrayEquals("value1".getBytes(), (byte[]) res.getDocuments().get(0).get("field1"));
  }

  @Test
  public void returnFieldsNames() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("a"), TextField.of("b"), TextField.of("c")));

    Map<String, Object> map = new HashMap<>();
    map.put("a", "value1");
    map.put("b", "value2");
    map.put("c", "value3");
    addDocument("doc", map);

    // Query
    SearchResult res = jedis.ftSearch(INDEX, "*",
      FTSearchParams.searchParams().returnFields(FieldName.of("a"), FieldName.of("b").as("d")));
    assertEquals(1, res.getTotalResults());
    Document doc = res.getDocuments().get(0);
    assertEquals("value1", doc.get("a"));
    assertNull(doc.get("b"));
    assertEquals("value2", doc.get("d"));
    assertNull(doc.get("c"));

    res = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnField(FieldName.of("a"))
        .returnField(FieldName.of("b").as("d")));
    assertEquals(1, res.getTotalResults());
    assertEquals("value1", res.getDocuments().get(0).get("a"));
    assertEquals("value2", res.getDocuments().get(0).get("d"));

    res = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams()
        .returnField(FieldName.of("a"), true).returnField(FieldName.of("b").as("d"), true));
    assertEquals(1, res.getTotalResults());
    assertEquals("value1", res.getDocuments().get(0).get("a"));
    assertEquals("value2", res.getDocuments().get(0).get("d"));

    res = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams()
        .returnField(FieldName.of("a"), false).returnField(FieldName.of("b").as("d"), false));
    assertEquals(1, res.getTotalResults());
    assertArrayEquals("value1".getBytes(), (byte[]) res.getDocuments().get(0).get("a"));
    assertArrayEquals("value2".getBytes(), (byte[]) res.getDocuments().get(0).get("d"));
  }

  @Test
  public void inKeys() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", "not");
    // Store it
    addDocument("doc1", doc);
    addDocument("doc2", doc);

    // Query
    SearchResult res = jedis.ftSearch(INDEX, "value", FTSearchParams.searchParams().inKeys("doc1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertNull(res.getDocuments().get(0).get("value"));
  }

  @Test
  public void slop() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "ok hi jedis");
    addDocument("doc1", doc);

    SearchResult res = jedis.ftSearch(INDEX, "ok jedis", FTSearchParams.searchParams().slop(0));
    assertEquals(0, res.getTotalResults());

    res = jedis.ftSearch(INDEX, "ok jedis", FTSearchParams.searchParams().slop(1));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("ok hi jedis", res.getDocuments().get(0).get("field1"));
  }

  @Test
  public void timeout() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1"), TextField.of("field2")));

    Map<String, String> map = new HashMap<>();
    map.put("field1", "value");
    map.put("field2", "not");
    jedis.hset("doc1", map);

    SearchResult res = jedis.ftSearch(INDEX, "value", FTSearchParams.searchParams().timeout(1000));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void inOrder() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("field1"), TextField.of("field2")));

    Map<String, String> map = new HashMap<>();
    map.put("field1", "value");
    map.put("field2", "not");
    jedis.hset("doc2", map);
    jedis.hset("doc1", map);

    SearchResult res = jedis.ftSearch(INDEX, "value", FTSearchParams.searchParams().inOrder());
    assertEquals(2, res.getTotalResults());
    assertEquals("doc2", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void testHNSWVectorSimilarity() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    assertOK(jedis.ftCreate(INDEX, VectorField.builder().fieldName("v")
        .algorithm(VectorAlgorithm.HNSW).attributes(attr).build()));

    jedis.hset("a", "v", "aaaaaaaa");
    jedis.hset("b", "v", "aaaabaaa");
    jedis.hset("c", "v", "aaaaabaa");

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC).returnFields("__v_score").dialect(2);
    Document doc1 = jedis.ftSearch(INDEX, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void testFlatVectorSimilarity() {
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.FLAT)
          .addAttribute("TYPE", "FLOAT32").addAttribute("DIM", 2)
          .addAttribute("DISTANCE_METRIC", "L2").build()));

    jedis.hset("a", "v", "aaaaaaaa");
    jedis.hset("b", "v", "aaaabaaa");
    jedis.hset("c", "v", "aaaaabaa");

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC).returnFields("__v_score").dialect(2);

    Document doc1 = jedis.ftSearch(INDEX, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void testFlatVectorSimilarityInt8() {
    assumeTrue(RedisConditions.of(jedis).moduleVersionIsGreaterThanOrEqual(SEARCH_MOD_VER_80M3),
      "INT8");
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.FLAT)
          .addAttribute("TYPE", "INT8").addAttribute("DIM", 2).addAttribute("DISTANCE_METRIC", "L2")
          .build()));

    byte[] a = { 127, 1 };
    byte[] b = { 127, 10 };
    byte[] c = { 127, 100 };

    jedis.hset("a".getBytes(), "v".getBytes(), a);
    jedis.hset("b".getBytes(), "v".getBytes(), b);
    jedis.hset("c".getBytes(), "v".getBytes(), c);

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", a)
        .sortBy("__v_score", SortingOrder.ASC).returnFields("__v_score");

    Document doc1 = jedis.ftSearch(INDEX, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "no optional params before 7.4.0")
  public void vectorFieldParams() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    assertOK(jedis.ftCreate("testindex-missing",
      new VectorField("vector", VectorAlgorithm.HNSW, attr).as("vec").indexMissing()));
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "FLOAT16")
  public void float16StorageType() {
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.HNSW)
          .addAttribute("TYPE", "FLOAT16").addAttribute("DIM", 4)
          .addAttribute("DISTANCE_METRIC", "L2").build()));
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "BFLOAT16")
  public void bfloat16StorageType() {
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.HNSW)
          .addAttribute("TYPE", "BFLOAT16").addAttribute("DIM", 4)
          .addAttribute("DISTANCE_METRIC", "L2").build()));
  }

  @Test
  public void int8StorageType() {
    assumeTrue(RedisConditions.of(jedis).moduleVersionIsGreaterThanOrEqual(SEARCH_MOD_VER_80M3),
      "INT8");
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.HNSW)
          .addAttribute("TYPE", "INT8").addAttribute("DIM", 4).addAttribute("DISTANCE_METRIC", "L2")
          .build()));
  }

  @Test
  public void uint8StorageType() {
    assumeTrue(RedisConditions.of(jedis).moduleVersionIsGreaterThanOrEqual(SEARCH_MOD_VER_80M3),
      "UINT8");
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.HNSW)
          .addAttribute("TYPE", "UINT8").addAttribute("DIM", 4)
          .addAttribute("DISTANCE_METRIC", "L2").build()));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testSvsVamanaVectorSimilarity() {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    assertOK(jedis.ftCreate(INDEX, VectorField.builder().fieldName("v")
        .algorithm(VectorAlgorithm.SVS_VAMANA).attributes(attr).build()));

    // Create proper float vectors
    float[] vectorA = { 1.0f, 2.0f };
    float[] vectorB = { 1.1f, 2.1f };
    float[] vectorC = { 2.0f, 3.0f };

    // Convert to byte arrays using RediSearchUtil
    byte[] bytesA = RediSearchUtil.toByteArray(vectorA);
    byte[] bytesB = RediSearchUtil.toByteArray(vectorB);
    byte[] bytesC = RediSearchUtil.toByteArray(vectorC);

    jedis.hset("a".getBytes(), "v".getBytes(), bytesA);
    jedis.hset("b".getBytes(), "v".getBytes(), bytesB);
    jedis.hset("c".getBytes(), "v".getBytes(), bytesC);

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", bytesA)
        .sortBy("__v_score", SortingOrder.ASC).returnFields("__v_score").dialect(2);
    Document doc1 = jedis.ftSearch(INDEX, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testSvsVamanaVectorWithAdvancedParameters() {
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.SVS_VAMANA)
          .addAttribute("TYPE", "FLOAT32").addAttribute("DIM", 4)
          .addAttribute("DISTANCE_METRIC", "L2").addAttribute("CONSTRUCTION_WINDOW_SIZE", 200)
          .addAttribute("GRAPH_MAX_DEGREE", 64).addAttribute("SEARCH_WINDOW_SIZE", 100)
          .addAttribute("EPSILON", 0.01).build()));
  }

  @Test
  public void searchProfile() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("t1"), TextField.of("t2")));

    Map<String, String> hash = new HashMap<>();
    hash.put("t1", "foo");
    hash.put("t2", "bar");
    jedis.hset("doc1", hash);

    Map.Entry<SearchResult, ProfilingInfo> reply = jedis.ftProfileSearch(INDEX,
      FTProfileParams.profileParams(), "foo", FTSearchParams.searchParams());

    SearchResult result = reply.getKey();
    assertEquals(1, result.getTotalResults());
    assertEquals(Collections.singletonList("doc1"),
      result.getDocuments().stream().map(Document::getId).collect(Collectors.toList()));

    // profile
    Object profileObject = reply.getValue().getProfilingInfo();
    if (RedisProtocol.canResolveToResp3(protocol)) {
      assertThat(profileObject, Matchers.isA(Map.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat(((Map<String, Object>) profileObject).keySet(),
          Matchers.hasItems("Shards", "Coordinator"));
      }
    } else {
      assertThat(profileObject, Matchers.isA(List.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat((List<Object>) profileObject, Matchers.hasItems("Shards", "Coordinator"));
      }
    }
  }

  @Test
  public void vectorSearchProfile() {
    assertOK(jedis.ftCreate(INDEX,
      VectorField.builder().fieldName("v").algorithm(VectorAlgorithm.FLAT)
          .addAttribute("TYPE", "FLOAT32").addAttribute("DIM", 2)
          .addAttribute("DISTANCE_METRIC", "L2").build(),
      TextField.of("t")));

    jedis.hset("1", toStringMap("v", "bababaca", "t", "hello"));
    jedis.hset("2", toStringMap("v", "babababa", "t", "hello"));
    jedis.hset("3", toStringMap("v", "aabbaabb", "t", "hello"));
    jedis.hset("4", toStringMap("v", "bbaabbaa", "t", "hello world"));
    jedis.hset("5", toStringMap("v", "aaaabbbb", "t", "hello world"));

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC).noContent().dialect(2);
    Map.Entry<SearchResult, ProfilingInfo> reply = jedis.ftProfileSearch(INDEX,
      FTProfileParams.profileParams(), "*=>[KNN 3 @v $vec]", searchParams);
    assertEquals(3, reply.getKey().getTotalResults());

    assertEquals(Arrays.asList(4, 2, 1).toString(), reply.getKey().getDocuments().stream()
        .map(Document::getId).collect(Collectors.toList()).toString());

    // profile
    Object profileObject = reply.getValue().getProfilingInfo();
    if (RedisProtocol.canResolveToResp3(protocol)) {
      assertThat(profileObject, Matchers.isA(Map.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat(((Map<String, Object>) profileObject).keySet(),
          Matchers.hasItems("Shards", "Coordinator"));
      }
    } else {
      assertThat(profileObject, Matchers.isA(List.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat((List<Object>) profileObject, Matchers.hasItems("Shards", "Coordinator"));
      }
    }
  }

  @Test
  public void limitedSearchProfile() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("t")));
    jedis.hset("1", "t", "hello");
    jedis.hset("2", "t", "hell");
    jedis.hset("3", "t", "help");
    jedis.hset("4", "t", "helowa");

    Map.Entry<SearchResult, ProfilingInfo> reply = jedis.ftProfileSearch(INDEX,
      FTProfileParams.profileParams().limited(), "%hell% hel*",
      FTSearchParams.searchParams().noContent());

    // profile
    Object profileObject = reply.getValue().getProfilingInfo();
    if (RedisProtocol.canResolveToResp3(protocol)) {
      assertThat(profileObject, Matchers.isA(Map.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat(((Map<String, Object>) profileObject).keySet(),
          Matchers.hasItems("Shards", "Coordinator"));
      }
    } else {
      assertThat(profileObject, Matchers.isA(List.class));
      if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V8_0_0)) {
        assertThat((List<Object>) profileObject, Matchers.hasItems("Shards", "Coordinator"));
      }
    }
  }

  @Test
  public void list() {
    assertEquals(Collections.emptySet(), jedis.ftList());

    final int count = 20;
    Set<String> names = new HashSet<>();
    for (int i = 0; i < count; i++) {
      final String name = "idx" + i;
      assertOK(jedis.ftCreate(name, TextField.of("t" + i)));
      names.add(name);
    }
    assertEquals(names, jedis.ftList());
  }

  @Test
  public void broadcast() {
    String reply = jedis.ftCreate(INDEX, TextField.of("t"));
    assertOK(reply);
  }

  @Test
  public void searchIteration() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams(), TextField.of("first"),
      TextField.of("last"), NumericField.of("age")));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    FtSearchIteration search = jedis.ftSearchIteration(3, INDEX, "*",
      FTSearchParams.searchParams());
    int total = 0;
    while (!search.isIterationCompleted()) {
      SearchResult result = search.nextBatch();
      int count = result.getDocuments().size();
      assertThat(count, Matchers.lessThanOrEqualTo(3));
      total += count;
    }
    assertEquals(7, total);
  }

  @Test
  public void searchIterationCollect() {
    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams(), TextField.of("first"),
      TextField.of("last"), NumericField.of("age")));

    jedis.hset("profesor:5555", toStringMap("first", "Albert", "last", "Blue", "age", "55"));
    jedis.hset("student:1111", toStringMap("first", "Joe", "last", "Dod", "age", "18"));
    jedis.hset("pupil:2222", toStringMap("first", "Jen", "last", "Rod", "age", "14"));
    jedis.hset("student:3333", toStringMap("first", "El", "last", "Mark", "age", "17"));
    jedis.hset("pupil:4444", toStringMap("first", "Pat", "last", "Shu", "age", "21"));
    jedis.hset("student:5555", toStringMap("first", "Joen", "last", "Ko", "age", "20"));
    jedis.hset("teacher:6666", toStringMap("first", "Pat", "last", "Rod", "age", "20"));

    ArrayList<Document> collect = new ArrayList<>();
    jedis.ftSearchIteration(3, INDEX, "*", FTSearchParams.searchParams()).collect(collect);
    assertEquals(7, collect.size());
    assertEquals(
      Arrays.asList("profesor:5555", "student:1111", "pupil:2222", "student:3333", "pupil:4444",
        "student:5555", "teacher:6666").stream().collect(Collectors.toSet()),
      collect.stream().map(Document::getId).collect(Collectors.toSet()));
  }

  @Test
  public void escapeUtil() {
    assertOK(jedis.ftCreate(INDEX, TextField.of("txt")));

    jedis.hset("doc1", "txt", RediSearchUtil.escape("hello-world"));
    assertNotEquals("hello-world", jedis.hget("doc1", "txt"));
    assertEquals("hello-world", RediSearchUtil.unescape(jedis.hget("doc1", "txt")));

    SearchResult resultNoEscape = jedis.ftSearch(INDEX, "hello-world");
    assertEquals(0, resultNoEscape.getTotalResults());

    SearchResult resultEscaped = jedis.ftSearch(INDEX, RediSearchUtil.escapeQuery("hello-world"));
    assertEquals(1, resultEscaped.getTotalResults());
  }

  @Test
  public void escapeMapUtil() {
    jedis.hset("doc2",
      RediSearchUtil.toStringMap(Collections.singletonMap("txt", "hello-world"), true));
    assertNotEquals("hello-world", jedis.hget("doc2", "txt"));
    assertEquals("hello-world", RediSearchUtil.unescape(jedis.hget("doc2", "txt")));
  }

  @Test
  public void hsetObject() {
    float[] floats = new float[] { 0.2f };
    assertEquals(1L, jedis.hsetObject("obj", "floats", floats));
    assertArrayEquals(RediSearchUtil.toByteArray(floats),
      jedis.hget("obj".getBytes(), "floats".getBytes()));

    GeoCoordinate geo = new GeoCoordinate(-0.441, 51.458);
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", geo);
    assertEquals(2L, jedis.hsetObject("obj", fields));
    Map<String, String> stringMap = jedis.hgetAll("obj");
    assertEquals(3, stringMap.size());
    assertEquals("hello world", stringMap.get("title"));
    assertEquals(geo.getLongitude() + "," + geo.getLatitude(), stringMap.get("loc"));
  }
}