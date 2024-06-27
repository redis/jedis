package redis.clients.jedis.csc;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.junit.Test;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.util.JedisURIHelper;

public class ClientSideCacheFunctionalityTest extends ClientSideCacheTestBase {

  @Test
  public void flushEntireCache() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    HashMap<CommandObject, Object> map = new HashMap<>();
    ClientSideCache clientSideCache = new MapClientSideCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    assertEquals(count, map.size());
    clientSideCache.clear();
    assertEquals(0, map.size());
  }

  @Test
  public void removeSpecificKey() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    // By using LinkedHashMap, we can get the hashes (map keys) at the same order of the actual keys.
    LinkedHashMap<CommandObject, Object> map = new LinkedHashMap<>();
    ClientSideCache clientSideCache = new MapClientSideCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    ArrayList<CommandObject> commandHashes = new ArrayList<>(map.keySet());
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      String key = "k" + i;
      CommandObject command = commandHashes.get(i);
      assertTrue(map.containsKey(command));
      clientSideCache.removeKey(key);
      assertFalse(map.containsKey(command));
    }
  }

  @Test
  public void multiKeyOperation() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new MapClientSideCache(map))) {
      jedis.mget("k1", "k2");
      assertEquals(1, map.size());
    }
  }

  @Test
  public void uriNoParam() {
    URI uri = URI.create(baseUrl + "?");
    assertNull(JedisURIHelper.getClientSideCache(uri));
  }

  @Test
  public void uriUnknownLib() {
    URI uri = URI.create(baseUrl + "?cache_lib=unknown");
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
        () -> JedisURIHelper.getClientSideCache(uri));
    assertEquals("Unsupported library unknown", iae.getMessage());
  }

  @Test
  public void uriNoLib() {
    String[] otherParams
        = new String[]{
          "?cache_max_size=1000",
          "?cache_ttl=10",
          "?cache_max_size=1000&cache_ttl=10"
        };
    Arrays.stream(otherParams).forEach(urlParams -> {
      URI uri = URI.create(baseUrl + urlParams);
      IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
          () -> JedisURIHelper.getClientSideCache(uri));
      assertEquals("A supported caching library (guava OR caffeine) must be selected.", iae.getMessage());
    });
  }

}
