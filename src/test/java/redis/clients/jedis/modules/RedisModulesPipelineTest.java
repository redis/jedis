package redis.clients.jedis.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static redis.clients.jedis.json.Path.ROOT_PATH;
import static redis.clients.jedis.modules.json.JsonObjects.Baz;
import static redis.clients.jedis.modules.json.JsonObjects.IRLObject;
import static redis.clients.jedis.search.RediSearchUtil.toStringMap;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;

@RunWith(Parameterized.class)
public class RedisModulesPipelineTest extends RedisModuleCommandsTestBase {

  private static final Gson gson = new Gson();

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public RedisModulesPipelineTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void search() {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);
    String index = "testindex";

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");

//    Connection c = createConnection();
//    Pipeline p = new Pipeline(c);
    Pipeline p = (Pipeline) client.pipelined();

    Response<String> create = p.ftCreate(index, IndexOptions.defaultOptions(), sc);
    Response<String> alter = p.ftAlter(index, new Schema().addTextField("foo", 1.0));
    p.hset("doc1", toStringMap(fields));
    p.hset("doc2", toStringMap(fields));
    Response<SearchResult> searchResult = p.ftSearch(index, new Query("hello world"));
//    Response<SearchResult> searchBytesResult = p.ftSearch(index.getBytes(), new Query("hello world")); // not RESP3 supported
    Response<AggregationResult> aggregateResult = p.ftAggregate(index, new AggregationBuilder().groupBy("@title"));
    Response<String> explain = p.ftExplain(index, new Query("@title:title_val"));
    Response<List<String>> explainCLI = p.ftExplainCLI(index, new Query("@title:title_val"));
    Response<Map<String, Object>> info = p.ftInfo(index);
//    // @org.junit.Ignore
//    Response<String> configSet = p.ftConfigSet("timeout", "100");
//    Response<Map<String, Object>> configGet = p.ftConfigGet("*");
//    Response<String> configSetIndex = p.ftConfigSet(index, "timeout", "100");
//    Response<Map<String, Object>> configGetIndex = p.ftConfigGet(index, "*");
    Response<String> synUpdate = p.ftSynUpdate(index, "foo", "bar");
    Response<Map<String, List<String>>> synDump = p.ftSynDump(index);

    p.sync();
//    c.close();

    assertEquals("OK", create.get());
    assertEquals("OK", alter.get());
    assertEquals("OK", alter.get());
    assertEquals(2, searchResult.get().getTotalResults());
//    assertEquals(2, searchBytesResult.get().getTotalResults());
    assertEquals(1, aggregateResult.get().getTotalResults());
    assertNotNull(explain.get());
    assertNotNull(explainCLI.get().get(0));
    assertEquals(index, info.get().get("index_name"));
//    // @org.junit.Ignore
//    assertEquals("OK", configSet.get());
//    assertEquals("100", configGet.get().get("TIMEOUT"));
//    assertEquals("OK", configSetIndex.get());
//    assertEquals("100", configGetIndex.get().get("TIMEOUT"));
    assertEquals("OK", synUpdate.get());
    Map<String, List<String>> expected = new HashMap<>();
    expected.put("bar", Collections.singletonList("foo"));
    assertEquals(expected, synDump.get());
  }

  @Test
  public void jsonV1() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3);

    Map<String, String> hm1 = new HashMap<>();
    hm1.put("hello", "world");
    hm1.put("oh", "snap");

    Map<String, Object> hm2 = new HashMap<>();
    hm2.put("array", new String[]{"a", "b", "c"});
    hm2.put("boolean", true);
    hm2.put("number", 3);

    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Baz baz3 = new Baz("quuz3", "grault3", "waldo3");

