package redis.clients.jedis.csc;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.redis.test.annotations.SinceRedisVersion;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.TestKeyRegistry;

@Tag("json")
@SinceRedisVersion(value = "7.4.0", message = "Client-side caching requires Redis 7.4 or later.")
public class CacheEntryJsonIT {

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("modules-docker"));

  @ParameterizedTest(name = "JSON.GET multiplePaths={0}")
  @ValueSource(booleans = { false, true })
  public void cachesJsonCopiesAndInvalidatesAfterUpdate(boolean multiplePaths, TestInfo testInfo) {
    EndpointConfig endpoint = Endpoints.getRedisEndpoint("modules-docker");
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String key = keys.key("document-" + multiplePaths);
    Path2[] paths = multiplePaths ? new Path2[] { Path2.of("$.nested"), Path2.of("$.items") }
        : new Path2[] { Path2.ROOT_PATH };
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (
        RedisClient control = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
            .clientConfig(endpoint.getClientConfigBuilder().resp3().build()).build();
        RedisClient cached = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
            .clientConfig(endpoint.getClientConfigBuilder().resp3().build()).poolConfig(poolConfig)
            .cacheConfig(CacheConfig.builder().build()).build()) {
      try {
        assertEquals("OK", control.jsonSet(key, Path2.ROOT_PATH,
          "{\"nested\":{\"n\":1,\"missing\":null},\"items\":[1,2]}"));
        Object expected = control.jsonGet(key, paths);
        CacheStats stats = cached.getCache().getStats();

        Object first = cached.jsonGet(key, paths);
        assertJsonEquals(expected, first);
        assertEquals(1, stats.getMissCount());
        assertEquals(0, stats.getHitCount());
        nestedObject(first, multiplePaths).put("n", 999);

        Object second = cached.jsonGet(key, paths);
        assertJsonEquals(expected, second);
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());

        assertEquals("OK", control.jsonSet(key, Path2.ROOT_PATH,
          "{\"nested\":{\"n\":2,\"missing\":null},\"items\":[3,4]}"));
        Object updated = control.jsonGet(key, paths);
        // Invalidation arrives asynchronously on the cached connection.
        await().pollInSameThread().atMost(5, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> assertJsonEquals(updated, cached.jsonGet(key, paths)));
        assertEquals(2, stats.getMissCount());
        assertTrue(stats.getInvalidationCount() >= 1);
      } finally {
        keys.cleanup(control);
      }
    }
  }

  private static JSONObject nestedObject(Object value, boolean multiplePaths) {
    return multiplePaths ? ((JSONObject) value).getJSONArray("$.nested").getJSONObject(0)
        : ((JSONArray) value).getJSONObject(0).getJSONObject("nested");
  }

  private static void assertJsonEquals(Object expected, Object actual) {
    assertEquals(expected.getClass(), actual.getClass());
    if (expected instanceof JSONObject) {
      assertTrue(((JSONObject) expected).similar(actual));
    } else {
      assertTrue(((JSONArray) expected).similar(actual));
    }
  }
}
