package redis.clients.jedis.modules.json;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path2.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.*;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class RedisJsonV2Test extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  private static final Gson gson = new Gson();

  @Test
  public void basicSetGetShouldSucceed() {

    // naive set with a path
//    client.jsonSet("null", null, ROOT_PATH);
    client.jsonSetWithEscape("null", ROOT_PATH, (Object) null);
    assertJsonArrayEquals(jsonArray((Object) null), client.jsonGet("null", ROOT_PATH));

    // real scalar value and no path
    client.jsonSetWithEscape("str", "strong");
    assertEquals("strong", client.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    client.jsonSetWithEscape("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(client.jsonGet("obj")));

    // check an update
    Path2 p = Path2.of(".str");
    client.jsonSet("obj", p, gson.toJson("strung"));
    assertJsonArrayEquals(jsonArray("strung"), client.jsonGet("obj", p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    client.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    client.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), client.jsonGet("obj", p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    client.jsonSet("obj", gson.toJson(new IRLObject()));
    Path2 p = Path2.of(".none");
    client.jsonSet("obj", p, gson.toJson("strangle"), JsonSetParams.jsonSetParams().nx());
    assertJsonArrayEquals(jsonArray("strangle"), client.jsonGet("obj", p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    String objStr = gson.toJson(new IRLObject());
    client.jsonSet("obj1", new JSONObject(objStr));
//    client.jsonSet("obj1", "strangle", JsonSetParams.jsonSetParams().xx());
    client.jsonSetWithEscape("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), client.jsonGet("obj1", ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    client.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    assertNull(client.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    client.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".none");
    assertNull(client.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test(expected = JedisDataException.class)
  public void setException() {
    // should error on non root path for new key
    client.jsonSet("test", Path2.of(".foo"), "bar");
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    client.jsonSetWithEscape("obj", obj);
    JSONObject result = (JSONObject) client.jsonGet("obj", Path2.of("bool"), Path2.of("str"));
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
    client.jsonSet("multi", obj);
    assertJsonArrayEquals(jsonArray("John", "Jane"), client.jsonGet("multi", new Path2("..foo")));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    client.jsonSetWithEscape("obj", obj);

    Path2 pbool = Path2.of(".bool");
    // check initial value
    assertJsonArrayEquals(jsonArray(true), client.jsonGet("obj", pbool));

    // true -> false
    client.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(false), client.jsonGet("obj", pbool));

    // false -> true
    client.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(true), client.jsonGet("obj", pbool));

    // ignore non-boolean field
    Path2 pstr = Path2.of(".str");
    assertEquals(singletonList(null), client.jsonToggle("obj", pstr));
    assertJsonArrayEquals(jsonArray("string"), client.jsonGet("obj", pstr));
  }

  @Test
  public void getAbsent() {
    client.jsonSetWithEscape("test", ROOT_PATH, "foo");
    assertJsonArrayEquals(jsonArray(), client.jsonGet("test", Path2.of(".bar")));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    client.jsonSetWithEscape("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, client.jsonDel("obj", Path2.of(".str")));
    assertTrue(client.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, client.jsonDel("obj"));
    assertFalse(client.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    client.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, client.jsonDel("foobar", Path2.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    client.jsonSet("foobar", ROOT_PATH, new JSONObject(gson.toJson(new FooBarObject())));
    assertSame(Object.class, client.jsonType("foobar"));
    assertEquals(singletonList(Object.class), client.jsonType("foobar", ROOT_PATH));
    assertEquals(singletonList(String.class), client.jsonType("foobar", Path2.of(".foo")));
    assertEquals(singletonList(int.class), client.jsonType("foobar", Path2.of(".fooI")));
    assertEquals(singletonList(float.class), client.jsonType("foobar", Path2.of(".fooF")));
    assertEquals(singletonList(List.class), client.jsonType("foobar", Path2.of(".fooArr")));
    assertEquals(singletonList(boolean.class), client.jsonType("foobar", Path2.of(".fooB")));
    assertEquals(emptyList(), client.jsonType("foobar", Path2.of(".fooErr")));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    client.jsonSet("qux1", new JSONObject(gson.toJson(qux1)));
    client.jsonSet("qux2", new JSONObject(gson.toJson(qux2)));

    List<JSONArray> list = client.jsonMGet(Path2.of("baz"), "qux1", "qux2");
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

    client.jsonSetWithEscape("qux1", qux1);
    client.jsonSetWithEscape("qux2", qux2);

    List<JSONArray> list = client.jsonMGet("qux1", "qux2", "qux3");

    assertEquals(3, list.size());
    assertNull(list.get(2));
    list.removeAll(singletonList(null));
    assertEquals(2, list.size());
  }

  @Test
  public void arrLen() {
    client.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(5L), client.jsonArrLen("arr", ROOT_PATH));
  }

  @Test
  public void clearArray() {
    client.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));

    Path2 arrPath = Path2.of(".fooArr");
    assertEquals(singletonList(3L), client.jsonArrLen("foobar", arrPath));

    assertEquals(1L, client.jsonClear("foobar", arrPath));
    assertEquals(singletonList(0L), client.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path2 strPath = Path2.of(".foo");
    assertEquals(0L, client.jsonClear("foobar", strPath));
    assertJsonArrayEquals(jsonArray("bar"), client.jsonGet("foobar", strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    client.jsonSet("qux", gson.toJson(qux));
    Path2 objPath = Path2.of(".baz");
//    assertEquals(baz, client.jsonGet("qux", objPath));

    assertEquals(1L, client.jsonClear("qux", objPath));
//    assertEquals(new Baz(null, null, null), client.jsonGet("qux", objPath));
    assertJsonArrayEquals(jsonArray(new JSONObject()), client.jsonGet("qux", objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), client.jsonArrAppend("test_arrappend", Path2.of(".b"), 4, 5, 6));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, 4, 5, 6)), client.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypes() {
    Object fooObject = gson.toJson("foo");
    Object trueObject = gson.toJson(true);
    Object nullObject = gson.toJson(null);
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), client.jsonArrAppend("test_arrappend", Path2.of(".b"), fooObject, trueObject, nullObject));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, "foo", true, null)), client.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(4L), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "foo", true, null));

    assertJsonArrayEquals(jsonArray(jsonArray("ello", "foo", true, null)), client.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(3L), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "a", "b", "c"));

    assertJsonArrayEquals(jsonArray(jsonArray("a", "b", "c")), client.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(null), client.jsonArrAppend("test_arrappend", Path2.of(".a"), 1));
    assertEquals(singletonList(null), client.jsonArrAppend("test_arrappend", Path2.of(".a"), gson.toJson(1)));
    assertEquals(singletonList(null), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".a"), 1));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    client.jsonArrIndexWithEscape("quxquux", ROOT_PATH, new JSONObject());
  }

  @Test
  public void arrIndexWithInts() {
    client.jsonSetWithEscape("quxquux", ROOT_PATH, new int[]{8, 6, 7, 5, 3, 0, 9});
    assertEquals(singletonList(2L), client.jsonArrIndexWithEscape("quxquux", ROOT_PATH, 7));
    assertEquals(singletonList(-1L), client.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    client.jsonSetWithEscape("quxquux", ROOT_PATH, new String[]{"8", "6", "7", "5", "3", "0", "9"});
    assertEquals(singletonList(2L), client.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    client.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(singletonList(1L), client.jsonArrIndexWithEscape("foobar", Path2.of(".fooArr"), "b"));
  }

  @Test
  public void arrIndexNonExistentPath() {
    client.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));
    assertEquals(emptyList(), client.jsonArrIndex("foobar", Path2.of(".barArr"), gson.toJson("x")));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    client.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), client.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, 1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "foo", "world", true, 1, 3, null, false)),
        client.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    client.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L), client.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, -1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "world", true, 1, 3, null, "foo", false)),
        client.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrPop() {
    client.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(4d), client.jsonArrPop("arr", ROOT_PATH));
    assertEquals(singletonList(3d), client.jsonArrPop("arr", ROOT_PATH, -1));
    assertEquals(singletonList(0d), client.jsonArrPop("arr", ROOT_PATH, 0));
  }

  @Test
  public void arrTrim() {
//    client.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    client.jsonSet("arr", ROOT_PATH, new JSONArray(new int[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(3L), client.jsonArrTrim("arr", ROOT_PATH, 1, 3));
//    assertArrayEquals(new Integer[]{1, 2, 3}, client.jsonGet("arr", Integer[].class, ROOT_PATH));
    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3)), client.jsonGet("arr", ROOT_PATH));
  }

  @Test
  public void strAppend() {
//    client.jsonSet("str", ROOT_PATH, "foo");
    client.jsonSet("str", ROOT_PATH, gson.toJson("foo"));
    assertEquals(singletonList(6L), client.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertEquals("foobar", client.jsonGet("str"));
  }

  @Test
  public void strLen() {
    client.jsonSetWithEscape("str", "foobar");
    assertEquals(singletonList(6L), client.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    client.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertJsonArrayEquals(jsonArray((Object) null), client.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertJsonArrayEquals(jsonArray(null, 4, 7, null), client.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertJsonArrayEquals(jsonArray((Object) null), client.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertJsonArrayEquals(jsonArray(), client.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void obj() {
    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    client.jsonSet("doc", ROOT_PATH, json);
    assertEquals(Arrays.asList(2L), client.jsonObjLen("doc", ROOT_PATH));
    assertEquals(Arrays.asList(Arrays.asList("a", "nested")), client.jsonObjKeys("doc", ROOT_PATH));
    assertEquals(Arrays.asList(null, 2L), client.jsonObjLen("doc", Path2.of("..a")));
    assertEquals(Arrays.asList(null, Arrays.asList("b", "c")), client.jsonObjKeys("doc", Path2.of("..a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(emptyList(), client.jsonDebugMemory("json", ROOT_PATH));

    client.jsonSet("json", new JSONObject("{ foo: 'bar', bar: { foo: 10 }}"));
    // it is okay as long as any 'long' is returned
    client.jsonDebugMemory("json");
    assertEquals(1, client.jsonDebugMemory("json", ROOT_PATH).size());
    assertEquals(2, client.jsonDebugMemory("json", Path2.of("$..foo")).size());
    assertEquals(1, client.jsonDebugMemory("json", Path2.of("$..bar")).size());
  }

  @Test
  public void resp() {
    assertNull(client.jsonResp("resp", ROOT_PATH));

    String json = "{\"foo\": {\"hello\":\"world\"}, \"bar\": [null, 3, 2.5, true]}";
    client.jsonSet("resp", ROOT_PATH, json);

    List<List<Object>> fullResp = client.jsonResp("resp", ROOT_PATH);
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
    assertEquals("2.5", arr.get(3));
    assertEquals("true", arr.get(4));
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
