package redis.clients.jedis.commands.unified.json;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static redis.clients.jedis.json.Path2.ROOT_PATH;
import static redis.clients.jedis.json.JsonObjects.*;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;

/**
 * Base test class for RedisJSON V2 commands using the UnifiedJedis pattern.
 */
@Tag("json")
public abstract class RedisJsonV2CommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final Gson gson = new Gson();

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public RedisJsonV2CommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void basicSetGetShouldSucceed() {
    // naive set with a path
    jedis.jsonSetWithEscape("null", ROOT_PATH, (Object) null);
    assertJsonArrayEquals(jsonArray((Object) null), jedis.jsonGet("null", ROOT_PATH));

    // real scalar value and no path
    jedis.jsonSetWithEscape("str", "strong");
    assertEquals("strong", jedis.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    jedis.jsonSetWithEscape("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertEquals(expected, jedis.jsonGet("obj"));

    // check an update
    Path2 p = Path2.of(".str");
    jedis.jsonSet("obj", p, gson.toJson("strung"));
    assertJsonArrayEquals(jsonArray("strung"), jedis.jsonGet("obj", p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    jedis.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    jedis.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), jedis.jsonGet("obj", p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    jedis.jsonSet("obj", gson.toJson(new IRLObject()));
    Path2 p = Path2.of(".none");
    jedis.jsonSet("obj", p, gson.toJson("strangle"), JsonSetParams.jsonSetParams().nx());
    assertJsonArrayEquals(jsonArray("strangle"), jedis.jsonGet("obj", p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    String objStr = gson.toJson(new IRLObject());
    jedis.jsonSet("obj1", new JSONObject(objStr));
    jedis.jsonSetWithEscape("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertJsonArrayEquals(jsonArray("strangle"), jedis.jsonGet("obj1", ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    jedis.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    assertNull(jedis.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    jedis.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".none");
    assertNull(jedis.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test
  public void setException() {
    // should error on non root path for new key
    assertThrows(JedisDataException.class, () -> jedis.jsonSet("test", Path2.of(".foo"), "bar"));
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    jedis.jsonSetWithEscape("obj", obj);
    JSONObject result = (JSONObject) jedis.jsonGet("obj", Path2.of("bool"), Path2.of("str"));
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
    jedis.jsonSet("multi", obj);
    assertJsonArrayEquals(jsonArray("John", "Jane"), jedis.jsonGet("multi", new Path2("..foo")));
  }

  @Test
  public void toggle() {
    IRLObject obj = new IRLObject();
    jedis.jsonSetWithEscape("obj", obj);

    Path2 pbool = Path2.of(".bool");
    // check initial value
    assertJsonArrayEquals(jsonArray(true), jedis.jsonGet("obj", pbool));

    // true -> false
    jedis.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(false), jedis.jsonGet("obj", pbool));

    // false -> true
    jedis.jsonToggle("obj", pbool);
    assertJsonArrayEquals(jsonArray(true), jedis.jsonGet("obj", pbool));

    // ignore non-boolean field
    Path2 pstr = Path2.of(".str");
    assertEquals(singletonList(null), jedis.jsonToggle("obj", pstr));
    assertJsonArrayEquals(jsonArray("string"), jedis.jsonGet("obj", pstr));
  }

  @Test
  public void getAbsent() {
    jedis.jsonSetWithEscape("test", ROOT_PATH, "foo");
    assertJsonArrayEquals(jsonArray(), jedis.jsonGet("test", Path2.of(".bar")));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    jedis.jsonSetWithEscape("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, jedis.jsonDel("obj", Path2.of(".str")));
    assertTrue(jedis.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, jedis.jsonDel("obj"));
    assertFalse(jedis.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    jedis.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, jedis.jsonDel("foobar", Path2.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    jedis.jsonSet("foobar", ROOT_PATH, new JSONObject(gson.toJson(new FooBarObject())));
    assertEquals(singletonList(Object.class), jedis.jsonType("foobar", ROOT_PATH));
    assertEquals(singletonList(String.class), jedis.jsonType("foobar", Path2.of(".foo")));
    assertEquals(singletonList(int.class), jedis.jsonType("foobar", Path2.of(".fooI")));
    assertEquals(singletonList(float.class), jedis.jsonType("foobar", Path2.of(".fooF")));
    assertEquals(singletonList(List.class), jedis.jsonType("foobar", Path2.of(".fooArr")));
    assertEquals(singletonList(boolean.class), jedis.jsonType("foobar", Path2.of(".fooB")));
    assertEquals(Collections.emptyList(), jedis.jsonType("foobar", Path2.of(".fooErr")));
  }

  @Test
  public void testJsonMerge() {
    // Test with root path
    JSONObject json = new JSONObject(
        "{\"person\":{\"name\":\"John Doe\",\"age\":25,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jedis.jsonSet("test_merge", json));

    json = new JSONObject(
        "{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\"},\"phone\":\"123-456-7890\"}}");
    assertEquals("OK", jedis.jsonMerge("test_merge", Path2.of("$"), "{\"person\":{\"age\":30}}"));

    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge", Path2.of("$")));

    // Test with root path path $.a.b
    assertEquals("OK",
      jedis.jsonMerge("test_merge", Path2.of("$.person.address"), "{\"work\":\"Redis office\"}"));
    json = new JSONObject(
        "{\"person\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge", Path2.of("$")));

    // Test with null value to delete a value
    assertEquals("OK", jedis.jsonMerge("test_merge", Path2.of("$.person"), "{\"age\":null}"));
    json = new JSONObject(
        "{\"person\":{\"name\":\"John Doe\",\"address\":{\"home\":\"123 Main Street\",\"work\":\"Redis office\"},\"phone\":\"123-456-7890\"}}");
    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge", Path2.of("$")));

    // cleanup
    assertEquals(1L, jedis.del("test_merge"));
  }

  @Test
  public void testJsonMergeArray() {
    // Test merge on an array
    JSONObject json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}");
    assertEquals("OK", jedis.jsonSet("test_merge_array", Path2.of("$"), json));
    assertEquals("OK", jedis.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));

    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge_array", Path2.of("$")));

    // Test merge an array on a value
    assertEquals("OK",
      jedis.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":\"d\"}}}"));
    assertEquals("OK", jedis.jsonMerge("test_merge_array", Path2.of("$.a.b.c"), "[\"f\"]"));
    json = new JSONObject("{\"a\":{\"b\":{\"c\":[\"f\"]}}}");
    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge_array", Path2.of("$")));

    // Test with null value to delete an array value
    assertEquals("OK",
      jedis.jsonSet("test_merge_array", Path2.of("$"), "{\"a\":{\"b\":{\"c\":[\"d\",\"e\"]}}}"));
    assertEquals("OK", jedis.jsonMerge("test_merge_array", Path2.of("$.a.b"), "{\"c\":null}"));
    json = new JSONObject("{\"a\":{\"b\":{}}}");
    assertJsonArrayEquals(jsonArray(json), jedis.jsonGet("test_merge_array", Path2.of("$")));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    jedis.jsonSet("{mget}qux1", new JSONObject(gson.toJson(qux1)));
    jedis.jsonSet("{mget}qux2", new JSONObject(gson.toJson(qux2)));

    List<JSONArray> list = jedis.jsonMGet(Path2.of("baz"), "{mget}qux1", "{mget}qux2");
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

    jedis.jsonSetWithEscape("{mget}qux1", qux1);
    jedis.jsonSetWithEscape("{mget}qux2", qux2);

    List<JSONArray> list = jedis.jsonMGet("{mget}qux1", "{mget}qux2", "{mget}qux3");

    assertEquals(3, list.size());
    assertNull(list.get(2));
    list.removeAll(singletonList(null));
    assertEquals(2, list.size());
  }

  @Test
  public void arrLen() {
    jedis.jsonSet("arr", ROOT_PATH, new JSONArray(new int[] { 0, 1, 2, 3, 4 }));
    assertEquals(singletonList(5L), jedis.jsonArrLen("arr", ROOT_PATH));
  }

  @Test
  public void clearArray() {
    jedis.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));

    Path2 arrPath = Path2.of(".fooArr");
    assertEquals(singletonList(3L), jedis.jsonArrLen("foobar", arrPath));

    assertEquals(1L, jedis.jsonClear("foobar", arrPath));
    assertEquals(singletonList(0L), jedis.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path2 strPath = Path2.of(".foo");
    assertEquals(0L, jedis.jsonClear("foobar", strPath));
    assertJsonArrayEquals(jsonArray("bar"), jedis.jsonGet("foobar", strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    jedis.jsonSet("qux", gson.toJson(qux));
    Path2 objPath = Path2.of(".baz");

    assertEquals(1L, jedis.jsonClear("qux", objPath));
    assertJsonArrayEquals(jsonArray(new JSONObject()), jedis.jsonGet("qux", objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jedis.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L), jedis.jsonArrAppend("test_arrappend", Path2.of(".b"), 4, 5, 6));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, 4, 5, 6)),
      jedis.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypes() {
    Object fooObject = gson.toJson("foo");
    Object trueObject = gson.toJson(true);
    Object nullObject = gson.toJson(null);
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jedis.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(6L),
      jedis.jsonArrAppend("test_arrappend", Path2.of(".b"), fooObject, trueObject, nullObject));

    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3, "foo", true, null)),
      jedis.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jedis.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(4L),
      jedis.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "foo", true, null));

    assertJsonArrayEquals(jsonArray(jsonArray("ello", "foo", true, null)),
      jedis.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    jedis.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(3L),
      jedis.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "a", "b", "c"));

    assertJsonArrayEquals(jsonArray(jsonArray("a", "b", "c")),
      jedis.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    jedis.jsonSet("test_arrappend", ROOT_PATH, new JSONObject(json));
    assertEquals(singletonList(null), jedis.jsonArrAppend("test_arrappend", Path2.of(".a"), 1));
    assertEquals(singletonList(null),
      jedis.jsonArrAppend("test_arrappend", Path2.of(".a"), gson.toJson(1)));
    assertEquals(singletonList(null),
      jedis.jsonArrAppendWithEscape("test_arrappend", Path2.of(".a"), 1));
  }

  @Test
  public void arrIndexAbsentKey() {
    assertThrows(JedisDataException.class,
      () -> jedis.jsonArrIndexWithEscape("quxquux", ROOT_PATH, new JSONObject()));
  }

  @Test
  public void arrIndexWithInts() {
    jedis.jsonSetWithEscape("quxquux", ROOT_PATH, new int[] { 8, 6, 7, 5, 3, 0, 9 });
    assertEquals(singletonList(2L), jedis.jsonArrIndexWithEscape("quxquux", ROOT_PATH, 7));
    assertEquals(singletonList(-1L), jedis.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    jedis.jsonSetWithEscape("quxquux", ROOT_PATH,
      new String[] { "8", "6", "7", "5", "3", "0", "9" });
    assertEquals(singletonList(2L), jedis.jsonArrIndexWithEscape("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    jedis.jsonSetWithEscape("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(singletonList(1L),
      jedis.jsonArrIndexWithEscape("foobar", Path2.of(".fooArr"), "b"));
  }

  @Test
  public void arrIndexNonExistentPath() {
    jedis.jsonSet("foobar", ROOT_PATH, gson.toJson(new FooBarObject()));
    assertEquals(Collections.emptyList(),
      jedis.jsonArrIndex("foobar", Path2.of(".barArr"), gson.toJson("x")));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jedis.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L),
      jedis.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, 1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "foo", "world", true, 1, 3, null, false)),
      jedis.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    jedis.jsonSet("test_arrinsert", ROOT_PATH, new JSONArray(json));
    assertEquals(singletonList(8L),
      jedis.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, -1, "foo"));

    assertJsonArrayEquals(jsonArray(jsonArray("hello", "world", true, 1, 3, null, "foo", false)),
      jedis.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrPop() {
    jedis.jsonSet("arr", ROOT_PATH, new JSONArray(new int[] { 0, 1, 2, 3, 4 }));
    assertEquals(singletonList(4d), jedis.jsonArrPop("arr", ROOT_PATH));
    assertEquals(singletonList(3d), jedis.jsonArrPop("arr", ROOT_PATH, -1));
    assertEquals(singletonList(0d), jedis.jsonArrPop("arr", ROOT_PATH, 0));
  }

  @Test
  public void arrTrim() {
    jedis.jsonSet("arr", ROOT_PATH, new JSONArray(new int[] { 0, 1, 2, 3, 4 }));
    assertEquals(singletonList(3L), jedis.jsonArrTrim("arr", ROOT_PATH, 1, 3));
    assertJsonArrayEquals(jsonArray(jsonArray(1, 2, 3)), jedis.jsonGet("arr", ROOT_PATH));
  }

  @Test
  public void strAppend() {
    jedis.jsonSet("str", ROOT_PATH, gson.toJson("foo"));
    assertEquals(singletonList(6L), jedis.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertJsonArrayEquals(jsonArray("foobar"), jedis.jsonGet("str", ROOT_PATH));
  }

  @Test
  public void strLen() {
    jedis.jsonSetWithEscape("str", "foobar");
    assertEquals(singletonList(6L), jedis.jsonStrLen("str", ROOT_PATH));
  }

  @Test
  public void numIncrBy() {
    assumeFalse(RedisProtocol.isResp3(protocol));
    jedis.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertJsonArrayEquals(jsonArray((Object) null), jedis.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertJsonArrayEquals(jsonArray(null, 4, 7, null),
      jedis.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertJsonArrayEquals(jsonArray((Object) null),
      jedis.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertJsonArrayEquals(jsonArray(), jedis.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void numIncrByResp3() {
    assumeTrue(protocol == RedisProtocol.RESP3);
    jedis.jsonSet("doc", "{\"a\":\"b\",\"b\":[{\"a\":2}, {\"a\":5}, {\"a\":\"c\"}]}");
    assertEquals(singletonList((Object) null), jedis.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertEquals(Arrays.asList(null, 4d, 7d, null),
      jedis.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertEquals(singletonList((Object) null), jedis.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertEquals(Collections.emptyList(), jedis.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
  }

  @Test
  public void numIncrByWithIntegerIncrementReturnsLong() {
    assumeTrue(protocol == RedisProtocol.RESP3);
    // Set an integer value
    jedis.jsonSet("numtest", ROOT_PATH, 10);

    // Increment by an integer - should return Long
    Object result = jedis.jsonNumIncrBy("numtest", ROOT_PATH, 5);
    assertInstanceOf(List.class, result, "Result should be a List");
    List<?> resultList = (List<?>) result;
    assertEquals(1, resultList.size());
    assertInstanceOf(Long.class, resultList.get(0),
      "Result should be Long but was " + resultList.get(0).getClass().getName());
    assertEquals(15L, resultList.get(0));
  }

  @Test
  public void numIncrByWithDoubleIncrementReturnsDouble() {
    assumeTrue(protocol == RedisProtocol.RESP3);
    // Set an integer value
    jedis.jsonSet("numtest", ROOT_PATH, 10);

    // Increment by a double - should return Double
    Object result = jedis.jsonNumIncrBy("numtest", ROOT_PATH, 2.5);
    assertInstanceOf(List.class, result, "Result should be a List");
    List<?> resultList = (List<?>) result;
    assertEquals(1, resultList.size());
    assertInstanceOf(Double.class, resultList.get(0),
      "Result should be Double but was " + resultList.get(0).getClass().getName());
    assertEquals(12.5, resultList.get(0));
  }

  @Test
  public void numIncrByPreservesLongForIntegerOperations() {
    assumeTrue(protocol == RedisProtocol.RESP3);
    // Set multiple integer values
    jedis.jsonSet("prices", ROOT_PATH, "{\"price1\":100,\"price2\":200,\"price3\":300}");

    // Increment all prices by 10 (integer)
    Object result = jedis.jsonNumIncrBy("prices", Path2.of("$.*"), 10);
    assertInstanceOf(List.class, result, "Result should be a List");
    List<?> resultList = (List<?>) result;
    assertEquals(3, resultList.size());

    // All results should be Long since we incremented integers by integers
    for (Object item : resultList) {
      assertInstanceOf(Long.class, item,
        "Result items should be Long but was " + item.getClass().getName());
    }
    assertTrue(resultList.contains(110L));
    assertTrue(resultList.contains(210L));
    assertTrue(resultList.contains(310L));
  }

  @Test
  public void numIncrByConvertsToDoubleWhenNeeded() {
    assumeTrue(protocol == RedisProtocol.RESP3);
    // Set an integer value
    jedis.jsonSet("numtest", ROOT_PATH, 100);

    // First increment by integer - should stay Long
    Object result1 = jedis.jsonNumIncrBy("numtest", ROOT_PATH, 50);
    List<?> resultList1 = (List<?>) result1;
    assertInstanceOf(Long.class, resultList1.get(0), "Should be Long after integer increment");
    assertEquals(150L, resultList1.get(0));

    // Now increment by double - should convert to Double
    Object result2 = jedis.jsonNumIncrBy("numtest", ROOT_PATH, 0.5);
    List<?> resultList2 = (List<?>) result2;
    assertInstanceOf(Double.class, resultList2.get(0), "Should be Double after decimal increment");
    assertEquals(150.5, resultList2.get(0));

    // Further increments should still be Double even if incrementing by integer-like value
    Object result3 = jedis.jsonNumIncrBy("numtest", ROOT_PATH, 1.0);
    List<?> resultList3 = (List<?>) result3;
    assertInstanceOf(Double.class, resultList3.get(0), "Should remain Double");
    assertEquals(151.5, resultList3.get(0));
  }

  @Test
  public void obj() {
    String json = "{\"a\":[3], \"nested\": {\"a\": {\"b\":2, \"c\": 1}}}";
    jedis.jsonSet("doc", ROOT_PATH, json);
    assertEquals(Arrays.asList(2L), jedis.jsonObjLen("doc", ROOT_PATH));
    assertEquals(Arrays.asList(Arrays.asList("a", "nested")), jedis.jsonObjKeys("doc", ROOT_PATH));
    assertEquals(Arrays.asList(null, 2L), jedis.jsonObjLen("doc", Path2.of("..a")));
    assertEquals(Arrays.asList(null, Arrays.asList("b", "c")),
      jedis.jsonObjKeys("doc", Path2.of("..a")));
  }

  @Test
  public void debugMemory() {
    assertEquals(Collections.emptyList(), jedis.jsonDebugMemory("json", ROOT_PATH));

    jedis.jsonSet("json", new JSONObject("{ foo: 'bar', bar: { foo: 10 }}"));
    assertEquals(1, jedis.jsonDebugMemory("json", ROOT_PATH).size());
    assertEquals(2, jedis.jsonDebugMemory("json", Path2.of("$..foo")).size());
    assertEquals(1, jedis.jsonDebugMemory("json", Path2.of("$..bar")).size());
  }

  // Helper methods for JSON assertions
  protected void assertJsonArrayEquals(JSONArray a, Object _b) {
    if (!(_b instanceof JSONArray)) {
      fail("Actual value is not JSONArray.");
    }
    JSONArray b = (JSONArray) _b;
    assertEquals(a.length(), b.length(), "JSONArray length mismatch");
    int length = a.length();
    for (int index = 0; index < length; index++) {
      if (a.isNull(index)) {
        assertTrue(b.isNull(index), index + "'th element is not null");
        continue;
      }
      Object ia = a.get(index);
      Object ib = b.get(index);
      if (ia instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) ia, ib);
      } else if (ia instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) ia, ib);
      } else if (ia instanceof Number && ib instanceof Number) {
        assertEquals(((Number) ia).doubleValue(), ((Number) ib).doubleValue(), 0d,
          index + "'th element mismatch");
      } else {
        assertEquals(ia, ib, index + "'th element mismatch");
      }
    }
  }

  protected void assertJsonObjectEquals(JSONObject a, Object _b) {
    if (!(_b instanceof JSONObject)) {
      fail("Actual value is not JSONObject.");
    }
    JSONObject b = (JSONObject) _b;
    assertEquals(a.length(), b.length(), "JSONObject length mismatch");
    assertEquals(a.keySet(), b.keySet());
    for (String key : a.keySet()) {
      if (a.isNull(key)) {
        assertTrue(b.isNull(key), key + "'s value is not null");
        continue;
      }
      Object oa = a.get(key);
      Object ob = b.get(key);
      if (oa instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) oa, ob);
      } else if (oa instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) oa, ob);
      } else {
        assertEquals(oa, ob, key + "'s value mismatch");
      }
    }
  }

  protected static JSONArray jsonArray(Object... objects) {
    JSONArray arr = new JSONArray();
    for (Object o : objects) {
      arr.put(o);
    }
    return arr;
  }
}
