package redis.clients.jedis.modules;

import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path.ROOT_PATH;
import static redis.clients.jedis.search.RediSearchUtil.toStringMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;

public class RedisModulesPipelineTest extends RedisModuleCommandsTestBase {
  private static class IRLObject {

    public String str;
    public boolean bool;

    public IRLObject() {
      this.str = "string";
      this.bool = true;
    }
  }

  private static final Gson gson = new Gson();


  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  @Test
  public void search() {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);
    String index = "testindex";

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");

    Connection c = createConnection();
    Pipeline p = new Pipeline(c);

    Response<String> create = p.ftCreate(index, IndexOptions.defaultOptions(), sc);
    Response<String> alter = p.ftAlter(index, new Schema.TagField("tags", ","));
    Response<String> alter_sc = p.ftAlter(index, new Schema().addTextField("foo", 1.0));
    for (int i = 0; i < 100; i++) {
      p.hset(String.format("doc%d", i), toStringMap(fields));
    }
    Response<SearchResult> searchResultResponse = p.ftSearch(index,
        new Query("hello world").limit(0, 5).setWithScores());
    Response<Long> delResponse = p.del("doc0");
    Response<SearchResult> searchResultResponse2 = p.ftSearch(index, new Query("hello world"));
    Response<String> explain = p.ftExplain(index, new Query("@title:title_val"));
    Response<Map<String, Object>> info = p.ftInfo(index);
    Response<String> aliasAdd = p.ftAliasAdd("ALIAS1", index);
    Response<String> aliasUpdate = p.ftAliasUpdate("ALIAS2", index);
    Response<String> aliasDel = p.ftAliasDel("ALIAS2");
    Response<String> configSet = p.ftConfigSet("timeout", "100");
    Response<Map<String, String>> configGet = p.ftConfigGet("*");
    Response<String> configSetIndex = p.ftConfigSet(index, "timeout", "100");
    Response<Map<String, String>> configGetIndex = p.ftConfigGet(index, "*");
    Response<String> dropIndexResponse = p.ftDropIndex(index);

    p.sync();
    c.close();

