package redis.clients.jedis.modules.json;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path2.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.*;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.json.commands.RedisJsonV2Commands;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

@RunWith(Parameterized.class)
public class RedisJsonV2Test extends RedisModuleCommandsTestBase {

  private static final Gson gson = new Gson();

  private RedisJsonV2Commands jsonV2;

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public RedisJsonV2Test(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    this.jsonV2 = super.client;
  }

  @Test
  public void basicSetGetShouldSucceed() {
    // naive set with a path
    jsonV2.jsonSetWithEscape("null", ROOT_PATH, (Object) null);
    assertJsonArrayEquals(jsonArray((Object) null), jsonV2.jsonGet("null", ROOT_PATH));

    // real scalar value and no path
    jsonV2.jsonSetWithEscape("str", "strong");
    assertEquals("strong", jsonV2.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    jsonV2.jsonSetWithEscape("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(jsonV2.jsonGet("obj")));

    // check an update
    Path2 p = Path2.of(".str");
    jsonV2.jsonSet("obj", p, gson.toJson("strung"));
    assertJsonArrayEquals(jsonArray("strung"), jsonV2.jsonGet("obj", p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    jsonV2.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    jsonV2.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), jsonV2.jsonGet("obj", p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    jsonV2.jsonSet("obj", gson.toJson(new IRLObject()));
    Path2 p = Path2.of(".none");
    jsonV2.jsonSet("obj", p, gson.toJson("strangle"), JsonSetParams.jsonSetParams().nx());
    assertJsonArrayEquals(jsonArray("strangle"), jsonV2.jsonGet("obj", p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    String objStr = gson.toJson(new IRLObject());
    jsonV2.jsonSet("obj1", new JSONObject(objStr));
//    jsonClient.jsonSet("obj1", "strangle", JsonSetParams.jsonSetParams().xx());
    jsonV2.jsonSetWithEscape("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), jsonV2.jsonGet("obj1", ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    jsonV2.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    assertNull(jsonV2.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    jsonV2.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".none");
    assertNull(jsonV2.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test(expected = JedisDataException.class)
  public void setException() {
    // should error on non root path for new key
    jsonV2.jsonSet("test", Path2.of(".foo"), "bar");
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    jsonV2.jsonSetWithEscape("obj", obj);
    JSONObject result = (JSONObject) jsonV2.jsonGet("obj", Path2.of("bool"), Path2.of("str"));
    assertJsonArrayEquals(jsonArray(true), result.get("$.bool"));
    assertJsonArrayEquals(jsonArray("string"), result.get("$.str"));
  }

  @Test
  public void getMultiLevels() {
    JSONObject obj = new JSONObject();
    obj.put("foo", "John");
    JSONObject inner = new JSONObject();
    inner.put("foo", "Jane");
    obj.put("bar", inner);
    jsonV2.jsonSet("multi", obj);
    assertJsonArrayEquals(jsonArray("John", "Jane"), jsonV2.jsonGet("multi", new Path2("..foo")));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    jsonV2.jsonSetWithEscape("obj", obj);

    Path2 pbool = Path2.of(".bool");
    // check initial value
    assertJsonArrayEquals(jsonArray(true), jsonV2.jsonGet("obj", pbool));

    // true -> false
    jsonV2.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(false), jsonV2.jsonGet("obj", pbool));

    // false -> true
    jsonV2.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(true), jsonV2.jsonGet("obj", pbool));

    // ignore non-boolean field
    Path2 pstr = Path2.of(".str");
    assertEquals(singletonList(null), jsonV2.jsonToggle("obj", pstr));
    assertJsonArrayEquals(jsonArray("string"), jsonV2.jsonGet("obj", pstr));
  }

  @Test
  public void getAbsent() {
    jsonV2.jsonSetWithEscape("test", ROOT_PATH, "foo");
    assertJsonArrayEquals(jsonArray(), jsonV2.jsonGet("test", Path2.of(".bar")));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    jsonV2.jsonSetWithEscape("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, jsonV2.jsonDel("obj", Path2.of(".str")));
    assertTrue(client.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, jsonV2.jsonDel("obj"));
    assertFalse(client.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    jsonV2.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, jsonV2.jsonDel("foobar", Path2.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    jsonV2.jsonSet("foobar", ROOT_PATH, new JSONObject(gson.toJson(new FooBarObject())));
    assertEquals(singletonList(Object.class), jsonV2.jsonType("foobar", ROOT_PATH));
    assertEquals(singletonList(String.class), jsonV2.jsonType("foobar", Path2.of(".foo")));
    assertEquals(singletonList(int.class), jsonV2.jsonType("foobar", Path2.of(".fooI")));
    assertEquals(singletonList(float.class), jsonV2.jsonType("foobar", Path2.of(".fooF")));
    assertEquals(singletonList(List.class), jsonV2.jsonType("foobar", Path2.of(".fooArr")));
    assertEquals(singletonList(boolean.class), jsonV2.jsonType("foobar", Path2.of(".fooB")));
    assertEquals(Collections.emptyList(), jsonV2.jsonType("foobar", Path2.of(".fooErr")));
  }

  @Test
  public void testJsonMerge() {
    // Test with root path
    JSONObject json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":25,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jsonV2.jsonSet("test_merge", json));

    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jsonV2.jsonMerge("test_merge", Path2.of("$"), "{\"person\":{\"age\":30}}"));

    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge", Path2.of("$")));

    // Test with root path path $.a.b
    assertEquals("OK", jsonV2.jsonMerge("test_merge", Path2.of("$.person.address"), "{\"work\":\"Redis office\"}"));
    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge", Path2.of("$")));

    // Test with null value to delete a value
    assertEquals("OK", jsonV2.jsonMerge("test_merge", Path2.of("$.person"), "{\"age\":null}"));
    json = new JSONObject("{\"person\":{\"name\":\"John Doe\",\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge", Path2.of("$")));

    // cleanup
    assertEquals(1L, client.del("test_merge"));
  }

  @Test
  public void testJsonMergeArray()
  {
    // Test merge on an array
    JSONObject json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}");
    assertEquals("OK", jsonV2.jsonSet("test_merge_array", Path2.of("$"), json));
    assertEquals("OK", jsonV2.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));

    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge_array", Path2.of("$")));

    // assertEquals("{{a={b={c=[f]}}}", jsonClient.jsonGet("test_merge_array", Path2.of("$")));

    // Test merge an array on a value
    assertEquals("OK", jsonV2.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":\"d\"}}}"));
    assertEquals("OK", jsonV2.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));
    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge_array", Path2.of("$")));

    // Test with null value to delete an array value
    assertEquals("OK", jsonV2.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}"));
    assertEquals("OK", jsonV2.jsonMerge("test_merge_array", Path2.of("$.a.b"), "{\"c\":null}"));
    json = new JSONObject("{\"a\":{\"b\":{}}}");
    assertJsonArrayEquals(jsonArray(json), jsonV2.jsonGet("test_merge_array", Path2.of("$")));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jsonV2.jsonSet("qux1", new JSONObject(gson.toJson(qux1)));
    jsonV2.jsonSet("qux2", new JSONObject(gson.toJson(qux2)));

    List<JSONArray> list = jsonV2.jsonMGet(Path2.of("baz"), "qux1", "qux2");
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

    jsonV2.jsonSetWithEscape("qux1", qux1);
    jsonV2.jsonSetWithEscape("qux2", qux2);

    List<JSONArray> list = jsonV2.jsonMGet("qux1", "qux2", "qux3");

    assertEquals(3, list.size());
    assertNull(list.get(2));
    list.removeAll(singletonList(null));
    assertEquals(2, list.size());
  }

  @Test
  public void arrLen() {
    jsonV2.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(5L), jsonV2.jsonArrLen("arr", ROOT_PATH));
  }

  @Test
  public void clearArray() {
    jsonV2.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));

    Path2 arrPath = Path2.of(".fooArr");
    assertEquals(singletonList(3L), jsonV2.jsonArrLen("foobar", arrPath));

    assertEquals(1L, jsonV2.jsonClear("foobar", arrPath));
    assertEquals(singletonList(0L), jsonV2.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path2 strPath = Path2.of(".foo");
    assertEquals(0L, jsonV2.jsonClear("foobar", strPath));
    assertJsonArrayEquals(jsonArray("bar"), jsonV2.jsonGet("foobar", strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    jsonV2.jsonSet("qux", gson.toJson(qux));
    Path2 objPath = Path2.of(".baz");
//    assertEquals(baz, jsonClient.jsonGet("qux", objPath));

    assertEquals(1L, jsonV2.jsonClear("qux", objPath));
//    assertEquals(new Baz(null, null, null), jsonClient.jsonGet("qux", objPath));
    assertJsonArrayEquals(jsonArray(new JSONObject()), jsonV2.jsonGet("qux", objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonV2.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), jsonV2.jsonArrAppend("test_arrappend", Path2.of(".b"), 4, 5, 6));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, 4, 5, 6)), jsonV2.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypes() {
    Object fooObject = gson.toJson("foo");
    Object trueObject = gson.toJson(true);
    Object nullObject = gson.toJson(null);
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonV2.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), jsonV2.jsonArrAppend("test_arrappend", Path2.of(".b"), fooObject, trueObject, nullObject));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, "foo", true, null)), jsonV2.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonV2.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(4L), jsonV2.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "foo", true, null));

    assertJsonArrayEquals(jsonArray(jsonArray("ello", "foo", true, null)), jsonV2.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    jsonV2.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(3L), jsonV2.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "a", "b", "c"));

    assertJsonArrayEquals(jsonArray(jsonArray("a", "b", "c")), jsonV2.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jsonV2.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(null), jsonV2.jsonArrAppend("test_arrappend", Path2.of(".a"), 1));
    assertEquals(singletonList(null), jsonV2.jsonArrAppend("test_arrappend", Path2.of(".a"), gson.toJson(1)));
    assertEquals(singletonList(null), jsonV2.jsonArrAppendWithEscape("test_arrappend", Path2.of(".a"), 1));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    jsonV2.jsonArrIndexWithEscape("quxquux", ROOT_PATH, new JSONObject());
  }

  @Test
  public void arrIndexWithInts() {
    jsonV2.jsonSetWithEscape("quxquux", ROOT_PATH, new int[]{8, 6, 7, 5, 3, 0, 9});
    assertEquals(singletonList(2L), jsonV2.jsonArrIndexWithEscape("quxquux", ROOT_PATH, 7));
    assertEquals(singletonList(-1L), jsonV2.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    jsonV2.jsonSetWithEscape("quxquux", ROOT_PATH, new String[]{"8", "6", "7", "5", "3", "0", "9"});
    assertEquals(singletonList(2L), jsonV2.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    jsonV2.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(singletonList(1L), jsonV2.jsonArrIndexWithEscape("foobar", Path2.of(".fooArr"), "b"));
  }

  @Test
  public void arrIndexNonExistentPath() {
    jsonV2.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));
    assertEquals(Collections.emptyList(), jsonV2.jsonArrIndex("foobar", Path2.of(".barArr"), gson.toJson("x")));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jsonV2.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), jsonV2.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, 1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "foo", "world", true, 1, 3, null, false)),
        jsonV2.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jsonV2.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), jsonV2.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, -1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "world", true, 1, 3, null, "foo", false)),
        jsonV2.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrPop() {
    jsonV2.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(4d), jsonV2.jsonArrPop("arr", ROOT_PATH));
    assertEquals(singletonList(3d), jsonV2.jsonArrPop("arr", ROOT_PATH, -1));
    assertEquals(singletonList(0d), jsonV2.jsonArrPop("arr", ROOT_PATH, 0));
  }

  @Test
  public void arrTrim() {
//    jsonClient.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    jsonV2.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(3L), jsonV2.jsonArrTrim("arr", ROOT_PATH, 1, 3));
//    assertArrayEquals(new Integer[]{1, 2, 3}, jsonClient.jsonGet("arr", Integer[].class, ROOT_PATH));
    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3)), jsonV2.jsonGet("arr", ROOT_PATH));
  }

  @Test
  public void strAppend() {
//    jsonClient.jsonSet("str", ROOT_PATH, "foo");
    jsonV2.jsonSet("str", ROOT_PATH, gson.toJson("foo"));
    assertEquals(singletonList(6L), jsonV2.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertJsonArrayEquals(jsonArray("foobar"), jsonV2.jsonGet("str", ROOT_PATH));
  }

  @Test
  public void strLen() {
    jsonV2.jsonSetWithEscape("str", "foobar");
    assertEquals(singletonList(6L), jsonV2.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3);
    jsonV2.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertJsonArrayEquals(jsonArray((Object) null), jsonV2.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertJsonArrayEquals(jsonArray(null, 4, 7, null), jsonV2.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertJsonArrayEquals(jsonArray((Object) null), jsonV2.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertJsonArrayEquals(jsonArray(), jsonV2.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void numIncrByResp3() {
    Assume.assumeTrue(protocol == RedisProtocol.RESP3);
    jsonV2.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertEquals(singletonList((Object) null), jsonV2.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertEquals(Arrays.asList(null, 4d, 7d, null), jsonV2.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertEquals(singletonList((Object) null), jsonV2.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertEquals(Collections.emptyList(), jsonV2.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void obj() {
    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    jsonV2.jsonSet("doc", ROOT_PATH, json);
    assertEquals(Arrays.asList(2L), jsonV2.jsonObjLen("doc", ROOT_PATH));
    assertEquals(Arrays.asList(Arrays.asList("a", "nested")), jsonV2.jsonObjKeys("doc", ROOT_PATH));
    assertEquals(Arrays.asList(null, 2L), jsonV2.jsonObjLen("doc", Path2.of("..a")));
    assertEquals(Arrays.asList(null, Arrays.asList("b", "c")), jsonV2.jsonObjKeys("doc", Path2.of("..a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(Collections.emptyList(), jsonV2.jsonDebugMemory("json", ROOT_PATH));

    jsonV2.jsonSet("json", new JSONObject("{ foo: 'bar', bar: { foo: 10 }}"));
    assertEquals(1, jsonV2.jsonDebugMemory("json", ROOT_PATH).size());
    assertEquals(2, jsonV2.jsonDebugMemory("json", Path2.of("$..foo")).size());
    assertEquals(1, jsonV2.jsonDebugMemory("json", Path2.of("$..bar")).size());
  }

  private void assertJsonArrayEquals(JSONArray a, Object _b) {
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

  private void assertJsonObjectEquals(JSONObject a, Object _b) {
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
