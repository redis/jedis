package redis.clients.jedis.csc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

public class ClientSideCacheFunctionalityTest extends ClientSideCacheTestBase {

  @Test
  public void flushEntireCache() {
    int count = 100;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    Cache clientSideCache = new TestCache(map);
    JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), clientSideCache);
    try {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }

      assertEquals(count, map.size());
      clientSideCache.flush();
      assertEquals(0, map.size());
    } finally {
      jedis.close();
    }
  }

  @Test
  public void removeSpecificKey() {
    int count = 100;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    // By using LinkedHashMap, we can get the hashes (map keys) at the same order of the actual keys.
    LinkedHashMap<CacheKey, CacheEntry> map = new LinkedHashMap<>();
    Cache clientSideCache = new TestCache(map);
    JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), clientSideCache);
    try {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
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
    } finally {
      jedis.close();
    }
  }

  @Test
  public void multiKeyOperation() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), new TestCache(map))) {
      jedis.mget("k1", "k2");
      assertEquals(1, map.size());
    }
  }

  @Test
  public void maximumSizeExact() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    DefaultCache cache = new DefaultCache(1);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), cache)) {
      assertEquals(0, cache.getSize());
      jedis.get("k1");
      assertEquals(1, cache.getSize());
      assertEquals(0, cache.getStats().getEvictCount());
      jedis.get("k2");
      assertEquals(1, cache.getSize());
      assertEquals(1, cache.getStats().getEvictCount());
    }
  }

  @Test
  public void testInvalidationWithUnifiedJedis() {
    Cache cache = new TestCache();
    Cache mock = Mockito.spy(cache);
    UnifiedJedis client = new UnifiedJedis(hnp, clientConfig.get(), mock) {
    };
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

      Mockito.verify(mock, Mockito.times(1)).deleteByRedisKeys(Mockito.anyList());
      Mockito.verify(mock, Mockito.times(2)).set(Mockito.any(CacheKey.class), Mockito.any(CacheEntry.class));
    } finally {
      client.close();
      controlClient.close();
    }
  }

  @Test
  public void differentInstanceOnEachCacheHit() {
    ConcurrentHashMap<CacheKey, CacheEntry> map = new ConcurrentHashMap<CacheKey, CacheEntry>();
    TestCache testCache = new TestCache(map);

    // fill the cache for maxSize
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), testCache)) {
      jedis.sadd("foo", "a");
      jedis.sadd("foo", "b");

      Set<String> expected = new HashSet<String>();
      expected.add("a");
      expected.add("b");

      Set<String> members1 = jedis.smembers("foo");
      Set<String> members2 = jedis.smembers("foo");

      Set<String> fromMap = (Set<String>) testCache.get(new CacheKey<>(new CommandObjects().smembers("foo")))
          .getValue();
      assertEquals(expected, members1);
      assertEquals(expected, members2);
      assertEquals(expected, fromMap);
      assertTrue(members1 != members2);
      assertTrue(members1 != fromMap);
    }
  }
}
