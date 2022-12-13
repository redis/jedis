package redis.clients.jedis.modules.json;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path2.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.*;
import static redis.clients.jedis.modules.json.JsonUtils.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

  @Test
  public void basicSetGetShouldSucceed() throws IOException {

    // naive set with a path
    client.jsonSetWithEscape("null", ROOT_PATH, (Object) null);
    assertEquals(jsonArray((Object) null), client.jsonGet("null", ROOT_PATH));

    // real scalar value and no path
    client.jsonSetWithEscape("str", "strong");
    assertEquals("strong", client.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    client.jsonSetWithEscape("obj", obj);
    Object expected = readJson(writeJson(obj), Object.class);
    assertEquals(expected, client.jsonGet("obj"));

    // check an update
    Path2 p = Path2.of(".str");
    client.jsonSet("obj", p, writeJson("strung"));
    assertEquals(jsonArray("strung"), client.jsonGet("obj", p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    client.jsonSetWithEscape("obj", new IRLObject());
    Path2 p = Path2.of(".str");
    client.jsonSetWithEscape("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals(jsonArray("strangle"), client.jsonGet("obj", p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() throws JsonProcessingException {
    client.jsonSet("obj", writeJson(new IRLObject()));
    Path2 p = Path2.of(".none");
    client.jsonSet("obj", p, writeJson("strangle"), JsonSetParams.jsonSetParams().nx());
    assertEquals(jsonArray("strangle"), client.jsonGet("obj", p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() throws JsonProcessingException {
    client.jsonSet("obj1", writeJson(new IRLObject()));
    client.jsonSetWithEscape("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals(jsonArray("strangle"), client.jsonGet("obj1", ROOT_PATH));
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
    ObjectNode result = (ObjectNode) client.jsonGet("obj", Path2.of("bool"), Path2.of("str"));
    assertEquals(jsonArray(true), result.get("$.bool"));
    assertEquals(jsonArray("string"), result.get("$.str"));
  }

  @Test
  public void getMultiLevels() {
    ObjectNode obj = JACKSON.createObjectNode();
    obj.put("foo", "John");
    ObjectNode inner = JACKSON.createObjectNode();
    inner.put("foo", "Jane");
    obj.put("bar", inner);
    client.jsonSet("multi", obj);
    assertEquals(jsonArray("John", "Jane"), client.jsonGet("multi", new Path2("..foo")));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    client.jsonSetWithEscape("obj", obj);

    Path2 pbool = Path2.of(".bool");
    // check initial value
    assertEquals(jsonArray(true), client.jsonGet("obj", pbool));

    // true -> false
    client.jsonToggle("obj", pbool);
    assertEquals(jsonArray(false), client.jsonGet("obj", pbool));

    // false -> true
    client.jsonToggle("obj", pbool);
    assertEquals(jsonArray(true), client.jsonGet("obj", pbool));

    // ignore non-boolean field
    Path2 pstr = Path2.of(".str");
    assertEquals(singletonList(null), client.jsonToggle("obj", pstr));
    assertEquals(jsonArray("string"), client.jsonGet("obj", pstr));
  }

  @Test
  public void getAbsent() {
    client.jsonSetWithEscape("test", ROOT_PATH, "foo");
    assertEquals(jsonArray(), client.jsonGet("test", Path2.of(".bar")));
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
  public void typeChecksShouldSucceed() throws JsonProcessingException {
    client.jsonSet("foobar", ROOT_PATH, writeJson(new FooBarObject()));
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
  public void mgetWithPathWithAllKeysExist() throws JsonProcessingException {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    client.jsonSet("qux1", writeJson(qux1));
    client.jsonSet("qux2", writeJson(qux2));

    List<ArrayNode> list = client.jsonMGet(Path2.of("baz"), "qux1", "qux2");
    assertEquals(2, list.size());
    assertEquals(jsonArray(readJson(baz1)), list.get(0));
    assertEquals(jsonArray(readJson(baz2)), list.get(1));
  }

  @Test
  public void mgetAtRootPathWithMissingKeys() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    client.jsonSetWithEscape("qux1", qux1);
    client.jsonSetWithEscape("qux2", qux2);

    List<ArrayNode> list = client.jsonMGet("qux1", "qux2", "qux3");

    assertEquals(3, list.size());
    assertNull(list.get(2));
    list.removeAll(singletonList(null));
    assertEquals(2, list.size());
  }

  @Test
  public void arrLen() {
    client.jsonSet("arr", ROOT_PATH, jsonArray((Object[]) new Integer[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(5L), client.jsonArrLen("arr", ROOT_PATH));
  }

  @Test
  public void clearArray() throws JsonProcessingException {
    client.jsonSet("foobar", ROOT_PATH, writeJson(new FooBarObject()));

    Path2 arrPath = Path2.of(".fooArr");
    assertEquals(singletonList(3L), client.jsonArrLen("foobar", arrPath));

    assertEquals(1L, client.jsonClear("foobar", arrPath));
    assertEquals(singletonList(0L), client.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path2 strPath = Path2.of(".foo");
    assertEquals(0L, client.jsonClear("foobar", strPath));
    assertEquals(jsonArray("bar"), client.jsonGet("foobar", strPath));
  }

  @Test
  public void clearObject() throws JsonProcessingException {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    client.jsonSet("qux", writeJson(qux));
    Path2 objPath = Path2.of(".baz");
//    assertEquals(baz, client.jsonGet("qux", objPath));

    assertEquals(1L, client.jsonClear("qux", objPath));
//    assertEquals(new Baz(null, null, null), client.jsonGet("qux", objPath));
    assertEquals(jsonArray(emptyJson()), client.jsonGet("qux", objPath));
  }

  @Test
  public void arrAppendSameType() throws JsonProcessingException {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, readJson(json));
    assertEquals(singletonList(6L), client.jsonArrAppend("test_arrappend", Path2.of(".b"), 4, 5, 6));

    assertEquals(jsonArray(jsonArray(1, 2, 3, 4, 5, 6)), client.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypes() throws JsonProcessingException {
    Object fooObject = writeJson("foo");
    Object trueObject = writeJson(true);
    Object nullObject = writeJson(null);
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, readJson(json));
    assertEquals(singletonList(6L), client.jsonArrAppend("test_arrappend", Path2.of(".b"), fooObject, trueObject, nullObject));

    assertEquals(jsonArray(jsonArray(1, 2, 3, "foo", true, null)), client.jsonGet("test_arrappend", Path2.of(".b")));
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() throws JsonProcessingException {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, readJson(json));
    assertEquals(singletonList(4L), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "foo", true, null));

    assertEquals(jsonArray(jsonArray("ello", "foo", true, null)), client.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendAgaintsEmptyArray() throws JsonProcessingException {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, readJson(json));
    assertEquals(singletonList(3L), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".c.d"), "a", "b", "c"));

    assertEquals(jsonArray(jsonArray("a", "b", "c")), client.jsonGet("test_arrappend", Path2.of(".c.d")));
  }

  @Test
  public void arrAppendPathIsNotArray() throws JsonProcessingException {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    client.jsonSet("test_arrappend", ROOT_PATH, readJson(json));
    assertEquals(singletonList(null), client.jsonArrAppend("test_arrappend", Path2.of(".a"), 1));
    assertEquals(singletonList(null), client.jsonArrAppend("test_arrappend", Path2.of(".a"), writeJson(1)));
    assertEquals(singletonList(null), client.jsonArrAppendWithEscape("test_arrappend", Path2.of(".a"), 1));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    client.jsonArrIndexWithEscape("quxquux", ROOT_PATH, emptyJson());
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
  public void arrIndexNonExistentPath() throws JsonProcessingException {
    client.jsonSet("foobar", ROOT_PATH, writeJson(new FooBarObject()));
    assertEquals(emptyList(), client.jsonArrIndex("foobar", Path2.of(".barArr"), writeJson("x")));
  }

  @Test
  public void arrInsert() throws JsonProcessingException {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    client.jsonSet("test_arrinsert", ROOT_PATH, readJson(json));
    assertEquals(singletonList(8L), client.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, 1, "foo"));

    assertEquals(jsonArray(jsonArray("hello", "foo", "world", true, 1, 3, null, false)),
        client.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrInsertWithNegativeIndex() throws JsonProcessingException {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    client.jsonSet("test_arrinsert", ROOT_PATH, readJson(json));
    assertEquals(singletonList(8L), client.jsonArrInsertWithEscape("test_arrinsert", ROOT_PATH, -1, "foo"));

    assertEquals(jsonArray(jsonArray("hello", "world", true, 1, 3, null, "foo", false)),
        client.jsonGet("test_arrinsert", ROOT_PATH));
  }

  @Test
  public void arrPop() {
    client.jsonSet("arr", ROOT_PATH, jsonArray((Object[]) new Integer[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(4), client.jsonArrPop("arr", ROOT_PATH));
    assertEquals(singletonList(3), client.jsonArrPop("arr", ROOT_PATH, -1));
    assertEquals(singletonList(0), client.jsonArrPop("arr", ROOT_PATH, 0));
  }

  @Test
  public void arrTrim() {
//    client.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    client.jsonSet("arr", ROOT_PATH, jsonArray((Object[]) new Integer[]{0, 1, 2, 3, 4}));
    assertEquals(singletonList(3L), client.jsonArrTrim("arr", ROOT_PATH, 1, 3));
//    assertArrayEquals(new Integer[]{1, 2, 3}, client.jsonGet("arr", Integer[].class, ROOT_PATH));
    assertEquals(jsonArray(jsonArray(1, 2, 3)), client.jsonGet("arr", ROOT_PATH));
  }

  @Test
  public void strAppend() throws JsonProcessingException {
//    client.jsonSet("str", ROOT_PATH, "foo");
    client.jsonSet("str", ROOT_PATH, writeJson("foo"));
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
    assertEquals(jsonArray((Object) null), client.jsonNumIncrBy("doc", Path2.of(".a"), 1d));
    assertEquals(jsonArray(null, 4.0, 7.0, null), client.jsonNumIncrBy("doc", Path2.of("..a"), 2d));
    assertEquals(jsonArray((Object) null), client.jsonNumIncrBy("doc", Path2.of("..b"), 0d));
    assertEquals(jsonArray(), client.jsonNumIncrBy("doc", Path2.of("..c"), 0d));
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
  public void debugMemory() throws JsonProcessingException {
    assertEquals(emptyList(), client.jsonDebugMemory("json", ROOT_PATH));

    client.jsonSet("json", readJson("{ foo: 'bar', bar: { foo: 10 }}"));
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

  private ArrayNode jsonArray(Object... objects) {
    ArrayNode arr = JACKSON.createArrayNode();
    for (Object o : objects) {
      if (o == null) {
        arr.addNull();
      } else if (o instanceof Boolean) {
        arr.add((Boolean) o);
      } else if (o instanceof Integer) {
        arr.add((Integer) o);
      } else if (o instanceof Double) {
        arr.add((Double) o);
      } else if (o instanceof String) {
        arr.add((String) o);
      } else if (o instanceof ObjectNode) {
        arr.add((ObjectNode) o);
      } else if (o instanceof ArrayNode) {
        arr.add((ArrayNode) o);
      } else {
        throw new IllegalArgumentException("" + o);
      }
    }
    return arr;
  }
}
