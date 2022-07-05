package redis.clients.jedis.modules.search;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;

import redis.clients.jedis.json.JsonProtocol;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.Schema.*;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class JsonSearchTest extends RedisModuleCommandsTestBase {

  public static final String JSON_ROOT = "$";

  private static final String index = "json-index";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  private void setJson(String key, JSONObject json) {
    CommandObject command = new CommandObject<>(
        new CommandArguments(JsonProtocol.JsonCommand.SET).key(key).add(Path2.ROOT_PATH).add(json),
        BuilderFactory.STRING);
    client.executeCommand(command);
  }

  private JSONObject toJson(Object... values) {
    JSONObject json = new JSONObject();
    for (int i = 0; i < values.length; i += 2) {
      json.put((String) values[i], values[i + 1]);
    }
    return json;
  }

  @Test
  public void create() {
    Schema schema = new Schema().addTextField("$.first", 1.0).addTextField("$.last", 1.0)
        .addNumericField("$.age");
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON)
        .setPrefixes(new String[]{"student:", "pupil:"});

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

//    try (Jedis jedis = client.connection()) {
//      setJson(jedis, "profesor:5555", toJson("first", "Albert", "last", "Blue", "age", 55));
//      setJson(jedis, "student:1111", toJson("first", "Joe", "last", "Dod", "age", 18));
//      setJson(jedis, "pupil:2222", toJson("first", "Jen", "last", "Rod", "age", 14));
//      setJson(jedis, "student:3333", toJson("first", "El", "last", "Mark", "age", 17));
//      setJson(jedis, "pupil:4444", toJson("first", "Pat", "last", "Shu", "age", 21));
//      setJson(jedis, "student:5555", toJson("first", "Joen", "last", "Ko", "age", 20));
//      setJson(jedis, "teacher:6666", toJson("first", "Pat", "last", "Rod", "age", 20));
//    }
    setJson("profesor:5555", toJson("first", "Albert", "last", "Blue", "age", 55));
    setJson("student:1111", toJson("first", "Joe", "last", "Dod", "age", 18));
    setJson("pupil:2222", toJson("first", "Jen", "last", "Rod", "age", 14));
    setJson("student:3333", toJson("first", "El", "last", "Mark", "age", 17));
    setJson("pupil:4444", toJson("first", "Pat", "last", "Shu", "age", 21));
    setJson("student:5555", toJson("first", "Joen", "last", "Ko", "age", 20));
    setJson("teacher:6666", toJson("first", "Pat", "last", "Rod", "age", 20));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(5, noFilters.getTotalResults());

    SearchResult res1 = client.ftSearch(index, new Query("@\\$\\.first:Jo*"));
    assertEquals(2, res1.getTotalResults());

    SearchResult res2 = client.ftSearch(index, new Query("@\\$\\.first:Pat"));
    assertEquals(1, res2.getTotalResults());
  }

  @Test
  public void createWithFieldNames() {
    Schema schema = new Schema()
        .addField(new TextField(FieldName.of("$.first").as("first")))
        .addField(new TextField(FieldName.of("$.last")))
        .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON)
        .setPrefixes(new String[]{"student:", "pupil:"});

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

