package redis.clients.jedis.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static redis.clients.jedis.search.RediSearchUtil.toStringMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.*;

public class RedisModulesPipelineTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  @Test
  public void search() {
    Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);

    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "hello world");
    fields.put("body", "lorem ipsum");

    Connection c = createConnection();
    Pipeline p = new Pipeline(c);

    Response<String> string = p.ftCreate("testindex", IndexOptions.defaultOptions(), sc);
    for (int i = 0; i < 100; i++) {
      p.hset(String.format("doc%d", i), toStringMap(fields));
    }
    Response<SearchResult> searchResultResponse = p.ftSearch("testindex", new Query("hello world").limit(0, 5).setWithScores());
    Response<Long> delResponse = p.del("doc0");
    Response<SearchResult> searchResultResponse2 = p.ftSearch("testindex", new Query("hello world"));
    Response<String> dropIndexResponse = p.ftDropIndex("testindex");

    p.sync();
    c.close();

    assertEquals("OK", string.get());
    assertEquals(100, searchResultResponse.get().getTotalResults());
    assertEquals(5, searchResultResponse.get().getDocuments().size());
    for (Document d : searchResultResponse.get().getDocuments()) {
      assertTrue(d.getId().startsWith("doc"));
      assertTrue(d.getScore() < 100);
    }
    assertEquals(Long.valueOf(1), delResponse.get());
    assertEquals(99, searchResultResponse2.get().getTotalResults());
    assertEquals("OK", dropIndexResponse.get());
  }

  @Test
  public void json() {
    Map<String, String> hm1 = new HashMap<>();
    hm1.put("hello", "world");
    hm1.put("oh", "snap");

    Map<String, Object> hm2 = new HashMap<>();
    hm2.put("array", new String[]{"a", "b", "c"});
    hm2.put("boolean", true);
    hm2.put("number", 3);

    Connection c = createConnection();
    Pipeline p = new Pipeline(c);

    Response<String> string1 = p.jsonSet("foo", Path.ROOT_PATH, hm1);
    Response<Object> object = p.jsonGet("foo");
    Response<List<JSONArray>> mget = p.jsonMGet("foo");
    Response<Long> strLenPath = p.jsonStrLen("foo", new Path("hello"));
    Response<Long> strAppPath = p.jsonStrAppend("foo", new Path("hello"), "!");
    Response<Long> delPath = p.jsonDel("foo", new Path("hello"));
    Response<Long> delKey = p.jsonDel("foo");
    Response<Set<String>> keys = p.keys("*");
    Response<String> string2 = p.jsonSet("foo", Path.ROOT_PATH, hm2, new JsonSetParams().nx());
    Response<Object> pop = p.jsonArrPop("foo", new Path("array"));
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
    Response<String> string3 = p.jsonSet("foo", Path.ROOT_PATH, "newStr");
    Response<Long> strLen = p.jsonStrLen("foo");
    Response<Long> strApp = p.jsonStrAppend("foo", "?");

    p.sync();
    c.close();

    assertEquals("OK", string1.get());
    assertEquals(hm1, object.get());
    assertEquals(1, mget.get().size());
    assertEquals(Long.valueOf(5), strLenPath.get());
    assertEquals(Long.valueOf(6), strAppPath.get());
    assertEquals(Long.valueOf(1), delPath.get());
    assertEquals(Long.valueOf(1), delKey.get());
    assertEquals(0, keys.get().size());
    assertEquals("OK", string2.get());
    assertEquals("c", pop.get());
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
    assertEquals("OK", string3.get());
    assertEquals(Long.valueOf(6), strLen.get());
    assertEquals(Long.valueOf(7), strApp.get());
  }
}
