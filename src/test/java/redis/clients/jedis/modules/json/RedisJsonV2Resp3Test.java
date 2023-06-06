package redis.clients.jedis.modules.json;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path2.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.*;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.json.commands.RedisJsonV2Commands;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.util.RedisProtocolUtil;

public class RedisJsonV2Resp3Test extends RedisModuleCommandsTestBase {

  private static final Gson gson = new Gson();

  private RedisJsonV2Commands jsonClient;

  @BeforeClass
  public static void prepare() {
    Assume.assumeTrue(RedisProtocolUtil.getRedisProtocol() == RedisProtocol.RESP3);
    RedisModuleCommandsTestBase.prepare();
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    this.jsonClient = super.client;
  }

  private static void assertJsonGetSingleElement(Object expected, Object replyObject) {
    if (expected instanceof String) {
      assertEquals("\"" + expected + "\"", replyObject);
    } else if (expected instanceof JSONObject) {
      assertJsonObjectEquals((JSONObject) expected, replyObject);
    } else if (expected instanceof JSONArray) {
      assertJsonArrayEquals((JSONArray) expected, replyObject);
    } else {
      assertEquals(expected, replyObject);
    }
  }

  private static void assertJsonGetMultipleElement(List expectedList, List replyList) {
    assertEquals("expectedList and replyList are of different sizes.", expectedList.size(), replyList.size());
    final int size = expectedList.size();
    for (int index = 0; index < size; index++) {
      assertJsonGetSingleElement(expectedList.get(index), replyList.get(index));
    }
  }

  private static void assertJsonGetReplySingleElement(Object expected, List<List<Object>> reply) {
    assertJsonGetSingleElement(expected, reply.get(0).get(0));
  }

  private static void assertJsonTypeReplySingleElement(Class expected, List<List<Class<?>>> reply) {
    assertSame(expected, reply.get(0).get(0));
  }

