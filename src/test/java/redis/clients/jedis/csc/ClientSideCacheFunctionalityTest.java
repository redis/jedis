package redis.clients.jedis.csc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

public class ClientSideCacheFunctionalityTest extends ClientSideCacheTestBase {

  @Test
  public void flushEntireCache() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    Cache clientSideCache = new TestCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    assertEquals(count, map.size());
    clientSideCache.flush();
    assertEquals(0, map.size());
  }

  @Test
  public void removeSpecificKey() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    // By using LinkedHashMap, we can get the hashes (map keys) at the same order of the actual keys.
    LinkedHashMap<CacheKey, CacheEntry> map = new LinkedHashMap<>();
    Cache clientSideCache = new TestCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    ArrayList<CacheKey> commandHashes = new ArrayList<>(map.keySet());
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      String key = "k" + i;
      CacheKey command = commandHashes.get(i);
      assertTrue(map.containsKey(command));
      clientSideCache.deleteByRedisKey(key);
      assertFalse(map.containsKey(command));
    }
  }

  @Test
  public void multiKeyOperation() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new TestCache(map))) {
      jedis.mget("k1", "k2");
      assertEquals(1, map.size());
    }
  }

  @Test
  public void testInvalidationWithUnifiedJedis() {
    Cache cache = new TestCache();
    Cache mock = Mockito.spy(cache);
    UnifiedJedis client = new UnifiedJedis(hnp, clientConfig.get(), mock);
    UnifiedJedis controlClient = new UnifiedJedis(hnp, clientConfig.get());

    try {
      // "foo" is cached
      client.set("foo", "bar");
      client.get("foo"); // read from the server
      Assert.assertEquals("bar", client.get("foo")); // cache hit

      // Using another connection
      controlClient.set("foo", "bar2");
      Assert.assertEquals("bar2", controlClient.get("foo"));

      //invalidating the cache and read it back from server
      Assert.assertEquals("bar2", client.get("foo"));

      // ArgumentCaptor<GuavaClientSideCache> argumentCaptor = ArgumentCaptor.forClass(GuavaClientSideCache.class);
      Mockito.verify(mock, Mockito.times(1)).deleteByRedisKeys(Mockito.anyList());
      Mockito.verify(mock, Mockito.times(2)).set(Mockito.any(CacheKey.class), Mockito.any(CacheEntry.class));
    } finally {
      client.close();
      controlClient.close();
    }
  }
}
