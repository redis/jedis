package redis.clients.jedis.modules.json;

import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.commands.RedisJsonV1Commands;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.util.JsonObjectMapperTestUtil;
import redis.clients.jedis.util.RedisProtocolUtil;

public class RedisJsonV1Test extends RedisModuleCommandsTestBase {

  private final Gson gson = new Gson();

  private RedisJsonV1Commands jsonClient;

  @BeforeClass
  public static void prepare() {
    Assume.assumeFalse(RedisProtocolUtil.getRedisProtocol() == RedisProtocol.RESP3);
    RedisModuleCommandsTestBase.prepare();
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    this.jsonClient = super.client;
  }

  @Test
  public void basicSetGetShouldSucceed() {

    // naive set with a path
//    jsonClient.jsonSet("null", null, ROOT_PATH);
    jsonClient.jsonSet("null", ROOT_PATH, (Object) null);
    assertNull(jsonClient.jsonGet("null", String.class, ROOT_PATH));

    // real scalar value and no path
    jsonClient.jsonSet("str", ROOT_PATH, "strong");
    assertEquals("strong", jsonClient.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    jsonClient.jsonSet("obj", ROOT_PATH, obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(jsonClient.jsonGet("obj")));

    // check an update
    Path p = Path.of(".str");
    jsonClient.jsonSet("obj", p, "strung");
    assertEquals("strung", jsonClient.jsonGet("obj", String.class, p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    jsonClient.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    jsonClient.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", jsonClient.jsonGet("obj", String.class, p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    jsonClient.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    jsonClient.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx());
    assertEquals("strangle", jsonClient.jsonGet("obj", String.class, p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    jsonClient.jsonSet("obj1", ROOT_PATH, new IRLObject());
//    jsonClient.jsonSet("obj1", "strangle", JsonSetParams.jsonSetParams().xx());
    jsonClient.jsonSetLegacy("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", jsonClient.jsonGet("obj1", String.class, ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    jsonClient.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    assertNull(jsonClient.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    jsonClient.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    assertNull(jsonClient.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test(expected = JedisDataException.class)
  public void setException() {
    // should error on non root path for new key
    jsonClient.jsonSet("test", Path.of(".foo"), "bar");
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    jsonClient.jsonSetLegacy("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(jsonClient.jsonGet("obj", Object.class, Path.of("bool"), Path.of("str"))));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    jsonClient.jsonSetLegacy("obj", obj);

    Path pbool = Path.of(".bool");
    // check initial value
    assertTrue(jsonClient.jsonGet("obj", Boolean.class, pbool));

    // true -> false
    jsonClient.jsonToggle("obj", pbool);
    assertFalse(jsonClient.jsonGet("obj", Boolean.class, pbool));

    // false -> true
    jsonClient.jsonToggle("obj", pbool);
    assertTrue(jsonClient.jsonGet("obj", Boolean.class, pbool));

    // ignore non-boolean field
    Path pstr = Path.of(".str");
    try {
      jsonClient.jsonToggle("obj", pstr);
      fail("String not a bool");
    } catch (JedisDataException jde) {
      assertTrue(jde.getMessage().contains("not a bool"));
    }
    assertEquals("string", jsonClient.jsonGet("obj", String.class, pstr));
  }

  @Test(expected = JedisDataException.class)
  public void getAbsent() {
    jsonClient.jsonSet("test", ROOT_PATH, "foo");
    jsonClient.jsonGet("test", String.class, Path.of(".bar"));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    jsonClient.jsonSet("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, jsonClient.jsonDel("obj", Path.of(".str")));
    assertTrue(client.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, jsonClient.jsonDel("obj"));
    assertFalse(client.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, jsonClient.jsonDel("foobar", Path.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    assertNull(jsonClient.jsonType("foobar"));
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertSame(Object.class, jsonClient.jsonType("foobar"));
    assertSame(Object.class, jsonClient.jsonType("foobar", ROOT_PATH));
    assertSame(String.class, jsonClient.jsonType("foobar", Path.of(".foo")));
    assertSame(int.class, jsonClient.jsonType("foobar", Path.of(".fooI")));
    assertSame(float.class, jsonClient.jsonType("foobar", Path.of(".fooF")));
    assertSame(List.class, jsonClient.jsonType("foobar", Path.of(".fooArr")));
    assertSame(boolean.class, jsonClient.jsonType("foobar", Path.of(".fooB")));
    assertNull(jsonClient.jsonType("foobar", Path.of(".fooErr")));
  }

  @Test
  public void testJsonMerge() {
    // create data
    List<String> childrens = new ArrayList<>();
    childrens.add("Child 1");
    Person person = new Person("John Doe", 25, "123 Main Street", "123-456-7890", childrens);
    assertEquals("OK", jsonClient.jsonSet("test_merge", ROOT_PATH, person));

    // After 5 years:
    person.age = 30;
    person.childrens.add("Child 2");
    person.childrens.add("Child 3");

    // merge the new data
    assertEquals("OK", jsonClient.jsonMerge("test_merge", Path.of((".childrens")), person.childrens));
    assertEquals("OK", jsonClient.jsonMerge("test_merge", Path.of((".age")), person.age));
    assertEquals(person, jsonClient.jsonGet("test_merge", Person.class));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jsonClient.jsonSetLegacy("qux1", qux1);
    jsonClient.jsonSetLegacy("qux2", qux2);

    List<Baz> allBaz = jsonClient.jsonMGet(Path.of("baz"), Baz.class, "qux1", "qux2");

    assertEquals(2, allBaz.size());

    Baz testBaz1 = allBaz.stream() //
        .filter(b -> b.quuz.equals("quuz1")) //
        .findFirst() //
        .orElseThrow(() -> new NullPointerException(""));
    Baz testBaz2 = allBaz.stream() //
        .filter(q -> q.quuz.equals("quuz2")) //
        .findFirst() //
        .orElseThrow(() -> new NullPointerException(""));

    assertEquals(baz1, testBaz1);
    assertEquals(baz2, testBaz2);
  }

  @Test
  public void mgetAtRootPathWithMissingKeys() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jsonClient.jsonSetLegacy("qux1", qux1);
    jsonClient.jsonSetLegacy("qux2", qux2);

    List<Qux> allQux = jsonClient.jsonMGet(Qux.class, "qux1", "qux2", "qux3");

    assertEquals(3, allQux.size());
    assertNull(allQux.get(2));
    allQux.removeAll(Collections.singleton(null));
    assertEquals(2, allQux.size());
  }

  @Test
  public void arrLen() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(Long.valueOf(3), jsonClient.jsonArrLen("foobar", Path.of(".fooArr")));
  }

  @Test
  public void arrLenDefaultPath() {
    assertNull(jsonClient.jsonArrLen("array"));
    jsonClient.jsonSetLegacy("array", new int[]{1, 2, 3});
    assertEquals(Long.valueOf(3), jsonClient.jsonArrLen("array"));
  }

  @Test
  public void clearArray() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());

    Path arrPath = Path.of(".fooArr");
    assertEquals(Long.valueOf(3), jsonClient.jsonArrLen("foobar", arrPath));

    assertEquals(1L, jsonClient.jsonClear("foobar", arrPath));
    assertEquals(Long.valueOf(0), jsonClient.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path strPath = Path.of("foo");
    assertEquals(0L, jsonClient.jsonClear("foobar", strPath));
    assertEquals("bar", jsonClient.jsonGet("foobar", String.class, strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    jsonClient.jsonSetLegacy("qux", qux);
    Path objPath = Path.of("baz");
    assertEquals(baz, jsonClient.jsonGet("qux", Baz.class, objPath));

    assertEquals(1L, jsonClient.jsonClear("qux", objPath));
    assertEquals(new Baz(null, null, null), jsonClient.jsonGet("qux", Baz.class, objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jsonClient.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6), jsonClient.jsonArrAppend("test_arrappend", Path.of(".b"), 4, 5, 6));

    Integer[] array = jsonClient.jsonGet("test_arrappend", Integer[].class, Path.of(".b"));
    assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 6}, array);
  }

  @Test
  public void arrAppendMultipleTypes() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jsonClient.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6), jsonClient.jsonArrAppend("test_arrappend", Path.of(".b"), "foo", true, null));

    Object[] array = jsonClient.jsonGet("test_arrappend", Object[].class, Path.of(".b"));

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[]{1.0, 2.0, 3.0, "foo", true, null}, array);
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jsonClient.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(4), jsonClient.jsonArrAppend("test_arrappend", Path.of(".c.d"), "foo", true, null));

    Object[] array = jsonClient.jsonGet("test_arrappend", Object[].class, Path.of(".c.d"));
    assertArrayEquals(new Object[]{"ello", "foo", true, null}, array);
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jsonClient.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(3), jsonClient.jsonArrAppend("test_arrappend", Path.of(".c.d"), "a", "b", "c"));

    String[] array = jsonClient.jsonGet("test_arrappend", String[].class, Path.of(".c.d"));
    assertArrayEquals(new String[]{"a", "b", "c"}, array);
  }

  @Test(expected = JedisDataException.class)
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jsonClient.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    jsonClient.jsonArrAppend("test_arrappend", Path.of(".a"), 1);
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    jsonClient.jsonArrIndex("quxquux", ROOT_PATH, gson.toJson(new Object()));
  }

  @Test
  public void arrIndexWithInts() {
    jsonClient.jsonSet("quxquux", ROOT_PATH, new int[]{8, 6, 7, 5, 3, 0, 9});
    assertEquals(2L, jsonClient.jsonArrIndex("quxquux", ROOT_PATH, 7));
    assertEquals(-1L, jsonClient.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    jsonClient.jsonSet("quxquux", ROOT_PATH, new String[]{"8", "6", "7", "5", "3", "0", "9"});
    assertEquals(2L, jsonClient.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(1L, jsonClient.jsonArrIndex("foobar", Path.of(".fooArr"), "b"));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexNonExistentPath() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(1L, jsonClient.jsonArrIndex("foobar", Path.of(".barArr"), "x"));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

    jsonClient.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, jsonClient.jsonArrInsert("test_arrinsert", ROOT_PATH, 1, "foo"));

    Object[] array = jsonClient.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[]{"hello", "foo", "world", true, 1.0, 3.0, null, false}, array);
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

    jsonClient.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, jsonClient.jsonArrInsert("test_arrinsert", ROOT_PATH, -1, "foo"));

    Object[] array = jsonClient.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);
    assertArrayEquals(new Object[]{"hello", "world", true, 1.0, 3.0, null, "foo", false}, array);
  }

  @Test
  public void testArrayPop() {
    jsonClient.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    assertEquals(Long.valueOf(4), jsonClient.jsonArrPop("arr", Long.class, ROOT_PATH));
    assertEquals(Long.valueOf(3), jsonClient.jsonArrPop("arr", Long.class, ROOT_PATH, -1));
    assertEquals(Long.valueOf(2), jsonClient.jsonArrPop("arr", Long.class));
    assertEquals(Long.valueOf(0), jsonClient.jsonArrPop("arr", Long.class, ROOT_PATH, 0));
    assertEquals(Double.valueOf(1), jsonClient.jsonArrPop("arr"));
  }

  @Test
  public void arrTrim() {
    jsonClient.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    assertEquals(Long.valueOf(3), jsonClient.jsonArrTrim("arr", ROOT_PATH, 1, 3));
    assertArrayEquals(new Integer[]{1, 2, 3}, jsonClient.jsonGet("arr", Integer[].class, ROOT_PATH));
  }

  @Test
  public void strAppend() {
    jsonClient.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(6L, jsonClient.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertEquals("foobar", jsonClient.jsonGet("str", String.class, ROOT_PATH));
    assertEquals(8L, jsonClient.jsonStrAppend("str", "ed"));
//    assertEquals("foobared", jsonClient.jsonGet("str", String.class));
    assertEquals("foobared", jsonClient.jsonGet("str"));
  }

  @Test
  public void strLen() {
    assertNull(jsonClient.jsonStrLen("str"));
    jsonClient.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(Long.valueOf(3), jsonClient.jsonStrLen("str"));
    assertEquals(Long.valueOf(3), jsonClient.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    jsonClient.jsonSetLegacy("doc", gson.fromJson("{a:3}", JsonObject.class));
    assertEquals(5d, jsonClient.jsonNumIncrBy("doc", Path.of(".a"), 2), 0d);
  }

  @Test
  public void obj() {
    assertNull(jsonClient.jsonObjLen("doc"));
    assertNull(jsonClient.jsonObjKeys("doc"));
    assertNull(jsonClient.jsonObjLen("doc", ROOT_PATH));
    assertNull(jsonClient.jsonObjKeys("doc", ROOT_PATH));

    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    jsonClient.jsonSetWithPlainString("doc", ROOT_PATH, json);
    assertEquals(Long.valueOf(2), jsonClient.jsonObjLen("doc"));
    assertEquals(Arrays.asList("a", "nested"), jsonClient.jsonObjKeys("doc"));
    assertEquals(Long.valueOf(2), jsonClient.jsonObjLen("doc", Path.of(".nested.a")));
    assertEquals(Arrays.asList("b", "c"), jsonClient.jsonObjKeys("doc", Path.of(".nested.a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(0L, jsonClient.jsonDebugMemory("json"));
    assertEquals(0L, jsonClient.jsonDebugMemory("json", ROOT_PATH));

    String json = "{ foo: 'bar', bar: { foo: 10 }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
    jsonClient.jsonSet("json", ROOT_PATH, jsonObject);
    // it is okay as long as any 'long' is returned
    jsonClient.jsonDebugMemory("json");
    jsonClient.jsonDebugMemory("json", ROOT_PATH);
    jsonClient.jsonDebugMemory("json", Path.of(".bar"));
  }

  @Test
  public void plainString() {
    String json = "{\"foo\":\"bar\",\"bar\":{\"foo\":10}}";
    assertEquals("OK", jsonClient.jsonSetWithPlainString("plain", ROOT_PATH, json));
    assertEquals(json, jsonClient.jsonGetAsPlainString("plain", ROOT_PATH));
  }

  @Test
  public void testJsonGsonParser() {
    Tick person = new Tick("foo", Instant.now());

    // setting the custom json gson parser
    client.setJsonObjectMapper(JsonObjectMapperTestUtil.getCustomGsonObjectMapper());

    jsonClient.jsonSet(person.getId(), ROOT_PATH, person);

    String valueExpected = jsonClient.jsonGet(person.getId(), String.class, Path.of(".created"));
    assertEquals(valueExpected, person.getCreated().toString());
  }

  @Test
  public void testDefaultJsonGsonParserStringsMustBeDifferent() {
    Tick tick = new Tick("foo", Instant.now());

    // using the default json gson parser which is automatically configured

    jsonClient.jsonSet(tick.getId(), ROOT_PATH, tick);

    Object valueExpected = jsonClient.jsonGet(tick.getId(), Path.of(".created"));
    assertNotEquals(valueExpected, tick.getCreated().toString());
  }

  @Test
  public void testJsonJacksonParser() {
    Tick person = new Tick("foo", Instant.now());

    // setting the custom json jackson parser
    client.setJsonObjectMapper(JsonObjectMapperTestUtil.getCustomJacksonObjectMapper());

    jsonClient.jsonSet(person.getId(), ROOT_PATH, person);

    String valueExpected = jsonClient.jsonGet(person.getId(), String.class, Path.of(".created"));
    assertEquals(valueExpected, person.getCreated().toString());
  }
}
