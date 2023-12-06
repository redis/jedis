package redis.clients.jedis.modules.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.*;
import redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm;
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

    SearchResult noFilters = client.ftSearch(index);
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = client.ftSearch(index, "@first:Jo*");
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = client.ftSearch(index, "@first:Pat");
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = client.ftSearch(index, "@last:Rod");
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

    SearchResult noFilters = client.ftSearch(index);
    assertEquals(4, noFilters.getTotalResults());

    SearchResult res1 = client.ftSearch(index, "@first:Jo*");
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = client.ftSearch(index, "@first:Pat");
    assertEquals(1, res2.getTotalResults());

    SearchResult res3 = client.ftSearch(index, "@last:Rod");
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

    SearchResult noFilters = client.ftSearch(index);
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asFirst = client.ftSearch(index, "@first:Jo*");
    assertEquals(0, asFirst.getTotalResults());

    SearchResult asGiven = client.ftSearch(index, "@given:Jo*");
    assertEquals(2, asGiven.getTotalResults());

    SearchResult nonLast = client.ftSearch(index, "@last:Rod");
    assertEquals(0, nonLast.getTotalResults());

    SearchResult asFamily = client.ftSearch(index, "@family:Rod");
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
    SearchResult res = client.ftSearch(index, "hello world");
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
    SearchResult res2 = client.ftSearch(index, "@tags:{tagA}");
    assertEquals(100, res2.getTotalResults());
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

    SearchResult res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().limit(0, 5).withScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(5, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }

    client.del("doc0");

    res = client.ftSearch(index, "hello world");
    assertEquals(99, res.getTotalResults());

    assertEquals("OK", client.ftDropIndex(index));
    try {
      client.ftSearch(index, "hello world");
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

    SearchResult res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().filter("price", 0, 49));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 0);
      assertTrue(price <= 49);
    }

    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().filter("price", 0, true, 49, true));
    assertEquals(48, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price > 0);
      assertTrue(price < 49);
    }
    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().filter("price", 50, 100));
    assertEquals(50, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());
    for (Document d : res.getDocuments()) {
      long price = Long.valueOf((String) d.get("price"));
      assertTrue(price >= 50);
      assertTrue(price <= 100);
    }

    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams()
            .filter("price", 20, Double.POSITIVE_INFINITY));
    assertEquals(80, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams()
            .filter("price", Double.NEGATIVE_INFINITY, 10));
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
    SearchResult res = client.ftSearch(index, "hello world");
    assertEquals(1, res.getTotalResults());
    res = client.ftSearch(index, "foo bar");
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

    assertEquals(1, client.ftSearch(index, "hello world").getTotalResults());
    assertEquals(1, client.ftSearch(index, "foo bar").getTotalResults());
    assertEquals(1, client.ftSearch(index, "to be or not to be").getTotalResults());
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

    SearchResult res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().
            geoFilter("loc", -0.44, 51.45, 10, GeoUnit.KM));
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams().
            geoFilter("loc", -0.44, 51.45, 100, GeoUnit.KM));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void geoFilterAndGeoCoordinateObject() {
    assertOK(client.ftCreate(index, TextField.of("title"), GeoField.of("loc")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", new GeoCoordinate(-0.441, 51.458));
    addDocument("doc1", fields);

    fields.put("loc", new GeoCoordinate(-0.1, 51.2));
    addDocument("doc2", fields);

    SearchResult res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams()
            .geoFilter(new FTSearchParams.GeoFilter("loc", -0.44, 51.45, 10, GeoUnit.KM)));
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, "hello world",
        FTSearchParams.searchParams()
            .geoFilter(new FTSearchParams.GeoFilter("loc", -0.44, 51.45, 100, GeoUnit.KM)));
    assertEquals(2, res.getTotalResults());
  }

  @Test
  public void geoShapeFilterSpherical() throws ParseException {
    final WKTReader reader = new WKTReader();
    final GeometryFactory factory = new GeometryFactory();

    assertOK(client.ftCreate(index, GeoShapeField.of("geom", GeoShapeField.CoordinateSystem.SPHERICAL)));

    // polygon type
    final Polygon small = factory.createPolygon(new Coordinate[]{new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7100), new Coordinate(34.9100, 29.7100),
        new Coordinate(34.9100, 29.7001), new Coordinate(34.9001, 29.7001)});
    client.hset("small", "geom", small.toString());

    final Polygon large = factory.createPolygon(new Coordinate[]{new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7200), new Coordinate(34.9200, 29.7200),
        new Coordinate(34.9200, 29.7001), new Coordinate(34.9001, 29.7001)});
    client.hset("large", "geom", large.toString());

    // within condition
    final Polygon within = factory.createPolygon(new Coordinate[]{new Coordinate(34.9000, 29.7000),
        new Coordinate(34.9000, 29.7150), new Coordinate(34.9150, 29.7150),
        new Coordinate(34.9150, 29.7000), new Coordinate(34.9000, 29.7000)});

    SearchResult res = client.ftSearch(index, "@geom:[within $poly]",
        FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(1, res.getTotalResults());
    assertEquals(1, res.getDocuments().size());
    assertEquals(small, reader.read(res.getDocuments().get(0).getString("geom")));

    // contains condition
    final Polygon contains = factory.createPolygon(new Coordinate[]{new Coordinate(34.9002, 29.7002),
        new Coordinate(34.9002, 29.7050), new Coordinate(34.9050, 29.7050),
        new Coordinate(34.9050, 29.7002), new Coordinate(34.9002, 29.7002)});

    res = client.ftSearch(index, "@geom:[contains $poly]",
        FTSearchParams.searchParams().addParam("poly", contains).dialect(3));
    assertEquals(2, res.getTotalResults());
    assertEquals(2, res.getDocuments().size());

    // point type
    final Point point = factory.createPoint(new Coordinate(34.9010, 29.7010));
    client.hset("point", "geom", point.toString());

    res = client.ftSearch(index, "@geom:[within $poly]",
        FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(2, res.getTotalResults());
    assertEquals(2, res.getDocuments().size());
  }

  @Test
  public void geoShapeFilterFlat() throws ParseException {
    final WKTReader reader = new WKTReader();
    final GeometryFactory factory = new GeometryFactory();

    assertOK(client.ftCreate(index, GeoShapeField.of("geom", GeoShapeField.CoordinateSystem.FLAT)));

    // polygon type
    final Polygon small = factory.createPolygon(new Coordinate[]{new Coordinate(1, 1),
        new Coordinate(1, 100), new Coordinate(100, 100), new Coordinate(100, 1), new Coordinate(1, 1)});
    client.hset("small", "geom", small.toString());

    final Polygon large = factory.createPolygon(new Coordinate[]{new Coordinate(1, 1),
        new Coordinate(1, 200), new Coordinate(200, 200), new Coordinate(200, 1), new Coordinate(1, 1)});
    client.hset("large", "geom", large.toString());

    // within condition
    final Polygon within = factory.createPolygon(new Coordinate[]{new Coordinate(0, 0),
        new Coordinate(0, 150), new Coordinate(150, 150), new Coordinate(150, 0), new Coordinate(0, 0)});

    SearchResult res = client.ftSearch(index, "@geom:[within $poly]",
        FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(1, res.getTotalResults());
    assertEquals(1, res.getDocuments().size());
    assertEquals(small, reader.read(res.getDocuments().get(0).getString("geom")));

    // contains condition
    final Polygon contains = factory.createPolygon(new Coordinate[]{new Coordinate(2, 2),
        new Coordinate(2, 50), new Coordinate(50, 50), new Coordinate(50, 2), new Coordinate(2, 2)});

    res = client.ftSearch(index, "@geom:[contains $poly]",
        FTSearchParams.searchParams().addParam("poly", contains).dialect(3));
    assertEquals(2, res.getTotalResults());
    assertEquals(2, res.getDocuments().size());

    // point type
    final Point point = factory.createPoint(new Coordinate(10, 10));
    client.hset("point", "geom", point.toString());

    res = client.ftSearch(index, "@geom:[within $poly]",
        FTSearchParams.searchParams().addParam("poly", within).dialect(3));
    assertEquals(2, res.getTotalResults());
    assertEquals(2, res.getDocuments().size());
  }

  @Test
  public void testQueryFlags() {
    assertOK(client.ftCreate(index, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      fields.put("title", i % 2 != 0 ? "hello worlds" : "hello world");
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, "hello",
        FTSearchParams.searchParams().withScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(((String) d.get("title")).startsWith("hello world"));
    }
//
//    res = client.ftSearch(index, "hello",
//        FTSearchParams.searchParams().withScores().explainScore());
//    assertEquals(100, res.getTotalResults());
//    assertEquals(10, res.getDocuments().size());
//
//    for (Document d : res.getDocuments()) {
//      assertTrue(d.getId().startsWith("doc"));
//      assertTrue(((String) d.get("title")).startsWith("hello world"));
//    }

    res = client.ftSearch(index, "hello",
        FTSearchParams.searchParams().noContent());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      if (protocol != RedisProtocol.RESP3) {
        assertEquals(1.0, d.getScore(), 0);
        assertNull(d.get("title"));
      } else {
        assertNull(d.getScore());
        assertThrows(NullPointerException.class, () -> d.get("title"));
      }
    }

    // test verbatim vs. stemming
    res = client.ftSearch(index, "hello worlds");
    assertEquals(100, res.getTotalResults());
    res = client.ftSearch(index, "hello worlds", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = client.ftSearch(index, "hello a world", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = client.ftSearch(index, "hello a worlds", FTSearchParams.searchParams().verbatim());
    assertEquals(50, res.getTotalResults());
    res = client.ftSearch(index, "hello a world", FTSearchParams.searchParams().verbatim().noStopwords());
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void testQueryParams() {
    assertOK(client.ftCreate(index, NumericField.of("numval")));

    client.hset("1", "numval", "1");
    client.hset("2", "numval", "2");
    client.hset("3", "numval", "3");

    assertEquals(2, client.ftSearch(index, "@numval:[$min $max]",
        FTSearchParams.searchParams().addParam("min", 1).addParam("max", 2)
            .dialect(2)).getTotalResults());

    Map<String, Object> paramValues = new HashMap<>();
    paramValues.put("min", 1);
    paramValues.put("max", 2);
    assertEquals(2, client.ftSearch(index, "@numval:[$min $max]",
        FTSearchParams.searchParams().params(paramValues)
            .dialect(2)).getTotalResults());
  }

  @Test
  public void testSortQueryFlags() {
    assertOK(client.ftCreate(index, TextField.of("title").sortable()));

    Map<String, Object> fields = new HashMap<>();

    fields.put("title", "b title");
    addDocument("doc1", fields);

    fields.put("title", "a title");
    addDocument("doc2", fields);

    fields.put("title", "c title");
    addDocument("doc3", fields);

    SearchResult res = client.ftSearch(index, "title",
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
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams()
            .on(IndexDataType.JSON)
            .prefix("king:"),
        TextField.of("$.name").as("name"),
        NumericField.of("$.num").as("num")));

    Map<String, Object> king1 = new HashMap<>();
    king1.put("name", "henry");
    king1.put("num", 42);
    client.jsonSet("king:1", Path.ROOT_PATH, king1);

    Map<String, Object> king2 = new HashMap<>();
    king2.put("name", "james");
    king2.put("num", 3.14);
    client.jsonSet("king:2", Path.ROOT_PATH, king2);

    SearchResult res = client.ftSearch(index, "@name:henry");
    assertEquals(1, res.getTotalResults());
    assertEquals("king:1", res.getDocuments().get(0).getId());

    res = client.ftSearch(index, "@num:[0 10]");
    assertEquals(1, res.getTotalResults());
    assertEquals("king:2", res.getDocuments().get(0).getId());
  }

  @Test
  public void dropIndex() {
    assertOK(client.ftCreate(index, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, "hello world");
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", client.ftDropIndex(index));

    try {
      client.ftSearch(index, "hello world");
      fail("Index should not exist.");
    } catch (JedisDataException de) {
      assertTrue(de.getMessage().contains("no such index"));
    }
    assertEquals(100, client.dbSize());
  }

  @Test
  public void dropIndexDD() {
    assertOK(client.ftCreate(index, TextField.of("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, "hello world");
    assertEquals(100, res.getTotalResults());

    assertEquals("OK", client.ftDropIndexDD(index));

    Set<String> keys = client.keys("*");
    assertTrue(keys.isEmpty());
    assertEquals(0, client.dbSize());
  }

  @Test
  public void noStem() {
    assertOK(client.ftCreate(index, new TextField("stemmed").weight(1.0),
        new TextField("notStemmed").weight(1.0).noStem()));

    Map<String, Object> doc = new HashMap<>();
    doc.put("stemmed", "located");
    doc.put("notStemmed", "located");
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, "@stemmed:location");
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, "@notStemmed:location");
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void phoneticMatch() {
    assertOK(client.ftCreate(index, new TextField("noPhonetic").weight(1.0),
        new TextField("withPhonetic").weight(1.0).phonetic("dm:en")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("noPhonetic", "morfix");
    doc.put("withPhonetic", "morfix");
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, "@withPhonetic:morphix=>{$phonetic:true}");
    assertEquals(1, res.getTotalResults());

    try {
      client.ftSearch(index, "@noPhonetic:morphix=>{$phonetic:true}");
      fail();
    } catch (JedisDataException e) {/*field does not support phonetics*/
    }

    SearchResult res3 = client.ftSearch(index, "@withPhonetic:morphix=>{$phonetic:false}");
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

    assertOK(client.ftCreate(index, sc));

    Map<String, Object> info = client.ftInfo(index);
    assertEquals(index, info.get("index_name"));
    assertEquals(6, ((List) info.get("attributes")).size());
    if (protocol != RedisProtocol.RESP3) {
      assertEquals("global_idle", ((List) info.get("cursor_stats")).get(0));
      assertEquals(0L, ((List) info.get("cursor_stats")).get(1));
    } else {
      assertEquals(0L, ((Map) info.get("cursor_stats")).get("global_idle"));
    }
  }

  @Test
  public void noIndexAndSortBy() {
    assertOK(client.ftCreate(index, TextField.of("f1").sortable().noIndex(), TextField.of("f2")));

    Map<String, Object> mm = new HashMap<>();

    mm.put("f1", "MarkZZ");
    mm.put("f2", "MarkZZ");
    addDocument("doc1", mm);

    mm.clear();
    mm.put("f1", "MarkAA");
    mm.put("f2", "MarkBB");
    addDocument("doc2", mm);

    SearchResult res = client.ftSearch(index, "@f1:Mark*");
    assertEquals(0, res.getTotalResults());

    res = client.ftSearch(index, "@f2:Mark*");
    assertEquals(2, res.getTotalResults());

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.DESC));
    assertEquals(2, res.getTotalResults());

    assertEquals("doc1", res.getDocuments().get(0).getId());

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.ASC));
    assertEquals("doc2", res.getDocuments().get(0).getId());
  }

  @Test
  public void testHighlightSummarize() {
    assertOK(client.ftCreate(index, TextField.of("text").weight(1)));

    Map<String, Object> doc = new HashMap<>();
    doc.put("text", "Redis is often referred as a data structures server. What this means is that "
        + "Redis provides access to mutable data structures via a set of commands, which are sent "
        + "using a server-client model with TCP sockets and a simple protocol. So different "
        + "processes can query and modify the same data structures in a shared way");
    // Add a document
    addDocument("foo", doc);

    SearchResult res = client.ftSearch(index, "data", FTSearchParams.searchParams().highlight().summarize());
    assertEquals("is often referred as a <b>data</b> structures server. What this means is that "
        + "Redis provides... What this means is that Redis provides access to mutable <b>data</b> "
        + "structures via a set of commands, which are sent using a... So different processes can "
        + "query and modify the same <b>data</b> structures in a shared... ",
        res.getDocuments().get(0).get("text"));

    res = client.ftSearch(index, "data", FTSearchParams.searchParams()
        .highlight(FTSearchParams.highlightParams().tags("<u>", "</u>"))
        .summarize());
    assertEquals("is often referred as a <u>data</u> structures server. What this means is that "
        + "Redis provides... What this means is that Redis provides access to mutable <u>data</u> "
        + "structures via a set of commands, which are sent using a... So different processes can "
        + "query and modify the same <u>data</u> structures in a shared... ",
        res.getDocuments().get(0).get("text"));
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

    assertEquals(1, client.ftSearch(index, "@category:{red}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{blue}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello @category:{red}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello @category:{blue}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{yellow}").getTotalResults());
    assertEquals(0, client.ftSearch(index, "@category:{purple}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{orange\\;purple}").getTotalResults());
    assertEquals(4, client.ftSearch(index, "hello").getTotalResults());

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

    assertEquals(1, client.ftSearch(index, "@category:{red}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{blue}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello @category:{red}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello @category:{blue}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello @category:{yellow}").getTotalResults());
    assertEquals(0, client.ftSearch(index, "@category:{purple}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{orange\\,purple}").getTotalResults());
    assertEquals(4, client.ftSearch(index, "hello").getTotalResults());

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

    assertEquals(0, client.ftSearch(index, "@category:{redx}").getTotalResults());
    assertEquals(0, client.ftSearch(index, "@category:{redX}").getTotalResults());
    assertEquals(0, client.ftSearch(index, "@category:{Redx}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "@category:{RedX}").getTotalResults());
    assertEquals(1, client.ftSearch(index, "hello").getTotalResults());
  }

  @Test
  public void testReturnFields() {
    assertOK(client.ftCreate(index, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value1");
    doc.put("field2", "value2");
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, "*",
        FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res.getTotalResults());
    Document ret = res.getDocuments().get(0);
    assertEquals("value1", ret.get("field1"));
    assertNull(ret.get("field2"));
  }

  @Test
  public void returnWithFieldNames() {
    assertOK(client.ftCreate(index, TextField.of("a"), TextField.of("b"), TextField.of("c")));

    Map<String, Object> map = new HashMap<>();
    map.put("a", "value1");
    map.put("b", "value2");
    map.put("c", "value3");
    addDocument("doc", map);

    // Query
    SearchResult res = client.ftSearch(index, "*",
        FTSearchParams.searchParams().returnFields(
            FieldName.of("a"), FieldName.of("b").as("d")));
    assertEquals(1, res.getTotalResults());
    Document doc = res.getDocuments().get(0);
    assertEquals("value1", doc.get("a"));
    assertNull(doc.get("b"));
    assertEquals("value2", doc.get("d"));
    assertNull(doc.get("c"));
  }

  @Test
  public void inKeys() {
    assertOK(client.ftCreate(index, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", "not");
    // Store it
    addDocument("doc1", doc);
    addDocument("doc2", doc);

    // Query
    SearchResult res = client.ftSearch(index, "value",
        FTSearchParams.searchParams().inKeys("doc1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals(null, res.getDocuments().get(0).get("value"));
  }

  @Test
  public void alias() {
    assertOK(client.ftCreate(index, TextField.of("field1")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    addDocument("doc1", doc);

    assertEquals("OK", client.ftAliasAdd("ALIAS1", index));
    SearchResult res1 = client.ftSearch("ALIAS1", "*",
        FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res1.getTotalResults());
    assertEquals("value", res1.getDocuments().get(0).get("field1"));

    assertEquals("OK", client.ftAliasUpdate("ALIAS2", index));
    SearchResult res2 = client.ftSearch("ALIAS2", "*",
        FTSearchParams.searchParams().returnFields("field1"));
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
  public void synonym() {
    assertOK(client.ftCreate(index, TextField.of("name").weight(1), TextField.of("addr").weight(1)));

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

  @Test
  public void slop() {
    assertOK(client.ftCreate(index, TextField.of("field1"), TextField.of("field2")));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "ok hi jedis");
    addDocument("doc1", doc);

    SearchResult res = client.ftSearch(index, "ok jedis", FTSearchParams.searchParams().slop(0));
    assertEquals(0, res.getTotalResults());

    res = client.ftSearch(index, "ok jedis", FTSearchParams.searchParams().slop(1));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("ok hi jedis", res.getDocuments().get(0).get("field1"));
  }

  @Test
  public void timeout() {
    assertOK(client.ftCreate(index, TextField.of("field1"), TextField.of("field2")));

    Map<String, String> map = new HashMap<>();
    map.put("field1", "value");
    map.put("field2", "not");
    client.hset("doc1", map);

    SearchResult res = client.ftSearch(index, "value", FTSearchParams.searchParams().timeout(1000));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void inOrder() {
    assertOK(client.ftCreate(index, TextField.of("field1"), TextField.of("field2")));

    Map<String, String> map = new HashMap<>();
    map.put("field1", "value");
    map.put("field2", "not");
    client.hset("doc2", map);
    client.hset("doc1", map);

    SearchResult res = client.ftSearch(index, "value", FTSearchParams.searchParams().inOrder());
    assertEquals(2, res.getTotalResults());
    assertEquals("doc2", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
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

    FTSearchParams searchParams = FTSearchParams.searchParams()
        .addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC)
        .returnFields("__v_score")
        .dialect(2);
    Document doc1 = client.ftSearch(index, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void testFlatVectorSimilarity() {
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

    FTSearchParams searchParams = FTSearchParams.searchParams()
        .addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC)
        .returnFields("__v_score")
        .dialect(2);

    Document doc1 = client.ftSearch(index, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }

  @Test
  public void searchProfile() {
    assertOK(client.ftCreate(index, TextField.of("t1"), TextField.of("t2")));

    Map<String, String> hash = new HashMap<>();
    hash.put("t1", "foo");
    hash.put("t2", "bar");
    client.hset("doc1", hash);

    Map.Entry<SearchResult, Map<String, Object>> reply = client.ftProfileSearch(index,
        FTProfileParams.profileParams(), "foo", FTSearchParams.searchParams());

    SearchResult result = reply.getKey();
    assertEquals(1, result.getTotalResults());
    assertEquals(Collections.singletonList("doc1"), result.getDocuments().stream().map(Document::getId).collect(Collectors.toList()));

    Map<String, Object> profile = reply.getValue();
    Map<String, Object> iteratorsProfile;
    if (protocol != RedisProtocol.RESP3) {
      iteratorsProfile = (Map<String, Object>) profile.get("Iterators profile");
    } else {
      List iteratorsProfileList = (List) profile.get("Iterators profile");
      assertEquals(1, iteratorsProfileList.size());
      iteratorsProfile = (Map<String, Object>) iteratorsProfileList.get(0);
    }
    assertEquals("TEXT", iteratorsProfile.get("Type"));
    assertEquals("foo", iteratorsProfile.get("Term"));
    assertEquals(1L, iteratorsProfile.get("Counter"));
    assertEquals(1L, iteratorsProfile.get("Size"));
    assertSame(Double.class, iteratorsProfile.get("Time").getClass());

    assertEquals(Arrays.asList("Index", "Scorer", "Sorter", "Loader"),
        ((List<Map<String, Object>>) profile.get("Result processors profile")).stream()
            .map(map -> map.get("Type")).collect(Collectors.toList()));
  }

  @Test
  public void vectorSearchProfile() {
    assertOK(client.ftCreate(index, VectorField.builder().fieldName("v")
        .algorithm(VectorAlgorithm.FLAT).addAttribute("TYPE", "FLOAT32")
        .addAttribute("DIM", 2).addAttribute("DISTANCE_METRIC", "L2").build(),
        TextField.of("t")));

    client.hset("1", toMap("v", "bababaca", "t", "hello"));
    client.hset("2", toMap("v", "babababa", "t", "hello"));
    client.hset("3", toMap("v", "aabbaabb", "t", "hello"));
    client.hset("4", toMap("v", "bbaabbaa", "t", "hello world"));
    client.hset("5", toMap("v", "aaaabbbb", "t", "hello world"));

    FTSearchParams searchParams = FTSearchParams.searchParams().addParam("vec", "aaaaaaaa")
        .sortBy("__v_score", SortingOrder.ASC).noContent().dialect(2);
    Map.Entry<SearchResult, Map<String, Object>> reply = client.ftProfileSearch(index,
        FTProfileParams.profileParams(), "*=>[KNN 3 @v $vec]", searchParams);
    assertEquals(3, reply.getKey().getTotalResults());

    assertEquals(Arrays.asList(4, 2, 1).toString(), reply.getKey().getDocuments()
        .stream().map(Document::getId).collect(Collectors.toList()).toString());

    Map<String, Object> profile = reply.getValue();

    if (protocol != RedisProtocol.RESP3) {
      assertEquals("VECTOR", ((Map<String, Object>) profile.get("Iterators profile")).get("Type"));
    } else {
      assertEquals(Arrays.asList("VECTOR"),
          ((List<Map<String, Object>>) profile.get("Iterators profile")).stream()
              .map(map -> map.get("Type")).collect(Collectors.toList()));
    }

    List<Map<String, Object>> resultProcessorsProfile
        = (List<Map<String, Object>>) reply.getValue().get("Result processors profile");
    assertEquals(3, resultProcessorsProfile.size());
    assertEquals("Index", resultProcessorsProfile.get(0).get("Type"));
    assertEquals("Sorter", resultProcessorsProfile.get(2).get("Type"));
  }

  @Test
  public void maxPrefixExpansionSearchProfile() {
    final String configParam = "MAXPREFIXEXPANSIONS";
    String configValue = (String) client.ftConfigGet(configParam).get(configParam);
    try {
      client.ftConfigSet(configParam, "2");

      assertOK(client.ftCreate(index, TextField.of("t")));
      client.hset("1", "t", "foo1");
      client.hset("2", "t", "foo2");
      client.hset("3", "t", "foo3");

      Map.Entry<SearchResult, Map<String, Object>> reply = client.ftProfileSearch(index,
          FTProfileParams.profileParams(), "foo*", FTSearchParams.searchParams().limit(0, 0));
      // Warning=Max prefix expansion reached
      if (protocol != RedisProtocol.RESP3) {
        assertEquals("Max prefix expansion reached",
            ((Map) reply.getValue().get("Iterators profile")).get("Warning"));
      } else {
        assertEquals("Max prefix expansion reached",
            ((Map) ((List) reply.getValue().get("Iterators profile")).get(0)).get("Warning"));
      }
    } finally {
      client.ftConfigSet(configParam, configValue);
    }
  }

  @Test
  public void noContentSearchProfile() {
    assertOK(client.ftCreate(index, TextField.of("t")));
    client.hset("1", "t", "foo");
    client.hset("2", "t", "bar");

    Map.Entry<SearchResult, Map<String, Object>> profile = client.ftProfileSearch(index,
        FTProfileParams.profileParams(), "foo -@t:baz", FTSearchParams.searchParams().noContent());

    Map<String, Object> depth0 = protocol != RedisProtocol.RESP3
        ? (Map<String, Object>) profile.getValue().get("Iterators profile")
        : ((List<Map<String, Object>>) profile.getValue().get("Iterators profile")).get(0);

    assertEquals("INTERSECT", depth0.get("Type"));
    List<Map<String, Object>> depth0_children = (List<Map<String, Object>>) depth0.get("Child iterators");
    assertEquals("TEXT", depth0_children.get(0).get("Type"));
    Map<String, Object> depth1 = depth0_children.get(1);
    assertEquals("NOT", depth1.get("Type"));
    if (protocol != RedisProtocol.RESP3) {
      List<Map<String, Object>> depth1_children = (List<Map<String, Object>>) depth1.get("Child iterators");
      assertEquals(1, depth1_children.size());
      assertEquals("EMPTY", depth1_children.get(0).get("Type"));
    } else {
      assertEquals("EMPTY", ((Map<String, Object>) depth1.get("Child iterator")).get("Type"));
    }
  }

  @Test
  public void deepReplySearchProfile() {
    assertOK(client.ftCreate(index, TextField.of("t")));
    client.hset("1", "t", "hello");
    client.hset("2", "t", "world");

    Map.Entry<SearchResult, Map<String, Object>> profile
        = client.ftProfileSearch(index, FTProfileParams.profileParams(),
            "hello(hello(hello(hello(hello(hello)))))", FTSearchParams.searchParams().noContent());

    Map<String, Object> depth0 = protocol != RedisProtocol.RESP3
        ? (Map<String, Object>) profile.getValue().get("Iterators profile")
        : ((List<Map<String, Object>>) profile.getValue().get("Iterators profile")).get(0);

    AtomicInteger intersectLevelCount = new AtomicInteger();
    AtomicInteger textLevelCount = new AtomicInteger();
    deepReplySearchProfile_assertProfile(depth0, intersectLevelCount, textLevelCount);
    assertThat(intersectLevelCount.get(), Matchers.greaterThan(0));
    assertThat(textLevelCount.get(), Matchers.greaterThan(0));
  }

  private void deepReplySearchProfile_assertProfile(Map<String, Object> attr,
      AtomicInteger intersectLevelCount, AtomicInteger textLevelCount) {

    String type = (String) attr.get("Type");
    assertThat(type, Matchers.not(Matchers.blankOrNullString()));

    switch (type) {
      case "INTERSECT":
        assertThat(attr, Matchers.hasKey("Child iterators"));
        intersectLevelCount.incrementAndGet();
        deepReplySearchProfile_assertProfile((Map) ((List) attr.get("Child iterators")).get(0),
            intersectLevelCount, textLevelCount);
        break;
      case "TEXT":
        assertThat(attr, Matchers.hasKey("Term"));
        textLevelCount.incrementAndGet();
        break;
    }
  }

  @Test
  public void limitedSearchProfile() {
    assertOK(client.ftCreate(index, TextField.of("t")));
    client.hset("1", "t", "hello");
    client.hset("2", "t", "hell");
    client.hset("3", "t", "help");
    client.hset("4", "t", "helowa");

    Map.Entry<SearchResult, Map<String, Object>> profile = client.ftProfileSearch(index,
        FTProfileParams.profileParams().limited(), "%hell% hel*", FTSearchParams.searchParams().noContent());

    Map<String, Object> depth0 = protocol != RedisProtocol.RESP3
        ? (Map<String, Object>) profile.getValue().get("Iterators profile")
        : ((List<Map<String, Object>>) profile.getValue().get("Iterators profile")).get(0);

    assertEquals("INTERSECT", depth0.get("Type"));
    assertEquals(3L, depth0.get("Counter"));

    List<Map<String, Object>> depth0_children = (List<Map<String, Object>>) depth0.get("Child iterators");
    assertFalse(depth0_children.isEmpty());
    for (Map<String, Object> depth1 : depth0_children) {
      assertEquals("UNION", depth1.get("Type"));
      assertNotNull(depth1.get("Query type"));
      if (protocol != RedisProtocol.RESP3) {
        List depth1_children = (List) depth1.get("Child iterators");
        assertEquals(1, depth1_children.size());
        assertSame(String.class, depth1_children.get(0).getClass());
      } else {
        assertSame(String.class, depth1.get("Child iterators").getClass());
      }
    }
  }

  @Test
  public void list() {
    assertEquals(Collections.emptySet(), client.ftList());

    final int count = 20;
    Set<String> names = new HashSet<>();
    for (int i = 0; i < count; i++) {
      final String name = "idx" + i;
      assertOK(client.ftCreate(name, TextField.of("t" + i)));
      names.add(name);
    }
    assertEquals(names, client.ftList());
  }

  @Test
  public void broadcast() {
    String reply = client.ftCreate(index, TextField.of("t"));
    assertOK(reply);
  }

  @Test
  public void searchIteration() {
    assertOK(client.ftCreate(index, FTCreateParams.createParams(),
        TextField.of("first"), TextField.of("last"), NumericField.of("age")));

    client.hset("profesor:5555", toMap("first", "Albert", "last", "Blue", "age", "55"));
    client.hset("student:1111", toMap("first", "Joe", "last", "Dod", "age", "18"));
    client.hset("pupil:2222", toMap("first", "Jen", "last", "Rod", "age", "14"));
    client.hset("student:3333", toMap("first", "El", "last", "Mark", "age", "17"));
    client.hset("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", "21"));
    client.hset("student:5555", toMap("first", "Joen", "last", "Ko", "age", "20"));
    client.hset("teacher:6666", toMap("first", "Pat", "last", "Rod", "age", "20"));

    FtSearchIteration search = client.ftSearchIteration(3, index, "*", FTSearchParams.searchParams());
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
    assertOK(client.ftCreate(index, FTCreateParams.createParams(),
        TextField.of("first"), TextField.of("last"), NumericField.of("age")));

    client.hset("profesor:5555", toMap("first", "Albert", "last", "Blue", "age", "55"));
    client.hset("student:1111", toMap("first", "Joe", "last", "Dod", "age", "18"));
    client.hset("pupil:2222", toMap("first", "Jen", "last", "Rod", "age", "14"));
    client.hset("student:3333", toMap("first", "El", "last", "Mark", "age", "17"));
    client.hset("pupil:4444", toMap("first", "Pat", "last", "Shu", "age", "21"));
    client.hset("student:5555", toMap("first", "Joen", "last", "Ko", "age", "20"));
    client.hset("teacher:6666", toMap("first", "Pat", "last", "Rod", "age", "20"));

    ArrayList<Document> collect = new ArrayList<>();
    client.ftSearchIteration(3, index, "*", FTSearchParams.searchParams()).collect(collect);
    assertEquals(7, collect.size());
    assertEquals(Arrays.asList("profesor:5555", "student:1111", "pupil:2222", "student:3333",
        "pupil:4444", "student:5555", "teacher:6666").stream().collect(Collectors.toSet()),
        collect.stream().map(Document::getId).collect(Collectors.toSet()));
  }

  @Test
  public void escapeUtil() {
    assertOK(client.ftCreate(index, TextField.of("txt")));

    client.hset("doc1", "txt", RediSearchUtil.escape("hello-world"));
    assertNotEquals("hello-world", client.hget("doc1", "txt"));
    assertEquals("hello-world", RediSearchUtil.unescape(client.hget("doc1", "txt")));

    SearchResult resultNoEscape = client.ftSearch(index, "hello-world");
    assertEquals(0, resultNoEscape.getTotalResults());

    SearchResult resultEscaped = client.ftSearch(index, RediSearchUtil.escapeQuery("hello-world"));
    assertEquals(1, resultEscaped.getTotalResults());
  }

  @Test
  public void escapeMapUtil() {
    client.hset("doc2", RediSearchUtil.toStringMap(Collections.singletonMap("txt", "hello-world"), true));
    assertNotEquals("hello-world", client.hget("doc2", "txt"));
    assertEquals("hello-world", RediSearchUtil.unescape(client.hget("doc2", "txt")));
  }

  @Test
  public void hsetObject() {
    float[] floats = new float[]{0.2f};
    assertEquals(1L, client.hsetObject("obj", "floats", floats));
    assertArrayEquals(RediSearchUtil.toByteArray(floats),
        client.hget("obj".getBytes(), "floats".getBytes()));

    GeoCoordinate geo = new GeoCoordinate(-0.441, 51.458);
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("loc", geo);
    assertEquals(2L, client.hsetObject("obj", fields));
    Map<String, String> stringMap = client.hgetAll("obj");
    assertEquals(3, stringMap.size());
    assertEquals("hello world", stringMap.get("title"));
    assertEquals(geo.getLongitude() + "," + geo.getLatitude(), stringMap.get("loc"));
  }
}
