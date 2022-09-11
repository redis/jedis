package redis.clients.jedis.modules.search;

import static org.junit.Assert.*;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path;
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
  public void testQueryFlags() {
    Schema sc = new Schema().addTextField("title", 1.0);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> fields = new HashMap<>();

    for (int i = 0; i < 100; i++) {
      fields.put("title", i % 2 != 0 ? "hello worlds" : "hello world");
//      assertTrue(client.addDocument(String.format("doc%d", i), (double) i / 100.0, fields));
      addDocument(String.format("doc%d", i), fields);
    }

    SearchResult res = client.ftSearch(index, "hello", FTSearchParams.searchParams().withScores());
    assertEquals(100, res.getTotalResults());
    assertEquals(10, res.getDocuments().size());

    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
//      assertNotEquals(1.0, d.getScore());
      assertTrue(((String) d.get("title")).startsWith("hello world"));
    }

    res = client.ftSearch(index, "hello", FTSearchParams.searchParams().noContent());
    for (Document d : res.getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertEquals(1.0, d.getScore(), 0);
      assertNull(d.get("title"));
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
    Schema sc = new Schema().addNumericField("numval");
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    client.hset("1", "numval", "1");
    client.hset("2", "numval", "2");
    client.hset("3", "numval", "3");

    assertEquals(2, client.ftSearch(index, "@numval:[$min $max]",
        FTSearchParams.searchParams().addParam("min", 1).addParam("max", 2).dialect(2)).getTotalResults());

    Map<String, Object> paramValues = new HashMap<>();
    paramValues.put("min", 1);
    paramValues.put("max", 2);
    assertEquals(2, client.ftSearch(index, "@numval:[$min $max]",
        FTSearchParams.searchParams().params(paramValues).dialect(2)).getTotalResults());
  }

  @Test
  public void testSortQueryFlags() {
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
  public void testNullField() {
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
    SearchResult res = client.ftSearch(index, "title");
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

    res = client.ftSearch(index, "title");
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
    res = client.ftSearch(index, "title");
    assertEquals(1, res.getTotalResults());
  }

  @Test
  public void testJsonWithAlias() {
    Schema sc = new Schema()
            .addTextField("$.name", 1.0).as("name")
            .addNumericField("$.num").as("num");

    IndexDefinition definition = new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes("king:");

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(definition), sc));

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
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

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
    Schema sc = new Schema().addTextField("title", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

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
  public void testDropMissing() {
    try {
      client.ftDropIndex(index);
      fail();
    } catch (JedisDataException ex) {
    }
  }

  @Test
  public void noStem() {
    Schema sc = new Schema().addTextField("stemmed", 1.0).addField(new Schema.TextField("notStemmed", 1.0, false, true));
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("stemmed", "located");
    doc.put("notStemmed", "located");
    // Store it
//    assertTrue(client.addDocument("doc", doc));
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, "@stemmed:location");
    assertEquals(1, res.getTotalResults());

    res = client.ftSearch(index, "@notStemmed:location");
    assertEquals(0, res.getTotalResults());
  }

  @Test
  public void phoneticMatch() {
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
  public void noIndex() {
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

    SearchResult res = client.ftSearch(index, "@f1:Mark*");
    assertEquals(0, res.getTotalResults());

    res = client.ftSearch(index, "@f2:Mark*");
    assertEquals(2, res.getTotalResults());

    Document[] docs = new Document[2];

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.DESC));
    assertEquals(2, res.getTotalResults());

    res.getDocuments().toArray(docs);
    assertEquals("doc1", docs[0].getId());

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.ASC));
    res.getDocuments().toArray(docs);
    assertEquals("doc2", docs[0].getId());
  }

  @Test
  public void sortBy() {
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

    SearchResult res = client.ftSearch(index, "@f2:Mark*");
    assertEquals(2, res.getTotalResults());

    Document[] docs = new Document[2];

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.DESC));
    assertEquals(2, res.getTotalResults());

    res.getDocuments().toArray(docs);
    assertEquals("doc1", docs[0].getId());

    res = client.ftSearch(index, "@f2:Mark*",
        FTSearchParams.searchParams().sortBy("f1", SortingOrder.ASC));
    res.getDocuments().toArray(docs);
    assertEquals("doc2", docs[0].getId());
  }

  @Test
  public void testExplain() {
    Schema sc = new Schema()
        .addTextField("f1", 1.0)
        .addTextField("f2", 1.0)
        .addTextField("f3", 1.0);
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    String res = client.ftExplain(index, "@f3:f3_val @f2:f2_val @f1:f1_val");
    assertNotNull(res);
    assertFalse(res.isEmpty());
  }

  @Test
  public void testHighlightSummarize() {
    Schema sc = new Schema().addTextField("text", 1.0);
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

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
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value1");
    doc.put("field2", "value2");
    // Store it
//    assertTrue(client.addDocument("doc", doc));
    addDocument("doc", doc);

    // Query
    SearchResult res = client.ftSearch(index, "*",
        FTSearchParams.searchParams().returnFields("field1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("value1", res.getDocuments().get(0).get("field1"));
    assertNull(res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void returnWithFieldNames() {
    Schema sc = new Schema().addTextField("a", 1).addTextField("b", 1).addTextField("c", 1);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> map = new HashMap<>();
    map.put("a", "value1");
    map.put("b", "value2");
    map.put("c", "value3");
//    assertTrue(client.addDocument("doc", map));
    addDocument("doc", map);

    // Query
    SearchResult res = client.ftSearch(index, "*",
        FTSearchParams.searchParams().returnFields(FieldName.of("a"), FieldName.of("b").as("d")));
    assertEquals(1, res.getTotalResults());
    Document doc = res.getDocuments().get(0);
    assertEquals("value1", doc.get("a"));
    assertNull(doc.get("b"));
    assertEquals("value2", doc.get("d"));
    assertNull(doc.get("c"));
  }

  @Test
  public void inKeys() {
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
    SearchResult res = client.ftSearch(index, "value", FTSearchParams.searchParams().inKeys("doc1"));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals(null, res.getDocuments().get(0).get("value"));
  }

  @Test
  public void blobField() {
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
//    SearchResult res = client.ftSearch(SafeEncoder.encode(index), new Query("value"));
//    assertEquals(1, res.getTotalResults());
//    assertEquals("doc1", res.getDocuments().get(0).getId());
//    assertEquals("value", res.getDocuments().get(0).getString("field1"));
//    assertArrayEquals(blob, (byte[]) res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void config() {
    assertEquals("OK", client.ftConfigSet("timeout", "100"));
    Map<String, String> configMap = client.ftConfigGet("*");
    assertEquals("100", configMap.get("TIMEOUT"));
  }

  @Test
  public void configOnTimeout() {
    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "fail"));
    assertEquals(Collections.singletonMap("ON_TIMEOUT", "fail"), client.ftConfigGet("ON_TIMEOUT"));

    try {
      client.ftConfigSet("ON_TIMEOUT", "null");
      fail("null is not valid value for ON_TIMEOUT");
    } catch (JedisDataException e) {
      // Should throw an exception after RediSearch 2.2
    }
  }

  @Test
  public void alias() {
    Schema sc = new Schema().addTextField("field1", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));
    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
//    assertTrue(client.addDocument("doc1", doc));
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
  public void dictionary() {
    assertEquals(3L, client.ftDictAdd("dict", "foo", "bar", "hello world"));
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "hello world")), client.ftDictDump("dict"));
    assertEquals(3L, client.ftDictDel("dict", "foo", "bar", "hello world"));
    assertEquals(Collections.emptySet(), client.ftDictDump("dict"));
  }

  @Test
  public void dictionaryBySampleKey() {
    assertEquals(3L, client.ftDictAddBySampleKey(index, "dict", "foo", "bar", "hello world"));
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "hello world")), client.ftDictDumpBySampleKey(index, "dict"));
    assertEquals(3L, client.ftDictDelBySampleKey(index, "dict", "foo", "bar", "hello world"));
    assertEquals(Collections.emptySet(), client.ftDictDumpBySampleKey(index, "dict"));
  }

  @Test
  public void synonym() {
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

  @Test
  public void slop() {
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

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
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", "not");

    addDocument("doc1", doc);

    SearchResult res = client.ftSearch(index, "value", FTSearchParams.searchParams().timeout(1000));
    assertEquals(1, res.getTotalResults());
    assertEquals("doc1", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void inOrder() {
    Schema sc = new Schema().addTextField("field1", 1.0).addTextField("field2", 1.0);
    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions(), sc));

    Map<String, Object> doc = new HashMap<>();
    doc.put("field1", "value");
    doc.put("field2", "not");

    addDocument("doc2", doc);
    addDocument("doc1", doc);

    SearchResult res = client.ftSearch(index, "value", FTSearchParams.searchParams().inOrder());
    assertEquals(2, res.getTotalResults());
    assertEquals("doc2", res.getDocuments().get(0).getId());
    assertEquals("value", res.getDocuments().get(0).get("field1"));
    assertEquals("not", res.getDocuments().get(0).get("field2"));
  }

  @Test
  public void testDialectConfig() {
    // confirm default
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "1"), client.ftConfigGet("DEFAULT_DIALECT"));

    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "2"));
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "2"), client.ftConfigGet("DEFAULT_DIALECT"));

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
  public void testDialectsWithFTExplain() {
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

    String query = "(*)";
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
//    query = new Query(q).dialect(2);
//    assertTrue("Should contain 'WILDCARD'", client.ftExplain(index, query).contains("WILDCARD"));

    query = "$hello";
    try {
      client.ftExplain(index, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
//    query = new Query(query).dialect(2).addParam("hello", "hello");
//    assertTrue("Should contain 'UNION {\n  hello\n  +hello(expanded)\n}\n'",
//        client.ftExplain(index, query).contains("UNION {\n  hello\n  +hello(expanded)\n}\n"));
//
//    query = "@title:(@num:[0 10])";
//    query = new Query(query).dialect(1);
//    assertTrue("Should contain 'NUMERIC {0.000000 <= @num <= 10.000000}'",
//        client.ftExplain(index, query).contains("NUMERIC {0.000000 <= @num <= 10.000000}"));
//    query = new Query(query).dialect(2);
//    try {
//      client.ftExplain(index, query);
//      fail();
//    } catch (JedisDataException e) {
//      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
//    }
//
//    query = "@t1:@t2:@t3:hello";
//    query = new Query(query).dialect(1);
//    assertTrue("Should contain '@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n'",
//        client.ftExplain(index, query).contains("@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n"));
//    query = new Query(query).dialect(2);
//    try {
//      client.ftExplain(index, query);
//      fail();
//    } catch (JedisDataException e) {
//      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
//    }
//
//    query = "@title:{foo}}}}}";
//    query = new Query(query).dialect(1);
//    assertTrue("Should contain 'TAG:@title {\n  foo\n}\n'",
//        client.ftExplain(index, query).contains("TAG:@title {\n  foo\n}\n"));
//    query = new Query(query).dialect(2);
//    try {
//      client.ftExplain(index, query);
//      fail();
//    } catch (JedisDataException e) {
//      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
//    }
//
//    query = "*=>[KNN 10 @v $BLOB]";
//    query = new Query(query).addParam("BLOB", "aaaa").dialect(1);
//    try {
//      client.ftExplain(index, query);
//      fail();
//    } catch (JedisDataException e) {
//      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
//    }
//    query = new Query(query).addParam("BLOB", "aaaa").dialect(2);
//    assertTrue("Should contain '{K=10 nearest vector'", client.ftExplain(index, query).contains("{K=10 nearest vector"));
//
//    query = "*=>[knn $K @vec_field $BLOB as score]";
//    query = new Query(query).addParam("BLOB", "aaaa").addParam("K", "10").dialect(1);
//    try {
//      client.ftExplain(index, query);
//      fail();
//    } catch (JedisDataException e) {
//      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
//    }
//    query = new Query(query).addParam("BLOB", "aaaa").addParam("K", "10").dialect(2);
//    assertTrue("Should contain '{K=10 nearest vector'", client.ftExplain(index, query).contains("{K=10 nearest vector"));
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
        .setSortBy("__v_score", true)
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
        .setSortBy("__v_score", true)
        .returnFields("__v_score")
        .dialect(2);

    Document doc1 = client.ftSearch(index, "*=>[KNN 2 @v $vec]", searchParams).getDocuments().get(0);
    assertEquals("a", doc1.getId());
    assertEquals("0", doc1.get("__v_score"));
  }
}
