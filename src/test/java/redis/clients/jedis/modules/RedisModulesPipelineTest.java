package redis.clients.jedis.modules;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static redis.clients.jedis.search.RediSearchUtil.toStringMap;

public class RedisModulesPipelineTest extends RedisModuleCommandsTestBase {
    @BeforeClass
    public static void prepare() {
        RedisModuleCommandsTestBase.prepare();
    }

    @Test
    public void search() {
        Jedis jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().timeoutMillis(500).build());
        jedis.flushAll();

        Schema sc = new Schema().addTextField("title", 1.0).addTextField("body", 1.0);

        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "hello world");
        fields.put("body", "lorem ipsum");

        Pipeline p = jedis.pipelined();
        Response<String> string = p.ftCreate("testindex", IndexOptions.defaultOptions(), sc);
        for (int i = 0; i < 100; i++) {
            p.hset(String.format("doc%d", i), toStringMap(fields));
        }
        Response<SearchResult> searchResultResponse = p.ftSearch("testindex", new Query("hello world").limit(0, 5).setWithScores());
        Response<Long> delResponse = p.del("doc0");
        Response<SearchResult> searchResultResponse2 = p.ftSearch("testindex", new Query("hello world"));
    //    Response<String> dropIndexResponse = p.ftDropIndex(index);
        p.sync();


        assertEquals("OK", string.get());
        assertEquals(100, searchResultResponse.get().getTotalResults());
        assertEquals(5, searchResultResponse.get().getDocuments().size());
        for (Document d : searchResultResponse.get().getDocuments()) {
            assertTrue(d.getId().startsWith("doc"));
            assertTrue(d.getScore() < 100);
        }
        assertEquals(Long.valueOf(1), delResponse.get());
        assertEquals(99, searchResultResponse2.get().getTotalResults());
    //    assertEquals("OK", dropIndexResponse.get());
    }

    @Test
    public void json() {
        Jedis jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().timeoutMillis(500).build());
        jedis.flushAll();

        Map<String, String> hm = new HashMap<>();
        hm.put("hello", "world");
        hm.put("oh", "snap");

        Pipeline p = jedis.pipelined();
        Response<String> string = p.jsonSet("foo", Path.ROOT_PATH, hm);
        Response<Object> object = p.jsonGet("foo");
        Response<Long> longResponse = p.jsonDel("foo");
        Response<Set<String>> keys = p.keys("*");
        p.sync();

        assertEquals("OK", string.get());
        assertEquals(hm, object.get());
        assertEquals(Long.valueOf(1), longResponse.get());
        assertEquals(0, keys.get().size());
    }
}
