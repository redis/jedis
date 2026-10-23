package redis.clients.jedis.csc;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.redis.test.annotations.SinceRedisVersion;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.SafeEncoder;
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

  @Test
  public void cachesJsonMGetWithIndependentNestedValues(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String firstKey = keys.key("first");
    String secondKey = keys.key("second");
    String missingKey = keys.key("missing");
    try (RedisClient control = client(false); RedisClient cached = client(true)) {
      try {
        control.jsonSet(firstKey, Path2.ROOT_PATH, "{\"n\":1}");
        control.jsonSet(secondKey, Path2.ROOT_PATH, "{\"n\":2}");
        List<JSONArray> first = cached.jsonMGet(Path2.ROOT_PATH, firstKey, secondKey, missingKey);
        assertEquals(1, first.get(0).getJSONObject(0).getInt("n"));
        assertEquals(2, first.get(1).getJSONObject(0).getInt("n"));
        assertNull(first.get(2));
        first.get(0).getJSONObject(0).put("n", 999);
        first.clear();

        List<JSONArray> second = cached.jsonMGet(Path2.ROOT_PATH, firstKey, secondKey, missingKey);
        assertEquals(1, second.get(0).getJSONObject(0).getInt("n"));
        assertEquals(2, second.get(1).getJSONObject(0).getInt("n"));
        assertEquals(1, cached.getCache().getStats().getHitCount());
        assertEquals(1, cached.getCache().getStats().getMissCount());
        assertNull(second.get(2));
      } finally {
        keys.cleanup(control);
      }
    }
  }

  @Test
  public void invalidatesJsonMGetWhenEitherKeyChanges(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String firstKey = keys.key("first");
    String secondKey = keys.key("second");
    try (RedisClient control = client(false); RedisClient cached = client(true)) {
      // Some RedisJSON versions report only the first key and do not invalidate later keys.
      List<String> reportedKeys = control
          .executeCommand(new CommandObject<>(new CommandArguments(Command.COMMAND).add("GETKEYS")
              .add("JSON.MGET").add(firstKey).add(secondKey).add("$"), BuilderFactory.STRING_LIST));
      assumeFalse(reportedKeys.equals(Collections.singletonList(firstKey)),
        "Server limitation: COMMAND GETKEYS JSON.MGET reports only the first key");
      assertEquals(Arrays.asList(firstKey, secondKey), reportedKeys);

      try {
        control.jsonSet(firstKey, Path2.ROOT_PATH, "{\"n\":1}");
        control.jsonSet(secondKey, Path2.ROOT_PATH, "{\"n\":2}");
        List<JSONArray> first = cached.jsonMGet(Path2.ROOT_PATH, firstKey, secondKey);
        assertEquals(1, first.get(0).getJSONObject(0).getInt("n"));
        assertEquals(2, first.get(1).getJSONObject(0).getInt("n"));
        control.jsonSet(secondKey, Path2.ROOT_PATH, "{\"n\":3}");
        await().pollInSameThread().atMost(5, TimeUnit.SECONDS).untilAsserted(
          () -> assertEquals(3, cached.jsonMGet(Path2.ROOT_PATH, firstKey, secondKey).get(1)
              .getJSONObject(0).getInt("n")));
        control.jsonSet(firstKey, Path2.ROOT_PATH, "{\"n\":4}");
        await().pollInSameThread().atMost(5, TimeUnit.SECONDS).untilAsserted(
          () -> assertEquals(4, cached.jsonMGet(Path2.ROOT_PATH, firstKey, secondKey).get(0)
              .getJSONObject(0).getInt("n")));
        assertEquals(3, cached.getCache().getStats().getMissCount());
      } finally {
        keys.cleanup(control);
      }
    }
  }

  @Test
  public void cachesNonSerializableGsonPojo(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String key = keys.key("pojo");
    // Reuse the command: typed JSON builders currently use identity-based cache keys.
    CommandObject<Person> command = new CommandObjects(RedisProtocol.RESP3).jsonGet(key,
      Person.class);
    try (RedisClient control = client(false); RedisClient cached = client(true)) {
      try {
        control.jsonSet(key, Path2.ROOT_PATH, "{\"name\":\"Ada\",\"scores\":[1,2]}");
        Person first = cached.executeCommand(command);
        assertEquals("Ada", first.name);
        first.name = "changed";
        first.scores[0] = 999;
        Person second = cached.executeCommand(command);
        assertNotSame(first, second);
        assertEquals("Ada", second.name);
        assertEquals(1, second.scores[0]);
        assertEquals(1, cached.getCache().getStats().getHitCount());
        assertEquals(1, cached.getCache().getStats().getMissCount());
      } finally {
        keys.cleanup(control);
      }
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void isolatesRawResp3MapKeysAndValues(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String key = keys.key("hash");
    CommandObject<Object> command = new CommandObject<>(
        new CommandArguments(Command.HGETALL).key(key), BuilderFactory.RAW_OBJECT);
    try (RedisClient control = client(false); RedisClient cached = client(true)) {
      try {
        control.hset(key, "field", "value");
        List<KeyValue<byte[], byte[]>> first = (List<KeyValue<byte[], byte[]>>) cached
            .executeCommand(command);
        first.get(0).getKey()[0] = 'X';
        first.get(0).getValue()[0] = 'X';
        first.clear();
        List<KeyValue<byte[], byte[]>> second = (List<KeyValue<byte[], byte[]>>) cached
            .executeCommand(command);
        assertEquals("field", SafeEncoder.encode(second.get(0).getKey()));
        assertEquals("value", SafeEncoder.encode(second.get(0).getValue()));
        assertEquals(1, cached.getCache().getStats().getHitCount());
      } finally {
        keys.cleanup(control);
      }
    }
  }

  @Test
  public void preservesEmptyResp3Map(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String key = keys.key("missing-hash");
    CommandObject<Object> command = new CommandObject<>(
        new CommandArguments(Command.HGETALL).key(key), BuilderFactory.AGGRESSIVE_ENCODED_OBJECT);
    try (RedisClient cached = client(true)) {
      assertTrue(cached.executeCommand(command) instanceof Map);
      assertTrue(cached.executeCommand(command) instanceof Map);
      assertEquals(1, cached.getCache().getStats().getHitCount());
    }
  }

  @Test
  public void onlyCachesSuccessfullyBuiltRepliesAndPreservesHooks(TestInfo testInfo) {
    TestKeyRegistry keys = TestKeyRegistry.create(testInfo);
    String key = keys.key("builder-error");
    AtomicInteger builds = new AtomicInteger();
    AtomicInteger hooks = new AtomicInteger();
    Builder<String> builder = new Builder<String>() {
      @Override
      public String build(Object data) {
        if (builds.getAndIncrement() == 0) {
          throw new IllegalArgumentException("builder rejected reply");
        }
        return BuilderFactory.STRING.build(data);
      }
    };
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.GET).key(key),
        builder).withPreProcessHook(connection -> hooks.incrementAndGet());
    try (RedisClient control = client(false); RedisClient cached = client(true)) {
      try {
        control.set(key, "value");
        assertThrows(IllegalArgumentException.class, () -> cached.executeCommand(command));
        assertEquals(0, cached.getCache().getSize());
        assertEquals("value", cached.executeCommand(command));
        assertEquals("value", cached.executeCommand(command));
        assertEquals(2, hooks.get());
        assertEquals(2, cached.getCache().getStats().getMissCount());
        assertEquals(1, cached.getCache().getStats().getHitCount());
      } finally {
        keys.cleanup(control);
      }
    }
  }

  private static RedisClient client(boolean caching) {
    EndpointConfig endpoint = Endpoints.getRedisEndpoint("modules-docker");
    ConnectionPoolConfig pool = new ConnectionPoolConfig();
    pool.setMaxTotal(1);
    if (caching) {
      return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
          .clientConfig(endpoint.getClientConfigBuilder().resp3().build()).poolConfig(pool)
          .cacheConfig(CacheConfig.builder().build()).build();
    }
    return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().resp3().build()).build();
  }

  private static class Person {
    String name;
    int[] scores;
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