//    Connection c = createConnection();
//    Pipeline p = new Pipeline(c);
    Pipeline p = (Pipeline) client.pipelined();

    Response<String> set1 = p.jsonSet("foo", Path.ROOT_PATH, hm1);
    Response<Object> get = p.jsonGet("foo");
    Response<Map> getObject = p.jsonGet("foo", Map.class);
    Response<Object> getWithPath = p.jsonGet("foo", Path.ROOT_PATH);
    Response<Map> getObjectWithPath = p.jsonGet("foo", Map.class, Path.ROOT_PATH);
    Response<List<JSONArray>> mget = p.jsonMGet("foo");
    p.jsonSet("baz", new JSONObject(gson.toJson(baz1)));
    Response<List<Baz>> mgetClass = p.jsonMGet(Path.ROOT_PATH, Baz.class, "baz");
    Response<Long> strLenPath = p.jsonStrLen("foo", new Path("hello"));
    Response<Long> strAppPath = p.jsonStrAppend("foo", new Path("hello"), "!");
    Response<Long> delPath = p.jsonDel("foo", new Path("hello"));
    Response<Long> delKey = p.jsonDel("foo");
    Response<String> set2 = p.jsonSet("foo", Path.ROOT_PATH, hm2, new JsonSetParams().nx());
    Response<Object> popPath = p.jsonArrPop("foo", new Path("array"));
    Response<Object> indexPop = p.jsonArrPop("foo", new Path("array"), 2);
    Response<Long> append = p.jsonArrAppend("foo", new Path("array"), "b", "c", "d");
    Response<Long> index = p.jsonArrIndex("foo", new Path("array"), "c");
    Response<Long> insert = p.jsonArrInsert("foo", new Path("array"), 0, "x");
    Response<Long> arrLenWithPath = p.jsonArrLen("foo", new Path("array"));
    Response<Long> trim = p.jsonArrTrim("foo", new Path("array"), 1, 4);
    Response<String> toggle = p.jsonToggle("foo", new Path("boolean"));
    Response<Class<?>> type = p.jsonType("foo", new Path("boolean"));
    Response<Class<?>> keyType = p.jsonType("foo");
    Response<Long> clearPath = p.jsonClear("foo", new Path("boolean"));
    Response<Long> clearKey = p.jsonClear("foo");
    Response<String> set3 = p.jsonSet("foo", Path.ROOT_PATH, "newStr");
    Response<Long> strLen = p.jsonStrLen("foo");
    Response<Long> strApp = p.jsonStrAppend("foo", "?");
    Response<String> set4 = p.jsonSetWithEscape("obj", new IRLObject());
    p.jsonSet("arr", ROOT_PATH, new int[]{ 0, 1, 2, 3 });
    Response<Object> pop = p.jsonArrPop("arr");
    Response<Long> arrLen = p.jsonArrLen("arr");
    p.jsonSet("baz", ROOT_PATH, new Baz[]{ baz1, baz2, baz3 });
    Response<Baz> popClass = p.jsonArrPop("baz", Baz.class);
    Response<Baz> popClassWithPath = p.jsonArrPop("baz", Baz.class, Path.ROOT_PATH);
    Response<Baz> popClassWithIndex = p.jsonArrPop("baz", Baz.class, Path.ROOT_PATH, 0);

    p.sync();