    assertEquals("OK", create.get());
    assertEquals("OK", alter.get());
    assertEquals("OK", alter_sc.get());
    assertEquals(100, searchResultResponse.get().getTotalResults());
    assertEquals(5, searchResultResponse.get().getDocuments().size());
    for (Document d : searchResultResponse.get().getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }
    assertEquals(Long.valueOf(1), delResponse.get());
    assertEquals(99, searchResultResponse2.get().getTotalResults());
    assertNotNull(explain.get());
    assertEquals(index, info.get().get("index_name"));
    assertEquals("OK", aliasAdd.get());
    assertEquals("OK", aliasUpdate.get());
    assertEquals("OK", aliasDel.get());
    assertEquals("OK", configSet.get());
    assertEquals("100", configGet.get().get("TIMEOUT"));
    assertEquals("OK", configSetIndex.get());
    assertEquals("100", configGetIndex.get().get("TIMEOUT"));
    assertEquals("OK", dropIndexResponse.get());
  }

  @Test
  public void jsonV1() {
    Map<String, String> hm1 = new HashMap<>();
    hm1.put("hello", "world");
    hm1.put("oh", "snap");

    Map<String, Object> hm2 = new HashMap<>();
    hm2.put("array", new String[]{"a", "b", "c"});
    hm2.put("boolean", true);
    hm2.put("number", 3);

    Connection c = createConnection();
    Pipeline p = new Pipeline(c);

    Response<String> set1 = p.jsonSet("foo", Path.ROOT_PATH, hm1);
    Response<Object> object = p.jsonGet("foo");
    Response<List<JSONArray>> mget = p.jsonMGet("foo");
    Response<Long> strLenPath = p.jsonStrLen("foo", new Path("hello"));
    Response<Long> strAppPath = p.jsonStrAppend("foo", new Path("hello"), "!");
    Response<Long> delPath = p.jsonDel("foo", new Path("hello"));
    Response<Long> delKey = p.jsonDel("foo");
    Response<String> set2 = p.jsonSet("foo", Path.ROOT_PATH, hm2, new JsonSetParams().nx());
    Response<Object> popPath1 = p.jsonArrPop("foo", new Path("array"));
    Response<Long> append = p.jsonArrAppend("foo", new Path("array"), "c", "d");
    Response<Long> index = p.jsonArrIndex("foo", new Path("array"), "c");
    Response<Long> insert = p.jsonArrInsert("foo", new Path("array"), 0, "x");
    Response<Long> arrLen = p.jsonArrLen("foo", new Path("array"));
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
    p.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3});
    Response<Object> pop = p.jsonArrPop("arr");

    p.sync();
    c.close();

    assertEquals("OK", set1.get());
    assertEquals(hm1, object.get());
    assertEquals(1, mget.get().size());
    assertEquals(Long.valueOf(5), strLenPath.get());
    assertEquals(Long.valueOf(6), strAppPath.get());
    assertEquals(Long.valueOf(1), delPath.get());
    assertEquals(Long.valueOf(1), delKey.get());
    assertEquals("OK", set2.get());
    assertEquals("c", popPath1.get());
    assertEquals(Long.valueOf(4), append.get());
    assertEquals(Long.valueOf(2), index.get());
    assertEquals(Long.valueOf(5), insert.get());
    assertEquals(Long.valueOf(5), arrLen.get());
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

    Connection c = createConnection();
    Pipeline p = new Pipeline(c);

    Response<String> setWithEscape = p.jsonSetWithEscape("foo", Path2.ROOT_PATH, hm1);
    Response<List<JSONArray>> mgetPath = p.jsonMGet(Path2.ROOT_PATH, "foo");
    Response<List<Long>> strLen = p.jsonStrLen("foo", new Path2("hello"));
    Response<List<Long>> strApp = p.jsonStrAppend("foo", new Path2("hello"), "!");
    Response<Long> del = p.jsonDel("foo", new Path2("hello"));
    Response<String> set = p.jsonSet("bar", Path2.ROOT_PATH, gson.toJson("strung"));
    Response<String> setWithParams = p.jsonSet("foo", Path2.ROOT_PATH, gson.toJson(hm2), new JsonSetParams().xx());
    Response<List<Object>> pop = p.jsonArrPop("foo", new Path2("array"));
    Response<List<Long>> append = p.jsonArrAppend("foo", Path2.of("$.array"), gson.toJson("d"));
    Response<List<Long>> appendWithEscape = p.jsonArrAppendWithEscape("foo", Path2.of("$.array"), "e");
    Response<List<Long>> insert = p.jsonArrInsert("foo", new Path2("array"), 0, gson.toJson("x"));
    Response<List<Long>> insertWithEscape = p.jsonArrInsertWithEscape("foo", new Path2("array"), 0, "x");
    Response<List<Long>> arrLen = p.jsonArrLen("foo", new Path2("array"));
    Response<List<Long>> trim = p.jsonArrTrim("foo", new Path2("array"), 1, 2);
    Response<List<Boolean>> toggle = p.jsonToggle("foo", new Path2("boolean"));
    Response<List<Class<?>>> type = p.jsonType("foo", new Path2("boolean"));
    Response<Long> clear = p.jsonClear("foo", new Path2("array"));

    p.sync();
    c.close();

    assertEquals("OK", setWithEscape.get());
    assertEquals(1, mgetPath.get().size());
    assertEquals(Long.valueOf(5), strLen.get().get(0));
    assertEquals(1, strApp.get().size());
    assertEquals(Long.valueOf(1), del.get());
    assertEquals("OK", set.get());
    assertEquals("OK", setWithParams.get());
    assertEquals("c", pop.get().get(0));
    assertEquals(Long.valueOf(3), append.get().get(0));
    assertEquals(Long.valueOf(4), appendWithEscape.get().get(0));
    assertEquals(Long.valueOf(5), insert.get().get(0));
    assertEquals(Long.valueOf(6), insertWithEscape.get().get(0));
    assertEquals(Long.valueOf(6), arrLen.get().get(0));
    assertEquals(Long.valueOf(2), trim.get().get(0));
    assertEquals(false, toggle.get().get(0));
    assertEquals(boolean.class, type.get().get(0));
    assertEquals(Long.valueOf(1), clear.get());
  }
}
