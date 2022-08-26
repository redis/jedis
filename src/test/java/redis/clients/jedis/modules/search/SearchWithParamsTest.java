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
        TextField.textField("first"), TextField.textField("last"), NumericField.numericField("age")));

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
        TextField.textField("first").weight(1),
        TextField.textField("last").weight(1),
        NumericField.numericField("age")));

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
        TextField.textField("first").as("given"),
        TextField.textField(FieldName.of("last").as("family"))));

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
    assertOK(client.ftCreate(index, TextField.textField("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    for (int i = 0; i < 100; i++) {
      addDocument(String.format("doc%d", i), fields);
    }
    SearchResult res = client.ftSearch(index, new Query("hello world"));
    assertEquals(100, res.getTotalResults());

    assertOK(client.ftAlter(index,
        TagField.tagField("tags"),
        TextField.textField("name").weight(0.5)));

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
        TextField.textField("title"), TextField.textField("body")));

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
  public void stopwords() {
    assertOK(client.ftCreate(index,
        FTCreateParams.createParams()
            .stopwords("foo", "bar", "baz"),
        TextField.textField("title")));

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
        TextField.textField("title")));

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world foo bar");
    fields.put("title", "hello world foo bar to be or not to be");
    addDocument("doc1", fields);

    assertEquals(1, client.ftSearch(index, new Query("hello world")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("foo bar")).getTotalResults());
    assertEquals(1, client.ftSearch(index, new Query("to be or not to be")).getTotalResults());
  }

  @Test
  public void caseSensitiveTagField() {
    assertOK(client.ftCreate(index,
        TextField.textField("title"),
        TagField.tagField("category").caseSensitive()));

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
}
