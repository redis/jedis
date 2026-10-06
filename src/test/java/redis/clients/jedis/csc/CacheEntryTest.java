package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.json.JsonBuilderFactory;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.util.SafeEncoder;

public class CacheEntryTest {

  private final CacheKey<Object> key = new CacheKey<>(
      new CommandObjects(RedisProtocol.RESP3).jsonGet("cache-entry-test", Path2.of("$")));

  @Test
  public void preservesJsonObjectContents() {
    JSONObject original = (JSONObject) parseJson("{\"name\":\"测试\",\"active\":true,"
        + "\"missing\":null,\"nested\":{\"n\":1,\"tags\":[\"first\",\"second\"]},"
        + "\"large\":9223372036854775808,\"decimal\":1.234567890123456789}");

    Object value = new CacheEntry<>(key, original, null).getValue();

    assertEquals(JSONObject.class, value.getClass());
    JSONObject restored = (JSONObject) value;
    assertTrue(original.similar(restored));
    assertEquals("测试", restored.getString("name"));
    assertTrue(restored.getBoolean("active"));
    assertEquals(JSONObject.NULL, restored.get("missing"));
    assertEquals(1, restored.getJSONObject("nested").getInt("n"));
    assertEquals("second", restored.getJSONObject("nested").getJSONArray("tags").getString(1));
    assertEquals(original.get("large"), restored.get("large"));
    assertEquals(original.get("decimal"), restored.get("decimal"));
  }

  @Test
  public void preservesJsonArrayContentsAndOrder() {
    JSONArray original = (JSONArray) parseJson(
      "[{\"name\":\"example\",\"nested\":{\"n\":1}},\"second\",42,true,null,[1,2]]");

    Object value = new CacheEntry<>(key, original, null).getValue();

    assertEquals(JSONArray.class, value.getClass());
    JSONArray restored = (JSONArray) value;
    assertTrue(original.similar(restored));
    assertEquals(6, restored.length());
    assertEquals("example", restored.getJSONObject(0).getString("name"));
    assertEquals(1, restored.getJSONObject(0).getJSONObject("nested").getInt("n"));
    assertEquals("second", restored.getString(1));
    assertEquals(42, restored.getInt(2));
    assertTrue(restored.getBoolean(3));
    assertEquals(JSONObject.NULL, restored.get(4));
    assertEquals(2, restored.getJSONArray(5).getInt(1));
  }

  @ParameterizedTest
  @ValueSource(strings = { "{}", "[]" })
  public void preservesEmptyJsonContainers(String json) {
    Object original = parseJson(json);

    Object restored = new CacheEntry<>(key, original, null).getValue();

    assertEquals(original.getClass(), restored.getClass());
    assertEquals(json, restored.toString());
  }

  @Test
  public void modifyingOriginalJsonObjectDoesNotAffectCachedValue() {
    JSONObject original = (JSONObject) parseJson("{\"nested\":{\"n\":1},\"items\":[1,2]}");
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);

    original.getJSONObject("nested").put("n", 999);
    original.getJSONArray("items").put(0, 999);

