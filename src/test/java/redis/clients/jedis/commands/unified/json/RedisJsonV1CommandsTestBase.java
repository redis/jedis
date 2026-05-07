package redis.clients.jedis.commands.unified.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.json.Path.ROOT_PATH;
import static redis.clients.jedis.json.JsonObjects.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.util.JsonObjectMapperTestUtil;

/**
 * Base test class for RedisJSON V1 commands using the UnifiedJedis pattern. V1 of the RedisJSON is
 * only supported with RESP2.
 */
@Tag("json")
public abstract class RedisJsonV1CommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final Gson gson = new Gson();

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public RedisJsonV1CommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void basicSetGetShouldSucceed() {
    // naive set with a path
    jedis.jsonSet("null", ROOT_PATH, (Object) null);
    assertNull(jedis.jsonGet("null", String.class, ROOT_PATH));

    // real scalar value and no path
    jedis.jsonSet("str", ROOT_PATH, "strong");
    assertEquals("strong", jedis.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    jedis.jsonSet("obj", ROOT_PATH, obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(jedis.jsonGet("obj")));

    // check an update
    Path p = Path.of(".str");
    jedis.jsonSet("obj", p, "strung");
    assertEquals("strung", jedis.jsonGet("obj", String.class, p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    jedis.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    jedis.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", jedis.jsonGet("obj", String.class, p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    jedis.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    jedis.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx());
    assertEquals("strangle", jedis.jsonGet("obj", String.class, p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    jedis.jsonSet("obj1", ROOT_PATH, new IRLObject());
    jedis.jsonSetLegacy("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", jedis.jsonGet("obj1", String.class, ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    jedis.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    assertNull(jedis.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    jedis.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    assertNull(jedis.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test
  public void setException() {
    // should error on non root path for new key
    assertThrows(JedisDataException.class, () -> jedis.jsonSet("test", Path.of(".foo"), "bar"));
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    jedis.jsonSetLegacy("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(
      expected.equals(jedis.jsonGet("obj", Object.class, Path.of("bool"), Path.of("str"))));
  }

  @Test
  public void toggle() {
    IRLObject obj = new IRLObject();
    jedis.jsonSetLegacy("obj", obj);

    Path pbool = Path.of(".bool");
    // check initial value
    assertTrue(jedis.jsonGet("obj", Boolean.class, pbool));

    // true -> false
    jedis.jsonToggle("obj", pbool);
    assertFalse(jedis.jsonGet("obj", Boolean.class, pbool));

    // false -> true
    jedis.jsonToggle("obj", pbool);
    assertTrue(jedis.jsonGet("obj", Boolean.class, pbool));

    // ignore non-boolean field
    Path pstr = Path.of(".str");
    try {
      jedis.jsonToggle("obj", pstr);
      fail("String not a bool");
    } catch (JedisDataException jde) {
      assertTrue(jde.getMessage().contains("not a bool"));
    }
    assertEquals("string", jedis.jsonGet("obj", String.class, pstr));
  }

  @Test
  public void getAbsent() {
    jedis.jsonSet("test", ROOT_PATH, "foo");
    assertThrows(JedisDataException.class,
      () -> jedis.jsonGet("test", String.class, Path.of(".bar")));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    jedis.jsonSet("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, jedis.jsonDel("obj", Path.of(".str")));
    assertTrue(jedis.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, jedis.jsonDel("obj"));
    assertFalse(jedis.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, jedis.jsonDel("foobar", Path.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    assertNull(jedis.jsonType("foobar"));
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertSame(Object.class, jedis.jsonType("foobar"));
    assertSame(Object.class, jedis.jsonType("foobar", ROOT_PATH));
    assertSame(String.class, jedis.jsonType("foobar", Path.of(".foo")));
    assertSame(int.class, jedis.jsonType("foobar", Path.of(".fooI")));
    assertSame(float.class, jedis.jsonType("foobar", Path.of(".fooF")));
    assertSame(List.class, jedis.jsonType("foobar", Path.of(".fooArr")));
    assertSame(boolean.class, jedis.jsonType("foobar", Path.of(".fooB")));
    assertNull(jedis.jsonType("foobar", Path.of(".fooErr")));
  }

  @Test
  public void testJsonMerge() {
    // create data
    List<String> childrens = new ArrayList<>();
    childrens.add("Child 1");
    Person person = new Person("John Doe", 25, "123 Main Street", "123-456-7890", childrens);
    assertEquals("OK", jedis.jsonSet("test_merge", ROOT_PATH, person));

    // After 5 years:
    person.age = 30;
    person.childrens.add("Child 2");
    person.childrens.add("Child 3");

    // merge the new data
    assertEquals("OK", jedis.jsonMerge("test_merge", Path.of((".childrens")), person.childrens));
    assertEquals("OK", jedis.jsonMerge("test_merge", Path.of((".age")), person.age));
    assertEquals(person, jedis.jsonGet("test_merge", Person.class));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jedis.jsonSetLegacy("{mget}qux1", qux1);
    jedis.jsonSetLegacy("{mget}qux2", qux2);

    List<Baz> allBaz = jedis.jsonMGet(Path.of("baz"), Baz.class, "{mget}qux1", "{mget}qux2");

    assertEquals(2, allBaz.size());

    Baz testBaz1 = allBaz.stream().filter(b -> b.quuz.equals("quuz1")).findFirst()
        .orElseThrow(() -> new NullPointerException(""));
    Baz testBaz2 = allBaz.stream().filter(q -> q.quuz.equals("quuz2")).findFirst()
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

    jedis.jsonSetLegacy("{mget}qux1", qux1);
    jedis.jsonSetLegacy("{mget}qux2", qux2);

    List<Qux> allQux = jedis.jsonMGet(Qux.class, "{mget}qux1", "{mget}qux2", "{mget}qux3");

    assertEquals(3, allQux.size());
    assertNull(allQux.get(2));
    allQux.removeAll(Collections.singleton(null));
    assertEquals(2, allQux.size());
  }

  @Test
  public void arrLen() {
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(Long.valueOf(3), jedis.jsonArrLen("foobar", Path.of(".fooArr")));
  }

  @Test
  public void arrLenDefaultPath() {
    assertNull(jedis.jsonArrLen("array"));
    jedis.jsonSetLegacy("array", new int[] { 1, 2, 3 });
    assertEquals(Long.valueOf(3), jedis.jsonArrLen("array"));
  }

  @Test
  public void clearArray() {
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());

    Path arrPath = Path.of(".fooArr");
    assertEquals(Long.valueOf(3), jedis.jsonArrLen("foobar", arrPath));

    assertEquals(1L, jedis.jsonClear("foobar", arrPath));
    assertEquals(Long.valueOf(0), jedis.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path strPath = Path.of("foo");
    assertEquals(0L, jedis.jsonClear("foobar", strPath));
    assertEquals("bar", jedis.jsonGet("foobar", String.class, strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    jedis.jsonSetLegacy("qux", qux);
    Path objPath = Path.of("baz");
    assertEquals(baz, jedis.jsonGet("qux", Baz.class, objPath));

    assertEquals(1L, jedis.jsonClear("qux", objPath));
    assertEquals(new Baz(null, null, null), jedis.jsonGet("qux", Baz.class, objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jedis.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6), jedis.jsonArrAppend("test_arrappend", Path.of(".b"), 4, 5, 6));

    Integer[] array = jedis.jsonGet("test_arrappend", Integer[].class, Path.of(".b"));
    assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, array);
  }

  @Test
  public void arrAppendMultipleTypes() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jedis.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6),
      jedis.jsonArrAppend("test_arrappend", Path.of(".b"), "foo", true, null));

    Object[] array = jedis.jsonGet("test_arrappend", Object[].class, Path.of(".b"));

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[] { 1.0, 2.0, 3.0, "foo", true, null }, array);
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jedis.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(4),
      jedis.jsonArrAppend("test_arrappend", Path.of(".c.d"), "foo", true, null));

    Object[] array = jedis.jsonGet("test_arrappend", Object[].class, Path.of(".c.d"));
    assertArrayEquals(new Object[] { "ello", "foo", true, null }, array);
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jedis.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(3),
      jedis.jsonArrAppend("test_arrappend", Path.of(".c.d"), "a", "b", "c"));

    String[] array = jedis.jsonGet("test_arrappend", String[].class, Path.of(".c.d"));
    assertArrayEquals(new String[] { "a", "b", "c" }, array);
  }

  @Test
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

    jedis.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertThrows(JedisDataException.class,
      () -> jedis.jsonArrAppend("test_arrappend", Path.of(".a"), 1));
  }

  @Test
  public void arrIndexAbsentKey() {
    assertThrows(JedisDataException.class,
      () -> jedis.jsonArrIndex("quxquux", ROOT_PATH, gson.toJson(new Object())));
  }

  @Test
  public void arrIndexWithInts() {
    jedis.jsonSet("quxquux", ROOT_PATH, new int[] { 8, 6, 7, 5, 3, 0, 9 });
    assertEquals(2L, jedis.jsonArrIndex("quxquux", ROOT_PATH, 7));
    assertEquals(-1L, jedis.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    jedis.jsonSet("quxquux", ROOT_PATH, new String[] { "8", "6", "7", "5", "3", "0", "9" });
    assertEquals(2L, jedis.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(1L, jedis.jsonArrIndex("foobar", Path.of(".fooArr"), "b"));
  }

  @Test
  public void arrIndexNonExistentPath() {
    jedis.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertThrows(JedisDataException.class,
      () -> assertEquals(1L, jedis.jsonArrIndex("foobar", Path.of(".barArr"), "x")));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

    jedis.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, jedis.jsonArrInsert("test_arrinsert", ROOT_PATH, 1, "foo"));

    Object[] array = jedis.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[] { "hello", "foo", "world", true, 1.0, 3.0, null, false }, array);
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

    jedis.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, jedis.jsonArrInsert("test_arrinsert", ROOT_PATH, -1, "foo"));

    Object[] array = jedis.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);
    assertArrayEquals(new Object[] { "hello", "world", true, 1.0, 3.0, null, "foo", false }, array);
  }

  @Test
  public void testArrayPop() {
    jedis.jsonSet("arr", ROOT_PATH, new int[] { 0, 1, 2, 3, 4 });
    assertEquals(Long.valueOf(4), jedis.jsonArrPop("arr", Long.class, ROOT_PATH));
    assertEquals(Long.valueOf(3), jedis.jsonArrPop("arr", Long.class, ROOT_PATH, -1));
    assertEquals(Long.valueOf(2), jedis.jsonArrPop("arr", Long.class));
    assertEquals(Long.valueOf(0), jedis.jsonArrPop("arr", Long.class, ROOT_PATH, 0));
    assertEquals(Double.valueOf(1), jedis.jsonArrPop("arr"));
  }

  @Test
  public void arrTrim() {
    jedis.jsonSet("arr", ROOT_PATH, new int[] { 0, 1, 2, 3, 4 });
    assertEquals(Long.valueOf(3), jedis.jsonArrTrim("arr", ROOT_PATH, 1, 3));
    assertArrayEquals(new Integer[] { 1, 2, 3 }, jedis.jsonGet("arr", Integer[].class, ROOT_PATH));
  }

  @Test
  public void strAppend() {
    jedis.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(6L, jedis.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertEquals("foobar", jedis.jsonGet("str", String.class, ROOT_PATH));
    assertEquals(8L, jedis.jsonStrAppend("str", "ed"));
    assertEquals("foobared", jedis.jsonGet("str"));
  }

  @Test
  public void strLen() {
    assertNull(jedis.jsonStrLen("str"));
    jedis.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(Long.valueOf(3), jedis.jsonStrLen("str"));
    assertEquals(Long.valueOf(3), jedis.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    jedis.jsonSetLegacy("doc", gson.fromJson("{a:3}", JsonObject.class));
    assertEquals(5d, jedis.jsonNumIncrBy("doc", Path.of(".a"), 2), 0d);
  }

  @Test
  public void obj() {
    assertNull(jedis.jsonObjLen("doc"));
    assertNull(jedis.jsonObjKeys("doc"));
    assertNull(jedis.jsonObjLen("doc", ROOT_PATH));
    assertNull(jedis.jsonObjKeys("doc", ROOT_PATH));

    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    jedis.jsonSetWithPlainString("doc", ROOT_PATH, json);
    assertEquals(Long.valueOf(2), jedis.jsonObjLen("doc"));
    assertEquals(Arrays.asList("a", "nested"), jedis.jsonObjKeys("doc"));
    assertEquals(Long.valueOf(2), jedis.jsonObjLen("doc", Path.of(".nested.a")));
    assertEquals(Arrays.asList("b", "c"), jedis.jsonObjKeys("doc", Path.of(".nested.a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(0L, jedis.jsonDebugMemory("json"));
    assertEquals(0L, jedis.jsonDebugMemory("json", ROOT_PATH));

    String json = "{ foo: 'bar', bar: { foo: 10 }}";
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
    jedis.jsonSet("json", ROOT_PATH, jsonObject);
    // it is okay as long as any 'long' is returned
    jedis.jsonDebugMemory("json");
    jedis.jsonDebugMemory("json", ROOT_PATH);
    jedis.jsonDebugMemory("json", Path.of(".bar"));
  }

  @Test
  public void plainString() {
    String json = "{\"foo\":\"bar\",\"bar\":{\"foo\":10}}";
    assertEquals("OK", jedis.jsonSetWithPlainString("plain", ROOT_PATH, json));
    assertEquals(json, jedis.jsonGetAsPlainString("plain", ROOT_PATH));
  }

  @Test
  public void testJsonGsonParser() {
    Tick person = new Tick("foo", Instant.now());

    // setting the custom json gson parser
    jedis.setJsonObjectMapper(JsonObjectMapperTestUtil.getCustomGsonObjectMapper());

    jedis.jsonSet(person.getId(), ROOT_PATH, person);

    String valueExpected = jedis.jsonGet(person.getId(), String.class, Path.of(".created"));
    assertEquals(valueExpected, person.getCreated().toString());
  }

  @Test
  public void testDefaultJsonGsonParserStringsMustBeDifferent() {
    Tick tick = new Tick("foo", Instant.now());

    // using the default json gson parser which is automatically configured

    jedis.jsonSet(tick.getId(), ROOT_PATH, tick);

    Object valueExpected = jedis.jsonGet(tick.getId(), Path.of(".created"));
    assertNotEquals(valueExpected, tick.getCreated().toString());
  }

  @Test
  public void testJsonJacksonParser() {
    Tick person = new Tick("foo", Instant.now());

    // setting the custom json jackson parser
    jedis.setJsonObjectMapper(JsonObjectMapperTestUtil.getCustomJacksonObjectMapper());

    jedis.jsonSet(person.getId(), ROOT_PATH, person);

    String valueExpected = jedis.jsonGet(person.getId(), String.class, Path.of(".created"));
    assertEquals(valueExpected, person.getCreated().toString());
  }
}