//    c.close();

    assertEquals("OK", set1.get());
    assertEquals(hm1, get.get());
    assertEquals(hm1, getObject.get());
    assertEquals(hm1, getWithPath.get());
    assertEquals(hm1, getObjectWithPath.get());
    assertEquals(1, mget.get().size());
    assertEquals(baz1, mgetClass.get().get(0));
    assertEquals(Long.valueOf(5), strLenPath.get());
    assertEquals(Long.valueOf(6), strAppPath.get());
    assertEquals(Long.valueOf(1), delPath.get());
    assertEquals(Long.valueOf(1), delKey.get());
    assertEquals("OK", set2.get());
    assertEquals("c", popPath.get());
    assertEquals("b", indexPop.get());
    assertEquals(Long.valueOf(4), append.get());
    assertEquals(Long.valueOf(2), index.get());
    assertEquals(Long.valueOf(5), insert.get());
    assertEquals(Long.valueOf(5), arrLenWithPath.get());
    assertEquals(Long.valueOf(4), trim.get());
    assertEquals("false", toggle.get());
    assertEquals(boolean.class, type.get());
    assertEquals(Object.class, keyType.get());
    assertEquals(Long.valueOf(0), clearPath.get());
    assertEquals(Long.valueOf(1), clearKey.get());
    assertEquals("OK", set3.get());
    assertEquals(Long.valueOf(6), strLen.get());
    assertEquals(Long.valueOf(7), strApp.get());
    assertEquals("OK", set4.get());
    assertEquals(3.0, pop.get());
    assertEquals(Long.valueOf(3), arrLen.get());
    assertEquals(baz3, popClass.get());
    assertEquals(baz2, popClassWithPath.get());
    assertEquals(baz1, popClassWithIndex.get());
  }

  @Test
  public void jsonV2() {
    Map<String, String> hm1 = new HashMap<>();
    hm1.put("hello", "world");
    hm1.put("oh", "snap");

    Map<String, Object> hm2 = new HashMap<>();
    hm2.put("array", new String[]{"a", "b", "c"});
    hm2.put("boolean", true);
    hm2.put("number", 3);

//    Connection c = createConnection();
//    Pipeline p = new Pipeline(c);
    Pipeline p = (Pipeline) client.pipelined();

    Response<String> setWithEscape = p.jsonSetWithEscape("foo", Path2.ROOT_PATH, hm1);
    Response<Object> get = p.jsonGet("foo",  Path2.ROOT_PATH);
    Response<List<JSONArray>> mget = p.jsonMGet(Path2.ROOT_PATH, "foo");
    Response<List<Long>> strLen = p.jsonStrLen("foo", new Path2("hello"));
    Response<List<Long>> strApp = p.jsonStrAppend("foo", new Path2("hello"), "!");
    Response<Long> del = p.jsonDel("foo", new Path2("hello"));
    Response<String> set = p.jsonSet("bar", Path2.ROOT_PATH, gson.toJson("strung"));
    Response<String> setWithParams = p.jsonSet("foo", Path2.ROOT_PATH, gson.toJson(hm2), new JsonSetParams().xx());
    Response<String> setWithEscapeWithParams = p.jsonSetWithEscape("foo", Path2.ROOT_PATH, hm2, new JsonSetParams().xx());
    Response<List<Object>> pop = p.jsonArrPop("foo", new Path2("array"));
    Response<List<Object>> popWithIndex = p.jsonArrPop("foo", new Path2("array"), 2);
    Response<List<Long>> append = p.jsonArrAppend("foo", Path2.of("$.array"), gson.toJson("b"), gson.toJson("d"));
    Response<List<Long>> appendWithEscape = p.jsonArrAppendWithEscape("foo", Path2.of("$.array"), "e");
    Response<List<Long>> index = p.jsonArrIndex("foo", Path2.of("$.array"), gson.toJson("b"));
    Response<List<Long>> indexWithEscape = p.jsonArrIndexWithEscape("foo", Path2.of("$.array"), "b");
    Response<List<Long>> insert = p.jsonArrInsert("foo", new Path2("array"), 0, gson.toJson("x"));
    Response<List<Long>> insertWithEscape = p.jsonArrInsertWithEscape("foo", new Path2("array"), 0, "x");
    Response<List<Long>> arrLen = p.jsonArrLen("foo", new Path2("array"));
    Response<List<Long>> trim = p.jsonArrTrim("foo", new Path2("array"), 1, 2);
    Response<List<Boolean>> toggle = p.jsonToggle("foo", new Path2("boolean"));
    Response<List<Class<?>>> type = p.jsonType("foo", new Path2("boolean"));
    Response<Long> clear = p.jsonClear("foo", new Path2("array"));

    p.sync();
//    c.close();

    assertEquals("OK", setWithEscape.get());
    assertNotNull(get.get());
    assertEquals(1, mget.get().size());
    assertEquals(Long.valueOf(5), strLen.get().get(0));
    assertEquals(1, strApp.get().size());
    assertEquals(Long.valueOf(1), del.get());
    assertEquals("OK", set.get());
    assertEquals("OK", setWithParams.get());
    assertEquals("OK", setWithEscapeWithParams.get());
    assertEquals("c", pop.get().get(0));
    assertEquals("b", popWithIndex.get().get(0));
    assertEquals(Long.valueOf(3), append.get().get(0));
    assertEquals(Long.valueOf(4), appendWithEscape.get().get(0));
    assertEquals(Long.valueOf(1), index.get().get(0));
    assertEquals(Long.valueOf(1), indexWithEscape.get().get(0));
    assertEquals(Long.valueOf(5), insert.get().get(0));
    assertEquals(Long.valueOf(6), insertWithEscape.get().get(0));
    assertEquals(Long.valueOf(6), arrLen.get().get(0));
    assertEquals(Long.valueOf(2), trim.get().get(0));
    assertEquals(false, toggle.get().get(0));
    assertEquals(boolean.class, type.get().get(0));
    assertEquals(Long.valueOf(1), clear.get());
  }
}
