package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Command;

public class CacheEntryTest {

  private final CacheKey<Object> key = new CacheKey<>(
      new CommandObject<>(new CommandArguments(Command.GET).key("k"), BuilderFactory.RAW_OBJECT));

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
}
