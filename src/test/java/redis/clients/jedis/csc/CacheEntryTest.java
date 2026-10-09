package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.json.JsonBuilderFactory;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.util.SafeEncoder;

public class CacheEntryTest {

  private final CommandObject<Object> jsonCommand = new CommandObjects(RedisProtocol.RESP3)
      .jsonGet("cache-entry-test", Path2.ROOT_PATH);
  private final CacheKey<Object> key = new CacheKey<>(jsonCommand);

  @ParameterizedTest
  @ValueSource(strings = { "{}", "[]",
      "{\"name\":\"测试\",\"active\":true,\"missing\":null,"
          + "\"nested\":{\"n\":1,\"tags\":[\"first\",\"second\"]},"
          + "\"large\":9223372036854775808,\"decimal\":1.234567890123456789}",
      "[{\"nested\":{\"n\":1}},\"second\",42,true,null,[1,2]]" })
  public void preservesJsonContents(String json) {
    Object expected = parseJson(json);
    CacheEntry<Object> entry = jsonEntry(json);
    for (int i = 0; i < 2; i++) {
      Object actual = entry.getValue();
      assertEquals(expected.getClass(), actual.getClass());
      if (expected instanceof JSONObject) {
        assertTrue(((JSONObject) expected).similar(actual));
      } else {
        assertTrue(((JSONArray) expected).similar(actual));
      }
    }
  }

  @Test
  public void modifyingReturnedJsonObjectDoesNotAffectCachedReply() {
    CacheEntry<Object> entry = jsonEntry("{\"nested\":{\"n\":1},\"items\":[1,2]}");
    JSONObject first = (JSONObject) entry.getValue();
    first.getJSONObject("nested").put("n", 999);
    first.getJSONArray("items").put(0, 999);
    JSONObject second = (JSONObject) entry.getValue();
    assertEquals(1, second.getJSONObject("nested").getInt("n"));
    assertEquals(1, second.getJSONArray("items").getInt(0));
  }