  @Test
  public void basicSetGetShouldSucceed() {

    // naive set with a path
//    jsonClient.jsonSet("null", null, ROOT_PATH);
    jsonClient.jsonSetWithEscape("null", ROOT_PATH, (Object) null);
    assertJsonGetReplySingleElement(null, jsonClient.jsonGetResp3("null", ROOT_PATH));

    // real scalar value and no path
    jsonClient.jsonSetWithEscape("str", "strong");
    assertJsonGetReplySingleElement("strong", jsonClient.jsonGetResp3("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    jsonClient.jsonSetWithEscape("obj", obj);
    assertJsonObjectEquals(new JSONObject(gson.toJson(obj)),
        jsonClient.jsonGetResp3("obj").get(0).get(0));

    // check an update
    Path2 p = Path2.of(".str");
    jsonClient.jsonSet("obj", p, gson.toJson("strung"));
    assertJsonGetReplySingleElement("strung", jsonClient.jsonGetResp3("obj", p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    jsonClient.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    jsonClient.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonGetReplySingleElement("strangle", jsonClient.jsonGetResp3("obj", p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    jsonClient.jsonSet("obj", gson.toJson(new IRLObject()));
    Path2 p = Path2.of(".none");
    jsonClient.jsonSet("obj", p, gson.toJson("strangle"), JsonSetParams.jsonSetParams().nx());
    assertJsonGetReplySingleElement("strangle", jsonClient.jsonGetResp3("obj", p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    String objStr = gson.toJson(new IRLObject());
    jsonClient.jsonSet("obj1", new JSONObject(objStr));
//    jsonClient.jsonSet("obj1", "strangle", JsonSetParams.jsonSetParams().xx());
    jsonClient.jsonSetWithEscape("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonGetReplySingleElement("strangle", jsonClient.jsonGetResp3("obj1", ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    jsonClient.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    assertNull(jsonClient.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    jsonClient.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".none");
    assertNull(jsonClient.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test(expected = JedisDataException.class)
  public void setException() {
    // should error on non root path for new key
    jsonClient.jsonSet("test", Path2.of(".foo"), "bar");
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    jsonClient.jsonSetWithEscape("obj", obj);
    List<List<Object>> result = jsonClient.jsonGetResp3("obj", Path2.of("bool"), Path2.of("str"));
    assertJsonGetSingleElement(true, result.get(0).get(0));
    assertJsonGetSingleElement("string", result.get(1).get(0));
  }

  @Test
  public void getMultiLevels() {
    JSONObject obj = new JSONObject();
    obj.put("foo", "John");
    JSONObject inner = new JSONObject();
    inner.put("foo", "Jane");
    obj.put("bar", inner);
    jsonClient.jsonSet("multi", obj);
    assertJsonGetMultipleElement(Arrays.asList("John", "Jane"), jsonClient.jsonGetResp3("multi", new Path2("..foo")).get(0));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    jsonClient.jsonSetWithEscape("obj", obj);

    Path2 pbool = Path2.of(".bool");
    // check initial value
    assertJsonGetReplySingleElement(true, jsonClient.jsonGetResp3("obj", pbool));

    // true -> false
    jsonClient.jsonToggle("obj", pbool);
    assertJsonGetReplySingleElement(false, jsonClient.jsonGetResp3("obj", pbool));

    // false -> true
    jsonClient.jsonToggle("obj", pbool);
    assertJsonGetReplySingleElement(true, jsonClient.jsonGetResp3("obj", pbool));

    // ignore non-boolean field
    Path2 pstr = Path2.of(".str");
    assertEquals(singletonList(null), jsonClient.jsonToggle("obj", pstr));
    assertJsonGetReplySingleElement("string", jsonClient.jsonGetResp3("obj", pstr));
  }

  @Test
  public void getAbsent() {
    jsonClient.jsonSetWithEscape("test", ROOT_PATH, "foo");
    assertEquals(singletonList(Collections.emptyList()), jsonClient.jsonGetResp3("test", Path2.of(".bar")));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    jsonClient.jsonSetWithEscape("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, jsonClient.jsonDel("obj", Path2.of(".str")));
    assertTrue(client.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, jsonClient.jsonDel("obj"));
    assertFalse(client.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    jsonClient.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, jsonClient.jsonDel("foobar", Path2.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    jsonClient.jsonSet("foobar", ROOT_PATH, new JSONObject(gson.toJson(new FooBarObject())));
    assertJsonTypeReplySingleElement(Object.class, jsonClient.jsonTypeResp3("foobar", ROOT_PATH));
    assertJsonTypeReplySingleElement(String.class, jsonClient.jsonTypeResp3("foobar", Path2.of(".foo")));
    assertJsonTypeReplySingleElement(int.class, jsonClient.jsonTypeResp3("foobar", Path2.of(".fooI")));
    assertJsonTypeReplySingleElement(float.class, jsonClient.jsonTypeResp3("foobar", Path2.of(".fooF")));
    assertJsonTypeReplySingleElement(List.class, jsonClient.jsonTypeResp3("foobar", Path2.of(".fooArr")));
    assertJsonTypeReplySingleElement(boolean.class, jsonClient.jsonTypeResp3("foobar", Path2.of(".fooB")));
    assertEquals(singletonList(Collections.emptyList()), jsonClient.jsonTypeResp3("foobar", Path2.of(".fooErr")));
  }

  @Test
  public void testJsonMerge() {
    // Test with root path
    JSONObject json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":25,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jsonClient.jsonSet("test_merge", json));

    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jsonClient.jsonMerge("test_merge", Path2.of("$"), "{\"person\":{\"age\":30}}"));

    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge", Path2.of("$")));

    // Test with root path path $.a.b
    assertEquals("OK", jsonClient.jsonMerge("test_merge", Path2.of("$.person.address"), "{\"work\":\"Redis office\"}"));
    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge", Path2.of("$")));

    // Test with null value to delete a value
    assertEquals("OK", jsonClient.jsonMerge("test_merge", Path2.of("$.person"), "{\"age\":null}"));
    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge", Path2.of("$")));

    // cleanup
    assertEquals(1L, client.del("test_merge"));
  }

  @Test
  public void testJsonMergeArray()
  {
    // Test merge on an array
    JSONObject json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}");
    assertEquals("OK", jsonClient.jsonSet("test_merge_array", Path2.of("$"), json));
    assertEquals("OK", jsonClient.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));

    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge_array", Path2.of("$")));

    // assertEquals("{{a={b={c=[f]}}}", jsonClient.jsonGet("test_merge_array", Path2.of("$")));

    // Test merge an array on a value
    assertEquals("OK", jsonClient.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":\"d\"}}}"));
    assertEquals("OK", jsonClient.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));
    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge_array", Path2.of("$")));

    // Test with null value to delete an array value
    assertEquals("OK", jsonClient.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}"));
    assertEquals("OK", jsonClient.jsonMerge("test_merge_array", Path2.of("$.a.b"), "{\"c\":null}"));
    json = new JSONObject("{\"a\":{\"b\":{}}}");
    assertJsonGetReplySingleElement(json, jsonClient.jsonGetResp3("test_merge_array", Path2.of("$")));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jsonClient.jsonSet("qux1", new JSONObject(gson.toJson(qux1)));
    jsonClient.jsonSet("qux2", new JSONObject(gson.toJson(qux2)));

    List<JSONArray> list = jsonClient.jsonMGet(Path2.of("baz"), "qux1", "qux2");
    assertEquals(2, list.size());
    assertJsonArrayEquals(jsonArray(new JSONObject(gson.toJson(baz1))), list.get(0));
    assertJsonArrayEquals(jsonArray(new JSONObject(gson.toJson(baz2))), list.get(1));
  }

  @Test
  public void mgetAtRootPathWithMissingKeys() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jsonClient.jsonSetWithEscape("qux1", qux1);
    jsonClient.jsonSetWithEscape("qux2", qux2);

    List<JSONArray> list = jsonClient.jsonMGet("qux1", "qux2", "qux3");

    assertEquals(3, list.size());
    assertNull(list.get(2));
    list.removeAll(singletonList(null));
    assertEquals(2, list.size());
  }

  @Test
  public void arrLen() {
    jsonClient.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(5L), jsonClient.jsonArrLen("arr", ROOT_PATH));
  }

  @Test
  public void clearArray() {
    jsonClient.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));

    Path2 arrPath = Path2.of(".fooArr");
    assertEquals(singletonList(3L), jsonClient.jsonArrLen("foobar", arrPath));

    assertEquals(1L, jsonClient.jsonClear("foobar", arrPath));
    assertEquals(singletonList(0L), jsonClient.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path2 strPath = Path2.of(".foo");
    assertEquals(0L, jsonClient.jsonClear("foobar", strPath));
    assertJsonGetReplySingleElement("bar", jsonClient.jsonGetResp3("foobar", strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    jsonClient.jsonSet("qux", gson.toJson(qux));
    Path2 objPath = Path2.of(".baz");
//    assertEquals(baz, jsonClient.jsonGet("qux", objPath));

    assertEquals(1L, jsonClient.jsonClear("qux", objPath));
//    assertEquals(new Baz(null, null, null), jsonClient.jsonGet("qux", objPath));
    assertJsonGetReplySingleElement(new JSONObject(), jsonClient.jsonGetResp3("qux", objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonClient.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), jsonClient.jsonArrAppend("test_arrappend", Path2.of(".b"), 4, 5, 6));

    assertJsonGetReplySingleElement(jsonArray(1, 2, 3, 4, 5, 6), jsonClient.jsonGetResp3("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypes() {
    Object fooObject = gson.toJson("foo");
    Object trueObject = gson.toJson(true);
    Object nullObject = gson.toJson(null);
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonClient.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), jsonClient.jsonArrAppend("test_arrappend", Path2.of(".b"), fooObject, trueObject, nullObject));

    assertJsonGetReplySingleElement(jsonArray(1, 2, 3, "foo", true, null), jsonClient.jsonGetResp3("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonClient.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(4L), jsonClient.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "foo", true, null));

    assertJsonGetReplySingleElement(jsonArray("ello", "foo", true, null), jsonClient.jsonGetResp3("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    jsonClient.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(3L), jsonClient.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "a", "b", "c"));

    assertJsonGetReplySingleElement(jsonArray("a", "b", "c"), jsonClient.jsonGetResp3("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonClient.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(null), jsonClient.jsonArrAppend("test_arrappend", Path2.of(".a"), 1));
    assertEquals(singletonList(null), jsonClient.jsonArrAppend("test_arrappend", Path2.of(".a"), gson.toJson(1)));
    assertEquals(singletonList(null), jsonClient.jsonArrAppendWithEscape("test_arrappend", Path2.of(".a"), 1));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    jsonClient.jsonArrIndexWithEscape("quxquux", ROOT_PATH, new JSONObject());
  }

  @Test
  public void arrIndexWithInts() {
    jsonClient.jsonSetWithEscape("quxquux", ROOT_PATH, new int[]{8, 6, 7, 5, 3, 0, 9});
    assertEquals(singletonList(2L), jsonClient.jsonArrIndexWithEscape("quxquux", ROOT_PATH, 7));
    assertEquals(singletonList(-1L), jsonClient.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    jsonClient.jsonSetWithEscape("quxquux", ROOT_PATH, new String[]{"8", "6", "7", "5", "3", "0", "9"});
    assertEquals(singletonList(2L), jsonClient.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    jsonClient.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(singletonList(1L), jsonClient.jsonArrIndexWithEscape("foobar", Path2.of(".fooArr"), "b"));
  }

  @Test
  public void arrIndexNonExistentPath() {
    jsonClient.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));
    assertEquals(Collections.emptyList(), jsonClient.jsonArrIndex("foobar", Path2.of(".barArr"), gson.toJson("x")));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jsonClient.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), jsonClient.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, 1, "foo"));

    assertJsonGetReplySingleElement(jsonArray("hello", "foo", "world", true, 1, 3, null, false),
        jsonClient.jsonGetResp3("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jsonClient.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), jsonClient.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, -1, "foo"));

    assertJsonGetReplySingleElement(jsonArray("hello", "world", true, 1, 3, null, "foo", false),
        jsonClient.jsonGetResp3("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrPop() {
    jsonClient.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(4d), jsonClient.jsonArrPop("arr", ROOT_PATH));
    assertEquals(singletonList(3d), jsonClient.jsonArrPop("arr", ROOT_PATH, -1));
    assertEquals(singletonList(0d), jsonClient.jsonArrPop("arr", ROOT_PATH, 0));
  }

  @Test
  public void arrTrim() {
//    jsonClient.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    jsonClient.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(3L), jsonClient.jsonArrTrim("arr", ROOT_PATH, 1, 3));
//    assertArrayEquals(new Integer[]{1, 2, 3}, jsonClient.jsonGet("arr", Integer[].class, ROOT_PATH));
    assertJsonGetReplySingleElement(jsonArray(1, 2, 3), jsonClient.jsonGetResp3("arr", ROOT_PATH));
  }

  @Test
  public void strAppend() {
//    jsonClient.jsonSet("str", ROOT_PATH, "foo");
    jsonClient.jsonSet("str", ROOT_PATH, gson.toJson("foo"));
    assertEquals(singletonList(6L), jsonClient.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertJsonGetReplySingleElement("foobar", jsonClient.jsonGetResp3("str"));
  }

  @Test
  public void strLen() {
    jsonClient.jsonSetWithEscape("str", "foobar");
    assertEquals(singletonList(6L), jsonClient.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    jsonClient.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertEquals(Collections.singletonList(null), jsonClient.jsonNumIncrByResp3("doc", Path2.of(".a"), 1d));
    assertEquals(Arrays.asList(null, 4d, 7d, null), jsonClient.jsonNumIncrByResp3("doc", Path2.of("..a"), 2d));
    assertEquals(Collections.singletonList(null), jsonClient.jsonNumIncrByResp3("doc", Path2.of("..b"), 0d));
    assertEquals(Collections.emptyList(), jsonClient.jsonNumIncrByResp3("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void obj() {
    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    jsonClient.jsonSet("doc", ROOT_PATH, json);
    assertEquals(Arrays.asList(2L), jsonClient.jsonObjLen("doc", ROOT_PATH));
    assertEquals(Arrays.asList(Arrays.asList("a", "nested")), jsonClient.jsonObjKeys("doc", ROOT_PATH));
    assertEquals(Arrays.asList(null, 2L), jsonClient.jsonObjLen("doc", Path2.of("..a")));
    assertEquals(Arrays.asList(null, Arrays.asList("b", "c")), jsonClient.jsonObjKeys("doc", Path2.of("..a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(Collections.emptyList(), jsonClient.jsonDebugMemory("json", ROOT_PATH));

    jsonClient.jsonSet("json", new JSONObject("{ foo: 'bar', bar: { foo: 10 }}"));
    assertEquals(1, jsonClient.jsonDebugMemory("json", ROOT_PATH).size());
    assertEquals(2, jsonClient.jsonDebugMemory("json", Path2.of("$..foo")).size());
    assertEquals(1, jsonClient.jsonDebugMemory("json", Path2.of("$..bar")).size());
  }

  @Test
  public void resp() {
    assertNull(jsonClient.jsonResp("resp", ROOT_PATH));

    String json = "{\"foo\": {\"hello\":\"world\"}, \"bar\": [null, 3, 2.5, true]}";
    jsonClient.jsonSet("resp", ROOT_PATH, json);

    List<List<Object>> fullResp = jsonClient.jsonResp("resp", ROOT_PATH);
    assertEquals(1, fullResp.size());

    List<Object> resp = fullResp.get(0);
    assertEquals("{", resp.get(0));

    assertEquals("foo", resp.get(1));
    assertEquals(Arrays.asList("{", "hello", "world"), resp.get(2));

    assertEquals("bar", resp.get(3));
    List<Object> arr = (List<Object>) resp.get(4);
    assertEquals("[", arr.get(0));
    assertNull(arr.get(1));
    assertEquals(Long.valueOf(3), arr.get(2));
    //assertEquals("2.5", arr.get(3));
    MatcherAssert.assertThat(arr.get(3), Matchers.isOneOf("2.5", 2.5));
    assertEquals("true", arr.get(4));
  }

  private static void assertJsonArrayEquals(JSONArray a, Object _b) {
    if (!(_b instanceof JSONArray)) {
      fail("Actual value is not JSONArray.");
    }
    JSONArray b = (JSONArray) _b;
    assertEquals("JSONArray length mismatch", a.length(), b.length());
    int length = a.length();
    for (int index = 0; index < length; index++) {
      if (a.isNull(index)) {
        assertTrue(index + "'th element is not null", b.isNull(index));
        continue;
      }
      Object ia = a.get(index);
      Object ib = b.get(index);
      if (ia instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) ia, ib);
      } else if (ia instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) ia, ib);
      } else if (ia instanceof Number && ib instanceof Number) {
        assertEquals(index + "'th element mismatch", ((Number) ia).doubleValue(), ((Number) ib).doubleValue(), 0d);
      } else {
        assertEquals(index + "'th element mismatch", ia, ib);
      }
    }
  }

  private static void assertJsonObjectEquals(JSONObject a, Object _b) {
    if (!(_b instanceof JSONObject)) {
      fail("Actual value is not JSONObject.");
    }
    JSONObject b = (JSONObject) _b;
    assertEquals("JSONObject length mismatch", a.length(), b.length());
    assertEquals(a.keySet(), b.keySet());
    for (String key : a.keySet()) {
      if (a.isNull(key)) {
        assertTrue(key + "'s value is not null", b.isNull(key));
        continue;
      }
      Object oa = a.get(key);
      Object ob = b.get(key);
      if (oa instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) oa, ob);
      } else if (oa instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) oa, ob);
      } else {
        assertEquals(key + "'s value mismatch", oa, ob);
      }
    }
  }

  private static JSONArray jsonArray(Object... objects) {
    JSONArray arr = new JSONArray();
    for (Object o : objects) {
      arr.put(o);
    }
    return arr;
  }
}