    JSONObject restored = (JSONObject) entry.getValue();
    assertEquals(1, restored.getJSONObject("nested").getInt("n"));
    assertEquals(1, restored.getJSONArray("items").getInt(0));
  }

  @Test
  public void modifyingReturnedJsonObjectDoesNotAffectCachedValue() {
    JSONObject original = (JSONObject) parseJson("{\"nested\":{\"n\":1},\"items\":[1,2]}");
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);

    JSONObject first = (JSONObject) entry.getValue();
    first.getJSONObject("nested").put("n", 999);
    first.getJSONArray("items").put(0, 999);

    JSONObject second = (JSONObject) entry.getValue();
    assertEquals(1, second.getJSONObject("nested").getInt("n"));
    assertEquals(1, second.getJSONArray("items").getInt(0));
    assertEquals(1, original.getJSONObject("nested").getInt("n"));
    assertEquals(1, original.getJSONArray("items").getInt(0));
  }

  @Test
  public void modifyingOriginalJsonArrayDoesNotAffectCachedValue() {
    JSONArray original = (JSONArray) parseJson("[{\"n\":1},[1,2]]");
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);

    original.getJSONObject(0).put("n", 999);
    original.getJSONArray(1).put(0, 999);

    JSONArray restored = (JSONArray) entry.getValue();
    assertEquals(1, restored.getJSONObject(0).getInt("n"));
    assertEquals(1, restored.getJSONArray(1).getInt(0));
  }

  @Test
  public void modifyingReturnedJsonArrayDoesNotAffectCachedValue() {
    JSONArray original = (JSONArray) parseJson("[{\"n\":1},[1,2]]");
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);

    JSONArray first = (JSONArray) entry.getValue();
    first.getJSONObject(0).put("n", 999);
    first.getJSONArray(1).put(0, 999);

    JSONArray second = (JSONArray) entry.getValue();
    assertEquals(1, second.getJSONObject(0).getInt("n"));
    assertEquals(1, second.getJSONArray(1).getInt(0));
    assertEquals(1, original.getJSONObject(0).getInt("n"));
    assertEquals(1, original.getJSONArray(1).getInt(0));
  }

  @Test
  public void preservesStringContents() {
    String original = "plain string, not JSON";

    Object restored = new CacheEntry<>(key, original, null).getValue();

    assertEquals(original, restored);
  }

  @ParameterizedTest
  @ValueSource(strings = { "1.0", "1.2300", "0.00", "-0.0", "1e3", "9223372036854775808",
      "0.12345678901234567890123456789" })
  public void preservesJsonNumberRepresentation(String number) {
    JSONObject original = (JSONObject) parseJson("{\"n\":" + number + "}");

    JSONObject restored = (JSONObject) new CacheEntry<>(key, original, null).getValue();

    assertEquals(original.get("n").getClass(), restored.get("n").getClass());
    assertEquals(original.get("n"), restored.get("n"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1.0", "1.2300", "-0.0" })
  public void preservesNestedJsonNumbersAndNulls(String number) {
    JSONArray original = (JSONArray) parseJson(
      "[{\"n\":" + number + ",\"missing\":null},[" + number + ",null]]");

    JSONArray restored = (JSONArray) new CacheEntry<>(key, original, null).getValue();

    Object expected = original.getJSONObject(0).get("n");
    assertEquals(expected.getClass(), restored.getJSONObject(0).get("n").getClass());
    assertEquals(expected, restored.getJSONObject(0).get("n"));
    assertEquals(expected.getClass(), restored.getJSONArray(1).get(0).getClass());
    assertEquals(expected, restored.getJSONArray(1).get(0));
    assertEquals(JSONObject.NULL, restored.getJSONObject(0).get("missing"));
    assertEquals(JSONObject.NULL, restored.getJSONArray(1).get(1));
  }

  @Test
  public void preservesSerializableListContentsAndIsolation() {
    List<String> original = new ArrayList<>(Arrays.asList("first", null, "second"));
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);
    original.set(0, "changed");

    List<?> first = (List<?>) entry.getValue();
    assertEquals(Arrays.asList("first", null, "second"), first);
    first.clear();

    assertEquals(Arrays.asList("first", null, "second"), entry.getValue());
  }

  @Test
  public void preservesSerializableMapContentsAndIsolation() {
    Map<String, Object> original = new LinkedHashMap<>();
    original.put("n", 1);
    original.put("missing", null);
    CacheEntry<Object> entry = new CacheEntry<>(key, original, null);
    original.put("n", 999);

    Map<?, ?> first = (Map<?, ?>) entry.getValue();
    assertEquals(1, first.get("n"));
    assertTrue(first.containsKey("missing"));
    assertNull(first.get("missing"));
    first.clear();

    Map<?, ?> second = (Map<?, ?>) entry.getValue();
    assertEquals(2, second.size());
    assertEquals(1, second.get("n"));
    assertTrue(second.containsKey("missing"));
    assertNull(second.get("missing"));
  }

  @Test
  public void preservesNullValue() {
    assertNull(new CacheEntry<>(key, null, null).getValue());
  }

  private Object parseJson(String json) {
    return JsonBuilderFactory.JSON_OBJECT.build(SafeEncoder.encode(json));
  }
}
