package redis.clients.jedis.csc;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandObject;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class CacheEntryTest {
    @Test
    public void testJsonObjectCaching() {
        JSONObject json = new JSONObject();
        json.put("foo", "bar");

        CommandObject<JSONObject> command = mock(CommandObject.class);
        CacheKey<JSONObject> key = new CacheKey<>(command);

        // Mock CacheConnection instead of creating a real one
        CacheConnection conn = mock(CacheConnection.class);

        CacheEntry<JSONObject> entry = new CacheEntry<>(key, json, conn);
        Object value = entry.getValue();

        // JSONObject is not Serializable → should fallback to String
        assertTrue(value instanceof String);
        assertEquals("{\"foo\":\"bar\"}", value.toString());
    }

    @Test
    public void testSerializableObjectCaching() {
        // Prepare a normal Serializable object
        String str = "Hello Jedis!";

        CommandObject<String> command = mock(CommandObject.class);
        CacheKey<String> key = new CacheKey<>(command);

        CacheConnection conn = mock(CacheConnection.class);

        CacheEntry<String> entry = new CacheEntry<>(key, str, conn);
        Object value = entry.getValue();

        // Serializable object should remain intact
        assertTrue(value instanceof String);
        assertEquals(str, value);

        // Check getters
        assertEquals(key, entry.getCacheKey());
        assertEquals(conn, entry.getConnection());
    }
}