  @Test
  public void modifyingReturnedJsonArrayDoesNotAffectCachedReply() {
    CacheEntry<Object> entry = jsonEntry("[{\"n\":1},[1,2]]");
    JSONArray first = (JSONArray) entry.getValue();
    first.getJSONObject(0).put("n", 999);
    first.getJSONArray(1).put(0, 999);
    JSONArray second = (JSONArray) entry.getValue();
    assertEquals(1, second.getJSONObject(0).getInt("n"));
    assertEquals(1, second.getJSONArray(1).getInt(0));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1.0", "1.2300", "0.00", "-0.0", "1e3", "9223372036854775808",
      "0.12345678901234567890123456789" })
  public void preservesJsonNumberRepresentation(String number) {
    String json = "{\"n\":" + number + "}";
    JSONObject expected = (JSONObject) parseJson(json);
    JSONObject actual = (JSONObject) jsonEntry(json).getValue();
    assertEquals(expected.get("n").getClass(), actual.get("n").getClass());
    assertEquals(expected.get("n"), actual.get("n"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1.0", "1.2300", "-0.0" })
  public void preservesNestedJsonNumbersAndNulls(String number) {
    String json = "[{\"n\":" + number + ",\"missing\":null},[" + number + ",null]]";
    JSONArray expected = (JSONArray) parseJson(json);
    JSONArray actual = (JSONArray) jsonEntry(json).getValue();
    Object value = expected.getJSONObject(0).get("n");
    assertEquals(value.getClass(), actual.getJSONObject(0).get("n").getClass());
    assertEquals(value, actual.getJSONObject(0).get("n"));
    assertEquals(value.getClass(), actual.getJSONArray(1).get(0).getClass());
    assertEquals(value, actual.getJSONArray(1).get(0));
    assertEquals(JSONObject.NULL, actual.getJSONObject(0).get("missing"));
    assertEquals(JSONObject.NULL, actual.getJSONArray(1).get(1));
  }

  @Test
  public void preservesRawReplyWhenInputAndOutputAreMutated() {
    byte[] bytes = SafeEncoder.encode("value");
    List<Object> input = new ArrayList<>(Arrays.asList(bytes, null));
    CacheEntry<Object> entry = entry(BuilderFactory.RAW_OBJECT, input);
    bytes[0] = 'X';
    input.clear();
    List<?> first = (List<?>) entry.getValue();
    assertArrayEquals(SafeEncoder.encode("value"), (byte[]) first.get(0));
    ((byte[]) first.get(0))[0] = 'Y';
    first.clear();
    List<?> second = (List<?>) entry.getValue();
    assertEquals(2, second.size());
    assertArrayEquals(SafeEncoder.encode("value"), (byte[]) second.get(0));
    assertNull(second.get(1));
  }

  @Test
  public void binaryBuilderNeverExposesCachedBytes() {
    byte[] input = SafeEncoder.encode("value");
    CacheEntry<byte[]> entry = entry(BuilderFactory.BINARY, input);
    input[0] = 'X';
    byte[] first = entry.getValue();
    first[0] = 'Y';
    assertArrayEquals(SafeEncoder.encode("value"), entry.getValue());
  }

  @Test
  public void rebuildsNonSerializableValuesWithAnIndependentInputEachTime() {
    AtomicInteger builds = new AtomicInteger();
    Builder<Holder> builder = new Builder<Holder>() {
      @Override
      public Holder build(Object data) {
        builds.incrementAndGet();
        List<?> list = (List<?>) data;
        Holder result = new Holder((byte[]) list.get(0));
        list.clear();
        return result;
      }
    };
    CacheEntry<Holder> entry = entry(builder,
      new ArrayList<>(Arrays.asList(SafeEncoder.encode("value"))));
    assertEquals(0, builds.get());
    Holder first = entry.getValue();
    first.bytes[0] = 'X';
    Holder second = entry.getValue();
    assertEquals(2, builds.get());
    assertNotSame(first, second);
    assertArrayEquals(SafeEncoder.encode("value"), second.bytes);
  }

  @Test
  public void concurrentReadsDoNotShareMutableReplies() throws Exception {
    CacheEntry<byte[]> entry = entry(BuilderFactory.BINARY, SafeEncoder.encode("value"));
    ExecutorService workers = Executors.newFixedThreadPool(4);
    try {
      List<Callable<Void>> readers = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        readers.add(() -> {
          byte[] value = entry.getValue();
          assertArrayEquals(SafeEncoder.encode("value"), value);
          value[0] = 'X';
          return null;
        });
      }
      for (Future<Void> result : workers.invokeAll(readers)) {
        result.get();
      }
      assertArrayEquals(SafeEncoder.encode("value"), entry.getValue());
    } finally {
      workers.shutdownNow();
    }
  }

  @Test
  public void preservesNullProtocolReply() {
    assertNull(entry(BuilderFactory.RAW_OBJECT, null).getValue());
    assertNull(entry(JsonBuilderFactory.JSON_OBJECT, null).getValue());
  }

  @Test
  public void compatibilityConstructorPreservesSerializableValuesAndIsolation() {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("list", new ArrayList<>(Arrays.asList("first", null)));
    input.put("missing", null);
    CacheEntry<Object> entry = new CacheEntry<>(key, input, null);
    input.clear();
    Map<?, ?> first = (Map<?, ?>) entry.getValue();
    assertEquals(Arrays.asList("first", null), first.get("list"));
    assertTrue(first.containsKey("missing"));
    assertNull(first.get("missing"));
    ((List<?>) first.get("list")).clear();
    Map<?, ?> second = (Map<?, ?>) entry.getValue();
    assertEquals(Arrays.asList("first", null), second.get("list"));
    assertEquals("plain string", new CacheEntry<>(key, "plain string", null).getValue());
    assertNull(new CacheEntry<>(key, null, null).getValue());
  }

  private CacheEntry<Object> jsonEntry(String json) {
    return new CacheEntry<>(key, SafeEncoder.encode(json), jsonCommand.getBuilder(), null);
  }

  private static <T> CacheEntry<T> entry(Builder<T> builder, Object reply) {
    CacheKey<T> key = new CacheKey<>(
        new CommandObject<>(new CommandArguments(Command.GET).key("reply"), builder));
    return new CacheEntry<>(key, reply, builder, null);
  }

  private static Object parseJson(String json) {
    return JsonBuilderFactory.JSON_OBJECT.build(SafeEncoder.encode(json));
  }

  private static class Holder {
    final byte[] bytes;

    Holder(byte[] bytes) {
      this.bytes = bytes;
    }
  }
}