//    try (Jedis jedis = client.connection()) {
//      setJson(jedis, "profesor:5555", toJson("first", "Albert", "last", "Blue", "age", 55));
//      setJson(jedis, "student:1111", toJson("first", "Joe", "last", "Dod", "age", 18));
//      setJson(jedis, "pupil:2222", toJson("first", "Jen", "last", "Rod", "age", 14));
//      setJson(jedis, "student:3333", toJson("first", "El", "last", "Mark", "age", 17));
//      setJson(jedis, "pupil:4444", toJson("first", "Pat", "last", "Shu", "age", 21));
//      setJson(jedis, "student:5555", toJson("first", "Joen", "last", "Ko", "age", 20));
//      setJson(jedis, "teacher:6666", toJson("first", "Pat", "last", "Rod", "age", 20));
//    }
    setJson("profesor:5555", toJson("first", "Albert", "last", "Blue", "age", 55));
    setJson("student:1111", toJson("first", "Joe", "last", "Dod", "age", 18));
    setJson("pupil:2222", toJson("first", "Jen", "last", "Rod", "age", 14));
    setJson("student:3333", toJson("first", "El", "last", "Mark", "age", 17));
    setJson("pupil:4444", toJson("first", "Pat", "last", "Shu", "age", 21));
    setJson("student:5555", toJson("first", "Joen", "last", "Ko", "age", 20));
    setJson("teacher:6666", toJson("first", "Pat", "last", "Rod", "age", 20));

    SearchResult noFilters = client.ftSearch(index, new Query());
    assertEquals(5, noFilters.getTotalResults());

    SearchResult asOriginal = client.ftSearch(index, new Query("@\\$\\.first:Jo*"));
    assertEquals(0, asOriginal.getTotalResults());

    SearchResult asAttribute = client.ftSearch(index, new Query("@first:Jo*"));
    assertEquals(2, asAttribute.getTotalResults());

    SearchResult nonAttribute = client.ftSearch(index, new Query("@\\$\\.last:Rod"));
    assertEquals(1, nonAttribute.getTotalResults());
  }

  @Test
  public void parseJson() {
    Schema schema = new Schema()
        .addField(new TextField(FieldName.of("$.first").as("first")))
        .addField(new TextField(FieldName.of("$.last")))
        .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe", "last", "Dod", "age", 18);
//    try (Jedis jedis = client.connection()) {
//      setJson(jedis, id, json);
//    }
    setJson(id, json);

    // query
    SearchResult sr = client.ftSearch(index, new Query().setWithScores().setWithPayload());
    assertEquals(1, sr.getTotalResults());

    Document doc = sr.getDocuments().get(0);
    assertEquals(1.0, doc.getScore(), 0);
    assertNull(doc.getPayload());
    assertEquals(json.toString(), doc.get(JSON_ROOT));

    // query repeat
    sr = client.ftSearch(index, new Query().setWithScores().setWithPayload());

    doc = sr.getDocuments().get(0);
    JSONObject jsonRead = new JSONObject((String) doc.get(JSON_ROOT));
    assertEquals(json.toString(), jsonRead.toString());

    // query repeat
    sr = client.ftSearch(index, new Query().setWithScores().setWithPayload());

    doc = sr.getDocuments().get(0);
    jsonRead = new JSONObject(doc.getString(JSON_ROOT));
    assertEquals(json.toString(), jsonRead.toString());
  }

  @Test
  public void parseJsonPartial() {
    Schema schema = new Schema()
                .addField(new TextField(FieldName.of("$.first").as("first")))
                .addField(new TextField(FieldName.of("$.last")))
                .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe", "last", "Dod", "age", 18);
//    try (Jedis jedis = client.connection()) {
//      setJson(jedis, id, json);
//    }
    setJson(id, json);

    // query
    SearchResult sr = client.ftSearch(index, new Query().returnFields("$.first", "$.last", "$.age"));
    assertEquals(1, sr.getTotalResults());

    Document doc = sr.getDocuments().get(0);
    assertEquals("Joe", doc.get("$.first"));
    assertEquals("Dod", doc.get("$.last"));
    assertEquals(Integer.toString(18), doc.get("$.age"));

    // query repeat
    sr = client.ftSearch(index, new Query().returnFields("$.first", "$.last", "$.age"));

    doc = sr.getDocuments().get(0);
    assertEquals("Joe", doc.getString("$.first"));
    assertEquals("Dod", doc.getString("$.last"));
    assertEquals(18, Integer.parseInt((String) doc.get("$.age")));
  }

  @Test
  public void parseJsonPartialWithFieldNames() {
    Schema schema = new Schema()
                .addField(new TextField(FieldName.of("$.first").as("first")))
                .addField(new TextField(FieldName.of("$.last")))
                .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe", "last", "Dod", "age", 18);
//    try (Jedis jedis = client.connection()) {
//      setJson(jedis, id, json);
//    }
    setJson(id, json);

    // query
    SearchResult sr = client.ftSearch(index, new Query().returnFields(FieldName.of("$.first").as("first"),
        FieldName.of("$.last").as("last"), FieldName.of("$.age")));
    assertEquals(1, sr.getTotalResults());

    Document doc = sr.getDocuments().get(0);
    assertNull(doc.get("$.first"));
    assertNull(doc.get("$.last"));
    assertEquals(Integer.toString(18), doc.get("$.age"));
    assertEquals("Joe", doc.get("first"));
    assertEquals("Dod", doc.get("last"));
    assertNull(doc.get("age"));
  }

  @Test
  public void dialect() {
    Schema schema = new Schema()
            .addField(new TextField(FieldName.of("$.first").as("first")))
            .addField(new TextField(FieldName.of("$.last")))
            .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe", "last", "Dod", "age", 18);
    setJson(id, json);

    SearchResult sr = client.ftSearch(index, new Query().returnFields(FieldName.of("$.first").as("first"),
            FieldName.of("$.last").as("last"), FieldName.of("$.age")).dialect(1));
    assertEquals(1, sr.getTotalResults());
    assertEquals("Joe", sr.getDocuments().get(0).get("first"));
    assertEquals("Dod", sr.getDocuments().get(0).get("last"));
  }

  @Test
  public void slop() {
    Schema schema = new Schema()
            .addField(new TextField(FieldName.of("$.first").as("first")))
            .addField(new TextField(FieldName.of("$.last")))
            .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe is first ok", "last", "Dod will be first next", "age", 18);
    setJson(id, json);

    SearchResult sr = client.ftSearch(index, new Query("Dod next").returnFields(FieldName.of("$.first").as("first"),
            FieldName.of("$.last").as("last"), FieldName.of("$.age")).slop(0));
    assertEquals(0, sr.getTotalResults());

    sr = client.ftSearch(index, new Query("Dod next").returnFields(FieldName.of("$.first").as("first"),
            FieldName.of("$.last").as("last"), FieldName.of("$.age")).slop(1));
    assertEquals(1, sr.getTotalResults());
  }

  @Test
  public void timeout() {
    Schema schema = new Schema()
            .addField(new TextField(FieldName.of("$.first").as("first")))
            .addField(new TextField(FieldName.of("$.last")))
            .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1111";
    JSONObject json = toJson("first", "Joe is first ok", "last", "Dod will be first next", "age", 18);
    setJson(id, json);

    SearchResult sr = client.ftSearch(index, new Query("Dod next").returnFields(FieldName.of("$.first").as("first"),
            FieldName.of("$.last").as("last"), FieldName.of("$.age")).timeout(2000));
    assertEquals(1, sr.getTotalResults());
  }

  @Test
  public void inOrder() {
    Schema schema = new Schema()
            .addField(new TextField(FieldName.of("$.first").as("first")))
            .addField(new TextField(FieldName.of("$.last")))
            .addField(new Field(FieldName.of("$.age").as("age"), FieldType.NUMERIC));
    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON);

    assertEquals("OK", client.ftCreate(index, IndexOptions.defaultOptions().setDefinition(rule), schema));

    String id = "student:1112";
    JSONObject json = toJson("first", "Joe is first ok", "last", "Dod will be first next", "age", 18);
    setJson(id, json);
    id = "student:1113";
    json = toJson("first", "Joe is first ok", "last", "Dod will be first next", "age", 18);
    setJson(id, json);
    id = "student:1111";
    json = toJson("first", "Joe is first ok", "last", "Dod will be first next", "age", 18);
    setJson(id, json);

    SearchResult sr = client.ftSearch(index, new Query().setInOrder());
    assertEquals(3, sr.getTotalResults());
    assertEquals("student:1112", sr.getDocuments().get(0).getId());
  }
}
