package redis.clients.jedis.commands.commandobjects;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

/**
 * Tests related to <a href="https://redis.io/commands/?group=json">JSON</a> commands.
 */
public class CommandObjectsJsonCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsJsonCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testJsonSetAndJsonGet() {
    String key = "jsonKey";

    JSONObject person = new JSONObject();
    person.put("name", "John Doe");
    person.put("age", 30);

    String setRoot = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(getRoot, jsonEquals(new JSONArray().put(person)));

    JSONObject details = new JSONObject();
    details.put("city", "New York");

    String setDeep = exec(commandObjects.jsonSet(key, new Path2("$.details"), details));
    assertThat(setDeep, equalTo("OK"));

    Object getDeep = exec(commandObjects.jsonGet(key, new Path2("$.details")));
    assertThat(getDeep, jsonEquals(new JSONArray().put(details)));

    Object getFull = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    person.put("details", details);
    assertThat(getFull, jsonEquals(new JSONArray().put(person)));
  }

  @Test
  public void testJsonSetWithEscape() {
    String key = "jsonKey";

    Map<String, Object> book = new HashMap<>();
    book.put("title", "Learning JSON");

    String setRoot = exec(commandObjects.jsonSetWithEscape(key, Path2.ROOT_PATH, book));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray().put(new JSONObject(book));
    assertThat(getRoot, jsonEquals(expected));
  }

  @Test
  @Deprecated
  public void testJsonSetJsonGetOldPath() {
    String key = "jsonKey";

    Map<String, Object> book = new HashMap<>();
    book.put("author", "Jane Doe");
    book.put("title", "Advanced JSON Techniques");

    String setRoot = exec(commandObjects.jsonSet(key, Path.ROOT_PATH, book));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path.ROOT_PATH));
    assertThat(getRoot, instanceOf(Map.class));

    @SuppressWarnings("unchecked")
    Map<String, Object> getRootMap = (Map<String, Object>) getRoot;
    assertThat(getRootMap, hasEntry("author", "Jane Doe"));
    assertThat(getRootMap, hasEntry("title", "Advanced JSON Techniques"));
  }

  @Test
  @Deprecated
  public void testJsonSetWithPlainString() {
    String key = "jsonKey";
    String jsonString = "{\"name\":\"John\"}";

    String setRoot = exec(commandObjects.jsonSetWithPlainString(key, Path.ROOT_PATH, jsonString));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path.ROOT_PATH));
    assertThat(getRoot, instanceOf(Map.class));

    @SuppressWarnings("unchecked")
    Map<String, Object> getRootMap = (Map<String, Object>) getRoot;
    assertThat(getRootMap, hasEntry("name", "John"));
  }

  @Test
  public void testJsonSetWithParams() {
    String key = "jsonKey";

    JSONObject book = new JSONObject();
    book.put("author", "Jane Doe");
    book.put("title", "Advanced JSON Techniques");

    JsonSetParams params = new JsonSetParams().nx();

    String setRoot = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, book, params));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray().put(book);
    assertThat(getRoot, jsonEquals(expected));
  }

  @Test
  public void testJsonSetWithEscapeAndParams() {
    String key = "jsonKey";

    Map<String, Object> book = new HashMap<>();
    book.put("author", "John Smith");
    book.put("title", "JSON Escaping 101");

    JsonSetParams params = new JsonSetParams().nx();

    String setRoot = exec(commandObjects.jsonSetWithEscape(key, Path2.ROOT_PATH, book, params));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray().put(new JSONObject(book));
    assertThat(getRoot, jsonEquals(expected));
  }

  @Test
  @Deprecated
  public void testJsonSetOldPathWithParams() {
    String key = "jsonKey";

    Map<String, Object> user = new HashMap<>();
    user.put("username", "johndoe");
    user.put("accountType", "premium");

    JsonSetParams params = new JsonSetParams().nx();

    String setRoot = exec(commandObjects.jsonSet(key, Path.ROOT_PATH, user, params));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path.ROOT_PATH));
    assertThat(getRoot, instanceOf(Map.class));

    @SuppressWarnings("unchecked")
    Map<String, Object> readResultMap = (Map<String, Object>) getRoot;
    assertThat(readResultMap, hasEntry("username", "johndoe"));
    assertThat(readResultMap, hasEntry("accountType", "premium"));
  }

  @Test
  public void testJsonMerge() {
    String key = "jsonKey";

    JSONObject initialUser = new JSONObject();
    initialUser.put("name", "John Doe");
    initialUser.put("age", 30);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, initialUser));

    JSONObject mergeUser = new JSONObject();
    mergeUser.put("occupation", "Software Developer");
    mergeUser.put("age", 31); // Assuming we're updating the age as well

    String mergeRoot = exec(commandObjects.jsonMerge(key, Path2.ROOT_PATH, mergeUser));
    assertThat(mergeRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(getRoot, notNullValue());

    JSONObject expectedUser = new JSONObject();
    expectedUser.put("name", "John Doe");
    expectedUser.put("age", 31);
    expectedUser.put("occupation", "Software Developer");

    JSONArray expected = new JSONArray().put(expectedUser);
    assertThat(getRoot, jsonEquals(expected));
  }

  @Test
  @Deprecated
  public void testJsonMergeOldPath() {
    String key = "jsonKey";

    Map<String, Object> initialUser = new HashMap<>();
    initialUser.put("name", "Jane Doe");

    exec(commandObjects.jsonSet(key, Path.ROOT_PATH, initialUser));

    Map<String, Object> mergeUser = new HashMap<>();
    mergeUser.put("occupation", "Data Scientist");
    mergeUser.put("name", "Jane Smith"); // update the name as well

    String mergeRoot = exec(commandObjects.jsonMerge(key, Path.ROOT_PATH, mergeUser));
    assertThat(mergeRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key, Path.ROOT_PATH));
    assertThat(getRoot, instanceOf(Map.class));

    @SuppressWarnings("unchecked")
    Map<String, Object> resultMap = (Map<String, Object>) getRoot;
    assertThat(resultMap, hasEntry("name", "Jane Smith"));
    assertThat(resultMap, hasEntry("occupation", "Data Scientist"));
  }

  @Test
  @Deprecated
  public void testJsonGenericObject() {
    String key = "user:1000";

    Person person = new Person();
    person.setName("John Doe");
    person.setAge(30);

    String setRoot = exec(commandObjects.jsonSet(key, Path.ROOT_PATH, person));
    assertThat(setRoot, equalTo("OK"));

    Object getRoot = exec(commandObjects.jsonGet(key));
    assertThat(getRoot, instanceOf(Map.class));

    @SuppressWarnings("unchecked")
    Map<String, Object> resultMap = (Map<String, Object>) getRoot;
    assertThat(resultMap, hasEntry("name", "John Doe"));
    assertThat(resultMap, hasEntry("age", 30.0));
  }

  @Test
  @Deprecated
  public void testJsonGetWithClass() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "user:2000";

    String jsonObject = "{\"name\":\"Jane Doe\",\"age\":25}";

    exec(commandObjects.jsonSetWithPlainString(key, Path.ROOT_PATH, jsonObject));

    Person getRoot = exec(commandObjects.jsonGet(key, Person.class));

    assertThat(getRoot.getName(), equalTo("Jane Doe"));
    assertThat(getRoot.getAge(), equalTo(25));
  }

  @Test
  public void testJsonMGet() {
    String keyBob = "user:bob";
    String keyCharlie = "user:charlie";

    JSONObject bob = new JSONObject();
    bob.put("name", "Bob");
    bob.put("age", 30);

    JSONObject charlie = new JSONObject();
    charlie.put("name", "Charlie");
    charlie.put("age", 25);

    String setBobRoot = exec(commandObjects.jsonSet(keyBob, Path2.ROOT_PATH, bob));
    assertThat(setBobRoot, equalTo("OK"));

    String setCharlieRoot = exec(commandObjects.jsonSet(keyCharlie, Path2.ROOT_PATH, charlie));
    assertThat(setCharlieRoot, equalTo("OK"));

    List<JSONArray> getNames = exec(commandObjects.jsonMGet(Path2.of("name"), keyBob, keyCharlie));
    assertThat(getNames, contains(
        jsonEquals(new JSONArray().put("Bob")),
        jsonEquals(new JSONArray().put("Charlie"))
    ));

    List<JSONArray> getRoots = exec(commandObjects.jsonMGet(Path2.ROOT_PATH, keyBob, keyCharlie));
    assertThat(getRoots, contains(
        jsonEquals(new JSONArray().put(bob)),
        jsonEquals(new JSONArray().put(charlie))
    ));
  }

  @Test
  @Deprecated
  public void testJsonMGetOldPath() {
    String keyBob = "user:bob";
    String keyCharlie = "user:charlie";

    JSONObject bob = new JSONObject();
    bob.put("name", "Bob");
    bob.put("age", 30);

    JSONObject charlie = new JSONObject();
    charlie.put("name", "Charlie");
    charlie.put("age", 25);

    String setBobRoot = exec(commandObjects.jsonSet(keyBob, Path2.ROOT_PATH, bob));
    assertThat(setBobRoot, equalTo("OK"));

    String setCharlieRoot = exec(commandObjects.jsonSet(keyCharlie, Path2.ROOT_PATH, charlie));
    assertThat(setCharlieRoot, equalTo("OK"));

    List<String> getNamesTyped = exec(commandObjects.jsonMGet(Path.of("name"), String.class, keyBob, keyCharlie));
    assertThat(getNamesTyped, contains("Bob", "Charlie"));

    List<Person> getPersonsTyped = exec(commandObjects.jsonMGet(Path.ROOT_PATH, Person.class, keyBob, keyCharlie));
    assertThat(getPersonsTyped, contains(
        new Person("Bob", 30),
        new Person("Charlie", 25)
    ));
  }

  @Test
  @Deprecated
  public void testJsonGetAsPlainString() {
    String key = "user:3000";

    Person person = new Person("John Smith", 30);

    exec(commandObjects.jsonSet(key, Path.ROOT_PATH, person));

    String getName = exec(commandObjects.jsonGetAsPlainString(key, Path.of(".name")));
    assertThat(getName, equalTo("\"John Smith\""));

    String getRoot = exec(commandObjects.jsonGetAsPlainString(key, Path.ROOT_PATH));
    assertThat(getRoot, jsonEquals(person));
  }

  @Test
  @Deprecated
  public void testJsonGetWithPathAndClass() {
    String key = "user:4000";

    String jsonObject = "{\"person\":{\"name\":\"Alice Johnson\",\"age\":28}}";

    String setRoot = exec(commandObjects.jsonSetWithPlainString(key, Path.ROOT_PATH, jsonObject));
    assertThat(setRoot, equalTo("OK"));

    Person getPerson = exec(commandObjects.jsonGet(key, Person.class, Path.of(".person")));
    assertThat(getPerson.getName(), equalTo("Alice Johnson"));
    assertThat(getPerson.getAge(), equalTo(28));
  }

  @Test
  public void testJsonDel() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long del = exec(commandObjects.jsonDel(key));
    assertThat(del, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(postCheck, nullValue());
  }

  @Test
  public void testJsonDelPath() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long delAge = exec(commandObjects.jsonDel(key, Path2.of(".age")));
    assertThat(delAge, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonDelOldPath() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long delAge = exec(commandObjects.jsonDel(key, Path.of(".age")));
    assertThat(delAge, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonClear() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long clear = exec(commandObjects.jsonClear(key));
    assertThat(clear, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray().put(new JSONObject());
    assertThat(postCheck, jsonEquals(expected));
  }

  @Test
  public void testJsonClearPath() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);
    person.put("occupations", new JSONArray().put("Data Scientist").put("Developer"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long clearOccupations = exec(commandObjects.jsonClear(key, Path2.of(".occupations")));
    assertThat(clearOccupations, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina");
    expected.put("age", 29);
    expected.put("occupations", new JSONArray());
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonClearOldPath() {
    String key = "user:11000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);
    person.put("occupations", new JSONArray().put("Data Scientist").put("Developer"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long clearOccupations = exec(commandObjects.jsonClear(key, Path.of(".occupations")));
    assertThat(clearOccupations, equalTo(1L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina");
    expected.put("age", 29);
    expected.put("occupations", new JSONArray());
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonToggle() {
    String key = "user:13000";

    JSONObject item = new JSONObject();
    item.put("active", true);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(item)));

    List<Boolean> toggle = exec(commandObjects.jsonToggle(key, Path2.of(".active")));
    assertThat(toggle, contains(false));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("active", false);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonType() {
    String key = "jsonKey";

    JSONObject item = new JSONObject();
    item.put("active", true);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));

    List<Class<?>> type = exec(commandObjects.jsonType(key, Path2.of(".active")));
    assertThat(type, contains(boolean.class));
  }

  @Test
  @Deprecated
  public void testJsonTypeOldPath() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "jsonKey";

    JSONObject item = new JSONObject();
    item.put("active", true);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));

    Class<?> type = exec(commandObjects.jsonType(key, Path.of(".active")));
    assertThat(type, equalTo(boolean.class));
  }

  @Test
  public void testJsonStrAppend() {
    String key = "user:1000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    List<Long> strAppend = exec(commandObjects.jsonStrAppend(key, Path2.of(".name"), " Smith"));
    assertThat(strAppend, contains(10L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina Smith");
    expected.put("age", 29);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonStrAppendOldPath() {
    String key = "user:1000";

    JSONObject person = new JSONObject();
    person.put("name", "Gina");
    person.put("age", 29);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, person));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(person)));

    Long strAppend = exec(commandObjects.jsonStrAppend(key, Path.of(".name"), " Smith"));
    assertThat(strAppend, equalTo(10L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("name", "Gina Smith");
    expected.put("age", 29);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonStrAppendRootPath() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "user:1000";

    String setRoot = exec(commandObjects.jsonSetWithPlainString(key, Path.ROOT_PATH, "\"John\""));
    assertThat(setRoot, equalTo("OK"));

    Object getBefore = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(getBefore, jsonEquals(new JSONArray().put("John")));

    Long strAppend = exec(commandObjects.jsonStrAppend(key, " Doe"));
    assertThat(strAppend, equalTo(8L));

    Object getAfter = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(getAfter, jsonEquals(new JSONArray().put("John Doe")));
  }

  @Test
  @Deprecated
  public void testJsonStrLenRootPath() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "user:1001";

    String setRoot = exec(commandObjects.jsonSetWithPlainString(key, Path.ROOT_PATH, "\"Hello World\""));
    assertThat(setRoot, equalTo("OK"));

    Long strLen = exec(commandObjects.jsonStrLen(key));
    assertThat(strLen, equalTo(11L)); // "Hello World" length
  }

  @Test
  public void testJsonStrLen() {
    String key = "user:1002";

    JSONObject item = new JSONObject();
    item.put("message", "Hello, Redis!");

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));
    assertThat(setResponse, equalTo("OK"));

    List<Long> strLenResponse = exec(commandObjects.jsonStrLen(key, Path2.of(".message")));
    assertThat(strLenResponse, contains(13L)); // "Hello, Redis!" length
  }

  @Test
  @Deprecated
  public void testJsonStrLenOldPath() {
    String key = "user:1003";

    JSONObject item = new JSONObject();
    item.put("message", "Hello, Redis!");

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));
    assertThat(setResponse, equalTo("OK"));

    Long strLenResponse = exec(commandObjects.jsonStrLen(key, Path.of(".message")));
    assertThat(strLenResponse, equalTo(13L)); // "Hello, Redis!" length
  }

  @Test
  public void testJsonNumIncrBy() {
    String key = "user:12000";

    JSONObject item = new JSONObject();
    item.put("balance", 100);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, item));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(item)));

    Object numIncrBy = exec(commandObjects.jsonNumIncrBy(key, Path2.of("$.balance"), 50.0));
    assertThat(numIncrBy, jsonEquals(new JSONArray().put(150.0)));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject();
    expected.put("balance", 150.0);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrAppendWithEscape() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Elixir")
        .put("Swift");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Long> arrAppend = exec(commandObjects.jsonArrAppendWithEscape(
        key, Path2.ROOT_PATH, "Kotlin", "TypeScript"));
    assertThat(arrAppend, contains(4L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put("Elixir")
        .put("Swift")
        .put("Kotlin")
        .put("TypeScript");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrAppend() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Java")
        .put("Python");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    JSONObject person = new JSONObject();
    person.put("name", "John");

    List<Long> arrAppend = exec(commandObjects.jsonArrAppend(key, Path2.ROOT_PATH,
        "\"C++\"", "\"JavaScript\"", person));
    assertThat(arrAppend, contains(5L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put("Java")
        .put("Python")
        .put("C++")
        .put("JavaScript")
        .put(person);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrAppendOldPath() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put(new JSONArray()
            .put("Java")
            .put("Python"))
        .put(1);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Person person = new Person("John", 45);

    Long arrAppend = exec(
        commandObjects.jsonArrAppend(key, Path.of(".[0]"), "Swift", "Go", person));
    assertThat(arrAppend, equalTo(5L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put(new JSONArray()
            .put("Java")
            .put("Python")
            .put("Swift")
            .put("Go")
            .put(new JSONObject()
                .put("name", "John")
                .put("age", 45)))
        .put(1);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrIndex() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Java")
        .put("Python")
        .put("Java"); // duplicate

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    List<Long> arrIndex = exec(commandObjects.jsonArrIndex(key, Path2.ROOT_PATH, "\"Java\""));
    assertThat(arrIndex, contains(0L));

    List<Long> arrIndexNotFound = exec(commandObjects.jsonArrIndex(key, Path2.ROOT_PATH, "\"C++\""));
    assertThat(arrIndexNotFound, contains(-1L));
  }

  @Test
  public void testJsonArrIndexWithEscape() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Java")
        .put("Python")
        .put("Java"); // duplicate

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    List<Long> arrIndex = exec(commandObjects.jsonArrIndexWithEscape(key, Path2.ROOT_PATH, "Java"));
    assertThat(arrIndex, contains(0L));

    List<Long> arrIndexNotFound = exec(commandObjects.jsonArrIndexWithEscape(key, Path2.ROOT_PATH, "Go"));
    assertThat(arrIndexNotFound, contains(-1L));
  }

  @Test
  @Deprecated
  public void testJsonArrIndexDeprecated() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put(new JSONArray()
            .put("Java")
            .put("Python")
            .put("Java")); // duplicate

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Long arrIndex = exec(commandObjects.jsonArrIndex(key, Path.of(".[0]"), "Java"));
    assertThat(arrIndex, equalTo(0L));

    Long arrIndexNotFound = exec(commandObjects.jsonArrIndex(key, Path.of(".[0]"), "Swift"));
    assertThat(arrIndexNotFound, equalTo(-1L));
  }

  @Test
  public void testJsonArrInsert() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Java")
        .put("Python");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Long> arrInsert = exec(
        commandObjects.jsonArrInsert(key, Path2.ROOT_PATH, 1, "\"C++\""));
    assertThat(arrInsert, contains(3L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put("Java")
        .put("C++")
        .put("Python");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrInsertWithEscape() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("Java")
        .put("Python");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Long> arrInsert = exec(commandObjects.jsonArrInsertWithEscape(key, Path2.ROOT_PATH, 1, "Go"));
    assertThat(arrInsert, contains(3L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put("Java")
        .put("Go")
        .put("Python");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrInsertOldPath() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put(1)
        .put(new JSONArray()
            .put("Scala")
            .put("Kotlin"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Long arrInsert = exec(commandObjects.jsonArrInsert(key, Path.of(".[1]"), 1, "Swift"));
    assertThat(arrInsert, equalTo(3L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put(1)
        .put(new JSONArray()
            .put("Scala")
            .put("Swift")
            .put("Kotlin"));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopRoot() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put("apple")
        .put("banana")
        .put("cherry");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Object arrPop = exec(commandObjects.jsonArrPop(key));
    assertThat(arrPop, equalTo("cherry"));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put("apple")
        .put("banana");
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrPopWithPath2() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Object> arrPop = exec(commandObjects.jsonArrPop(key, Path2.of(".fruits")));
    assertThat(arrPop, contains("cherry"));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana"));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopOldPath() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Object arrPop = exec(commandObjects.jsonArrPop(key, Path.of(".fruits")));
    assertThat(arrPop, equalTo("cherry"));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana"));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopRootWithType() {
    String key = "json";

    JSONArray data = new JSONArray()
        .put(1)
        .put(2)
        .put(3);

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Integer arrPop = exec(commandObjects.jsonArrPop(key, Integer.class));
    assertThat(arrPop, equalTo(3));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONArray expected = new JSONArray()
        .put(1)
        .put(2);
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopWithOldPathAndType() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(20)
            .put(30));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Integer arrPop = exec(commandObjects.jsonArrPop(key, Integer.class, Path.of(".numbers")));
    assertThat(arrPop, equalTo(30));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(20));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopWithOldPathTypeAndIndex() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(20)
            .put(30));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Integer arrPop = exec(commandObjects.jsonArrPop(key, Integer.class, Path.of(".numbers"), 1));
    assertThat(arrPop, equalTo(20));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(30));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrPopWithPathAndIndex() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(20)
            .put(30));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Object> arrPop = exec(commandObjects.jsonArrPop(key, Path2.of(".numbers"), 1));
    assertThat(arrPop, contains(20.0));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(30));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrPopOldPathAndIndex() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(20)
            .put(30));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Object arrPop = exec(commandObjects.jsonArrPop(key, Path.of(".numbers"), 1));
    assertThat(arrPop, equalTo(20.0));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("numbers", new JSONArray()
            .put(10)
            .put(30));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  public void testJsonArrTrimWithPath() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry")
            .put("date")
            .put("fig"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    List<Long> arrTrim = exec(commandObjects.jsonArrTrim(key, Path2.of(".fruits"), 1, 3));
    assertThat(arrTrim, contains(3L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("fruits", new JSONArray()
            .put("banana")
            .put("cherry")
            .put("date"));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrTrimOldPath() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry")
            .put("date")
            .put("fig"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Object preCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));
    assertThat(preCheck, jsonEquals(new JSONArray().put(data)));

    Long arrTrim = exec(commandObjects.jsonArrTrim(key, Path.of(".fruits"), 1, 3));
    assertThat(arrTrim, equalTo(3L));

    Object postCheck = exec(commandObjects.jsonGet(key, Path2.ROOT_PATH));

    JSONObject expected = new JSONObject()
        .put("fruits", new JSONArray()
            .put("banana")
            .put("cherry")
            .put("date"));
    assertThat(postCheck, jsonEquals(new JSONArray().put(expected)));
  }

  @Test
  @Deprecated
  public void testJsonArrLenRoot() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "json";

    JSONArray data = new JSONArray()
        .put("apple")
        .put("banana")
        .put("cherry")
        .put("date")
        .put("fig");

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Long arrLen = exec(commandObjects.jsonArrLen(key));
    assertThat(arrLen, equalTo(5L));
  }

  @Test
  public void testJsonArrLenWithPath() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry")
            .put("date")
            .put("fig"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    List<Long> arrLen = exec(commandObjects.jsonArrLen(key, Path2.of(".fruits")));
    assertThat(arrLen, contains(5L));
  }

  @Test
  @Deprecated
  public void testJsonArrLenOldPath() {
    String key = "json";

    JSONObject data = new JSONObject()
        .put("fruits", new JSONArray()
            .put("apple")
            .put("banana")
            .put("cherry")
            .put("date")
            .put("fig"));

    exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));

    Long arrLen = exec(commandObjects.jsonArrLen(key, Path.of(".fruits")));
    assertThat(arrLen, equalTo(5L));
  }

  @Test
  @Deprecated
  public void testJsonObjLenRoot() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "json";

    JSONObject data = new JSONObject();
    data.put("name", "John");
    data.put("age", 30);

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    Long objLen = exec(commandObjects.jsonObjLen(key));
    assertThat(objLen, equalTo(2L)); // 2 keys: "name" and "age"
  }

  @Test
  @Deprecated
  public void testJsonObjLenOldPath() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    Long objLen = exec(commandObjects.jsonObjLen(key, Path.of(".user")));
    assertThat(objLen, equalTo(2L));
  }

  @Test
  public void testJsonObjLenWithPath2() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    List<Long> objLen = exec(commandObjects.jsonObjLen(key, Path2.of(".user")));
    assertThat(objLen, contains(2L));
  }

  @Test
  @Deprecated
  public void testJsonObjKeysRoot() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));

    String key = "json";

    JSONObject data = new JSONObject();
    data.put("name", "John");
    data.put("age", 30);

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    List<String> keys = exec(commandObjects.jsonObjKeys(key));
    assertThat(keys, containsInAnyOrder("name", "age"));
  }

  @Test
  @Deprecated
  public void testJsonObjKeysOldPath() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    List<String> keys = exec(commandObjects.jsonObjKeys(key, Path.of(".user")));
    assertThat(keys, containsInAnyOrder("name", "age"));
  }

  @Test
  public void testJsonObjKeysWithPath() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    List<List<String>> keys = exec(commandObjects.jsonObjKeys(key, Path2.of(".user")));
    assertThat(keys, contains(containsInAnyOrder("name", "age")));
  }

  @Test
  @Deprecated
  public void testJsonDebugMemoryRoot() {
    assumeThat(protocol, not(equalTo(RedisProtocol.RESP3)));
    String key = "json";

    JSONObject data = new JSONObject()
        .put("name", "John")
        .put("age", 30);

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    Long memoryUsage = exec(commandObjects.jsonDebugMemory(key));
    assertThat(memoryUsage, notNullValue());
    assertThat(memoryUsage, greaterThan(0L));
  }

  @Test
  @Deprecated
  public void testJsonDebugMemoryOldPath() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    Long memoryUsage = exec(commandObjects.jsonDebugMemory(key, Path.of(".user")));
    assertThat(memoryUsage, notNullValue());
    assertThat(memoryUsage, greaterThan(0L));
  }

  @Test
  public void testJsonDebugMemoryWithPath2() {
    String key = "json";

    JSONObject data = new JSONObject().put("user",
        new JSONObject()
            .put("name", "John")
            .put("age", 30));

    String setResponse = exec(commandObjects.jsonSet(key, Path2.ROOT_PATH, data));
    assertThat(setResponse, equalTo("OK"));

    List<Long> memoryUsages = exec(commandObjects.jsonDebugMemory(key, Path2.of(".user")));
    assertThat(memoryUsages, contains(greaterThan(0L)));
  }
}
