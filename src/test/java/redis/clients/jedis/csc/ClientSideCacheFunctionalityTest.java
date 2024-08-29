package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

public class ClientSideCacheFunctionalityTest extends ClientSideCacheTestBase {

  @Test // T.5.1
  public void flushAllTest() {
    final int count = 100;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }

      assertEquals(count, cache.getSize());
      cache.flush();
      assertEquals(0, cache.getSize());
    }
  }

  @Test // T.4.1
  public void lruEvictionTest() {
    final int count = 100;
    final int extra = 10;

    // Add 100 + 10 keys to Redis
    for (int i = 0; i < count + extra; i++) {
      control.set("key:" + i, "value" + i);
    }

    Map<CacheKey, CacheEntry> map = new LinkedHashMap<>(count);
    Cache cache = new DefaultCache(count, map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), cache)) {

      // Retrieve the 100 keys in the same order
      for (int i = 0; i < count; i++) {
        jedis.get("key:" + i);
      }
      assertThat(map, aMapWithSize(count));

      List<CacheKey> earlierKeys = new ArrayList<>(map.keySet()).subList(0, extra);
      // earlier keys in map
      earlierKeys.forEach(cacheKey -> assertThat(map, Matchers.hasKey(cacheKey)));

      // Retrieve the 10 extra keys
      for (int i = count; i < count + extra; i++) {
        jedis.get("key:" + i);
      }

      // earlier keys NOT in map
      earlierKeys.forEach(cacheKey -> assertThat(map, Matchers.not(Matchers.hasKey(cacheKey))));
      assertThat(map, aMapWithSize(count));
    }
  }

  @Test // T.5.2
  public void deleteByKeyUsingMGetTest() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      Cache clientSideCache = jedis.getCache();

      jedis.set("1", "one");
      jedis.set("2", "two");

      assertEquals(Arrays.asList("one", "two"), jedis.mget("1", "2"));
      assertEquals(1, clientSideCache.getSize());

      assertThat(clientSideCache.deleteByRedisKey("1"), hasSize(1));
      assertEquals(0, clientSideCache.getSize());
    }
  }

  @Test // T.5.2
  public void deleteByKeyTest() {
    final int count = 100;
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
      assertThat(map, aMapWithSize(count));

      ArrayList<CacheKey> cacheKeys = new ArrayList<>(map.keySet());
      for (int i = 0; i < count; i++) {
        String key = "k" + i;
        CacheKey cacheKey = cacheKeys.get(i);
        assertTrue(map.containsKey(cacheKey));
        assertThat(clientSideCache.deleteByRedisKey(key), hasSize(1));
        assertFalse(map.containsKey(cacheKey));
        assertThat(map, aMapWithSize(count - i - 1));
      }
      assertThat(map, aMapWithSize(0));
    }
  }

  @Test // T.5.2
  public void deleteByKeysTest() {
    final int count = 100;
    final int delete = 10;

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
      assertThat(map, aMapWithSize(count));

      List<String> keysToDelete = new ArrayList<>(delete);
      for (int i = 0; i < delete; i++) {
        String key = "k" + i;
        keysToDelete.add(key);
      }
      assertThat(clientSideCache.deleteByRedisKeys(keysToDelete), hasSize(delete));
      assertThat(map, aMapWithSize(count - delete));
    }
  }

  @Test // T.5.3
  public void deleteByEntryTest() {
    final int count = 100;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
      assertEquals(count, cache.getSize());

      List<CacheEntry> cacheKeys = new ArrayList<>(cache.getCacheEntries());
      for (int i = 0; i < count; i++) {
        CacheKey cacheKey = cacheKeys.get(i).getCacheKey();
        assertTrue(cache.delete(cacheKey));
        assertFalse(cache.hasCacheKey(cacheKey));
        assertEquals(count - i - 1, cache.getSize());
      }
    }
  }

  @Test // T.5.3
  public void deleteByEntriesTest() {
    final int count = 100;
    final int delete = 10;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
      assertEquals(count, cache.getSize());

      List<CacheKey> cacheKeysToDelete = new ArrayList<>(cache.getCacheEntries()).subList(0, delete).stream().map(e -> e.getCacheKey())
          .collect(Collectors.toList());
      List<Boolean> isDeleted = cache.delete(cacheKeysToDelete);
      assertThat(isDeleted, hasSize(delete));
      isDeleted.forEach(Assert::assertTrue);
      assertEquals(count - delete, cache.getSize());
    }
  }

  @Test
  public void multiKeyOperation() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      jedis.mget("k1", "k2");
      assertEquals(1, jedis.getCache().getSize());
    }
  }

  @Test
  public void maximumSizeExact() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().maxSize(1).build())) {
      Cache cache = jedis.getCache();
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

      Mockito.verify(mock, Mockito.times(1)).deleteByRedisKeys(Mockito.anyList());
      Mockito.verify(mock, Mockito.times(2)).set(Mockito.any(CacheKey.class), Mockito.any(CacheEntry.class));
    } finally {
      client.close();
      controlClient.close();
    }
  }

  @Test
  public void differentInstanceOnEachCacheHit() {

    // fill the cache for maxSize
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      jedis.sadd("foo", "a");
      jedis.sadd("foo", "b");

      Set<String> expected = new HashSet<>();
      expected.add("a");
      expected.add("b");

      Set<String> members1 = jedis.smembers("foo");
      Set<String> members2 = jedis.smembers("foo");

      Set<String> fromMap = (Set<String>) cache.get(new CacheKey<>(new CommandObjects().smembers("foo"))).getValue();
      assertEquals(expected, members1);
      assertEquals(expected, members2);
      assertEquals(expected, fromMap);
      assertTrue(members1 != members2);
      assertTrue(members1 != fromMap);
    }
  }

  @Test
  public void testSequentialAccess() throws InterruptedException {
    int threadCount = 10;
    int iterations = 10000;

    control.set("foo", "0");

    ReentrantLock lock = new ReentrantLock(true);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    CacheConfig cacheConfig = CacheConfig.builder().maxSize(1000).build();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cacheConfig)) {
      // Submit multiple threads to perform concurrent operations
      CountDownLatch latch = new CountDownLatch(threadCount);
      for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
          try {
            for (int j = 0; j < iterations; j++) {
              lock.lock();
              try {
                // Simulate continious get and update operations and consume invalidation events meanwhile
                assertEquals(control.get("foo"), jedis.get("foo"));
                Integer value = new Integer(jedis.get("foo"));
                assertEquals("OK", jedis.set("foo", (++value).toString()));
              } finally {
                lock.unlock();
              }
            }
          } finally {
            latch.countDown();
          }
        });
      }

      // wait for all threads to complete
      latch.await();
    }

    executorService.shutdownNow();

    // Verify the final value of "foo" in Redis
    String finalValue = control.get("foo");
    assertEquals(threadCount * iterations, Integer.parseInt(finalValue));
  }

  @Test
  public void testConcurrentAccessWithStats() throws InterruptedException {
    int threadCount = 10;
    int iterations = 10000;

    control.set("foo", "0");

    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    // Create the shared mock instance of cache
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      // Submit multiple threads to perform concurrent operations
      CountDownLatch latch = new CountDownLatch(threadCount);
      for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
          try {
            for (int j = 0; j < iterations; j++) {
              // Simulate continious get and update operations and consume invalidation events meanwhile
              Integer value = new Integer(jedis.get("foo")) + 1;
              assertEquals("OK", jedis.set("foo", value.toString()));
            }
          } finally {
            latch.countDown();
          }
        });
      }

      // wait for all threads to complete
      latch.await();

      executorService.shutdownNow();

      CacheStats stats = cache.getStats();
      assertEquals(threadCount * iterations, stats.getMissCount() + stats.getHitCount());
      assertEquals(stats.getMissCount(), stats.getLoadCount());
    }
  }

  @Test
  public void testMaxSize() throws InterruptedException {
    int threadCount = 10;
    int iterations = 11000;
    int maxSize = 1000;

    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), CacheConfig.builder().maxSize(maxSize).build())) {
      Cache testCache = jedis.getCache();
      // Submit multiple threads to perform concurrent operations
      CountDownLatch latch = new CountDownLatch(threadCount);
      for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
          try {
            for (int j = 0; j < iterations; j++) {
              // Simulate continious get and update operations and consume invalidation events meanwhile
              assertEquals("OK", jedis.set("foo" + j, "foo" + j));
              jedis.get("foo" + j);
            }
          } finally {
            latch.countDown();
          }
        });
      }

      // wait for all threads to complete
      latch.await();

      executorService.shutdownNow();

      CacheStats stats = testCache.getStats();

      assertEquals(threadCount * iterations, stats.getMissCount() + stats.getHitCount());
      assertEquals(stats.getMissCount(), stats.getLoadCount());
      assertEquals(threadCount * iterations, stats.getNonCacheableCount());
      assertTrue(maxSize >= testCache.getSize());
    }
  }

  @Test
  public void testEvictionPolicy() throws InterruptedException {
    int maxSize = 100;
    int expectedEvictions = 20;
    int touchOffset = 10;

    // fill the cache for maxSize
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(),
        CacheConfig.builder().maxSize(maxSize).build())) {
      Cache cache = jedis.getCache();
      for (int i = 0; i < maxSize; i++) {
        jedis.set("foo" + i, "bar" + i);
        assertEquals("bar" + i, jedis.get("foo" + i));
      }

      // touch a set of keys to prevent from eviction from index 10 to 29
      for (int i = touchOffset; i < touchOffset + expectedEvictions; i++) {
        assertEquals("bar" + i, jedis.get("foo" + i));
      }

      // add more keys to trigger eviction, adding from 100 to 119
      for (int i = maxSize; i < maxSize + expectedEvictions; i++) {
        jedis.set("foo" + i, "bar" + i);
        assertEquals("bar" + i, jedis.get("foo" + i));
      }

      // check touched keys not evicted
      for (int i = touchOffset; i < touchOffset + expectedEvictions; i++) {
        assertTrue(cache.hasCacheKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      // check expected evictions are done till the offset
      for (int i = 0; i < touchOffset; i++) {
        assertTrue(!cache.hasCacheKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      /// check expected evictions are done after the touched keys
      for (int i = touchOffset + expectedEvictions; i < (2 * expectedEvictions); i++) {
        assertTrue(!cache.hasCacheKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      assertEquals(maxSize, cache.getSize());
    }
  }

  @Test
  public void testEvictionPolicyMultithreaded() throws InterruptedException {
    int NUMBER_OF_THREADS = 100;
    int TOTAL_OPERATIONS = 1000000;
    int NUMBER_OF_DISTINCT_KEYS = 53;
    int MAX_SIZE = 20;
    List<Exception> exceptions = new ArrayList<>();

    List<Thread> tds = new ArrayList<>();
    final AtomicInteger ind = new AtomicInteger();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(),
        CacheConfig.builder().maxSize(MAX_SIZE).build())) {
      Cache cache = jedis.getCache();
      for (int i = 0; i < NUMBER_OF_THREADS; i++) {
        Thread hj = new Thread(new Runnable() {
          @Override
          public void run() {
            for (int i = 0; (i = ind.getAndIncrement()) < TOTAL_OPERATIONS;) {
              try {
                final String key = "foo" + i % NUMBER_OF_DISTINCT_KEYS;
                if (i < NUMBER_OF_DISTINCT_KEYS) {
                  jedis.set(key, key);
                }
                jedis.get(key);
              } catch (Exception e) {
                exceptions.add(e);
                throw e;
              }
            }
          }
        });
        tds.add(hj);
        hj.start();
      }

      for (Thread t : tds) {
        t.join();
      }

      assertEquals(MAX_SIZE, cache.getSize());
      assertEquals(0, exceptions.size());
    }
  }

  @Test
  public void testNullValue() throws InterruptedException {
    int MAX_SIZE = 20;
    String nonExisting = "non-existing-key";
    control.del(nonExisting);

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().maxSize(MAX_SIZE).build())) {
      Cache cache = jedis.getCache();
      CacheStats stats = cache.getStats();

      String val = jedis.get(nonExisting);
      assertNull(val);
      assertEquals(1, cache.getSize());
      assertEquals(0, stats.getHitCount());
      assertEquals(1, stats.getMissCount());

      val = jedis.get(nonExisting);
      assertNull(val);
      assertEquals(1, cache.getSize());
      assertNull(cache.getCacheEntries().iterator().next().getValue());
      assertEquals(1, stats.getHitCount());
      assertEquals(1, stats.getMissCount());

      control.set(nonExisting, "bar");
      val = jedis.get(nonExisting);
      assertEquals("bar", val);
      assertEquals(1, cache.getSize());
      assertEquals("bar", cache.getCacheEntries().iterator().next().getValue());
      assertEquals(1, stats.getHitCount());
      assertEquals(2, stats.getMissCount());
    }
  }

  @Test
  public void testCacheFactory() throws InterruptedException {
    // this checks the instantiation with parameters (int, EvictionPolicy, Cacheable)
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), CacheConfig.builder().cacheClass(TestCache.class).build())) {
      Cache cache = jedis.getCache();
      CacheStats stats = cache.getStats();

      String val = jedis.get("foo");
      val = jedis.get("foo");
      assertNull(val);
      assertEquals(1, cache.getSize());
      assertNull(cache.getCacheEntries().iterator().next().getValue());
      assertEquals(1, stats.getHitCount());
      assertEquals(1, stats.getMissCount());
    }

    // this checks the instantiation with parameters (int, EvictionPolicy)
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        CacheConfig.builder().cacheClass(TestCache.class).cacheable(null).build())) {
      Cache cache = jedis.getCache();
      CacheStats stats = cache.getStats();

      String val = jedis.get("foo");
      val = jedis.get("foo");
      assertNull(val);
      assertEquals(1, cache.getSize());
      assertNull(cache.getCacheEntries().iterator().next().getValue());
      assertEquals(1, stats.getHitCount());
      assertEquals(1, stats.getMissCount());
    }
  }
}
