package redis.clients.jedis.csc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.UnifiedJedis;

public abstract class UnifiedJedisClientSideCacheTestBase {

  protected UnifiedJedis control;

  protected abstract UnifiedJedis createRegularJedis();

  protected abstract UnifiedJedis createCachedJedis(CacheConfig cacheConfig);

  @Before
  public void setUp() throws Exception {
    control = createRegularJedis();
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  @Test
  public void simple() {
    CacheConfig cacheConfig = CacheConfig.builder().maxSize(1000).build();
    try (UnifiedJedis jedis = createCachedJedis(cacheConfig)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertNull(jedis.get("foo")));
    }
  }

  @Test
  public void simpleWithSimpleMap() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
      control.del("foo");
      assertEquals(1, cache.getSize());
      await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertNull(jedis.get("foo")));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void flushAll() {
    CacheConfig cacheConfig = CacheConfig.builder().maxSize(1000).build();
    try (UnifiedJedis jedis = createCachedJedis(cacheConfig)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.flushAll();
      await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertNull(jedis.get("foo")));
    }
  }

  @Test
  public void flushAllWithSimpleMap() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
      control.flushAll();
      assertEquals(1, cache.getSize());
      await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertNull(jedis.get("foo")));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void cacheNotEmptyTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void cacheUsedTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();

      control.set("foo", "bar");

      assertEquals(0, cache.getStats().getMissCount());
      assertEquals(0, cache.getStats().getHitCount());

      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getStats().getMissCount());
      assertEquals(0, cache.getStats().getHitCount());

      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getStats().getMissCount());
      assertEquals(1, cache.getStats().getHitCount());
    }
  }

  @Test
  public void immutableCacheEntriesTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      jedis.set("{csc}a", "AA");
      jedis.set("{csc}b", "BB");
      jedis.set("{csc}c", "CC");

      List<String> expected = Arrays.asList("AA", "BB", "CC");

      List<String> reply1 = jedis.mget("{csc}a", "{csc}b", "{csc}c");
      List<String> reply2 = jedis.mget("{csc}a", "{csc}b", "{csc}c");

      assertEquals(expected, reply1);
      assertEquals(expected, reply2);
      assertEquals(reply1, reply2);
      assertNotSame(reply1, reply2);
    }
  }

  @Test
  public void invalidationTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      jedis.set("{csc}1", "one");
      jedis.set("{csc}2", "two");
      jedis.set("{csc}3", "three");

      assertEquals(0, cache.getSize());
      assertEquals(0, cache.getStats().getInvalidationCount());

      List<String> reply1 = jedis.mget("{csc}1", "{csc}2", "{csc}3");
      assertEquals(Arrays.asList("one", "two", "three"), reply1);
      assertEquals(1, cache.getSize());
      assertEquals(0, cache.getStats().getInvalidationCount());

      jedis.set("{csc}1", "new-one");
      List<String> reply2 = jedis.mget("{csc}1", "{csc}2", "{csc}3");
      assertEquals(Arrays.asList("new-one", "two", "three"), reply2);

      assertEquals(1, cache.getSize());
      assertEquals(1, cache.getStats().getInvalidationCount());
    }
  }

  @Test
  public void getNumEntriesTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();

      // Create 100 keys
      for (int i = 0; i < 100; i++) {
        jedis.set("key" + i, "value" + i);
      }
      assertEquals(0, cache.getSize());

      // Get 100 keys into the cache
      for (int i = 0; i < 100; i++) {
        jedis.get("key" + i);
      }
      assertEquals(100, cache.getSize());
    }
  }

  @Test
  public void invalidationOnCacheHitTest() {
    try (UnifiedJedis jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
      // Create 100 keys
      for (int i = 0; i < 100; i++) {
        jedis.set("key" + i, "value" + i);
      }
      assertEquals(0, cache.getSize());

      // Get 100 keys into the cache
      for (int i = 0; i < 100; i++) {
        jedis.get("key" + i);
      }
      assertEquals(100, cache.getSize());

      assertEquals(100, cache.getStats().getLoadCount());
      assertEquals(0, cache.getStats().getInvalidationCount());

      // Change 50 of the 100 keys
      for (int i = 1; i < 100; i += 2) {
        jedis.set("key" + i, "val" + i);
      }

      assertEquals(100, cache.getStats().getLoadCount());
      // invalidation count is anything between 0 and 50

      // Get the 100 keys again
      for (int i = 0; i < 100; i++) {
        jedis.get("key" + i);
      }
      assertEquals(100, cache.getSize());

      assertEquals(150, cache.getStats().getLoadCount());
      assertEquals(50, cache.getStats().getInvalidationCount());
    }
  }

  @Test
  public void simplePubsubWithClientCache() {
    String test_channel = "test_channel";
    String test_message = "test message";

    UnifiedJedis publisher = createCachedJedis(CacheConfig.builder().build());
    Runnable command = () -> publisher.publish(test_channel, test_message + System.currentTimeMillis());
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(command, 0, 100, java.util.concurrent.TimeUnit.MILLISECONDS);

    List<String> receivedMessages = new ArrayList<>();
    try (UnifiedJedis subscriber = createCachedJedis(CacheConfig.builder().build())) {
      JedisPubSub jedisPubSub = new JedisPubSub() {
        private int count = 0;

        @Override
        public void onMessage(String channel, String message) {
          receivedMessages.add(message);
          if (message.startsWith(test_message) && count++ > 1) {
            this.unsubscribe(test_channel);
          }
        }
      };
      subscriber.subscribe(jedisPubSub, test_channel);
    }

    executor.shutdown();
    publisher.close();

    assertTrue(receivedMessages.size() > 1);
    receivedMessages.forEach(message -> assertTrue(message.startsWith(test_message)));
  }

  @Test
  public void advancedPubsubWithClientCache() {
    String test_channel = "test_channel";
    String test_message = "test message";
    String test_key = "test_key";
    String test_value = "test_value";

    UnifiedJedis publisher = createCachedJedis(CacheConfig.builder().build());
    Runnable command = () -> publisher.publish(test_channel, test_message + System.currentTimeMillis());
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(command, 0, 50, java.util.concurrent.TimeUnit.MILLISECONDS);

    int iteration = 0;
    int totalIteration = 10;
    while (iteration++ < totalIteration) {

      List<String> receivedMessages = new ArrayList<>();
      try (UnifiedJedis subscriber = createCachedJedis(CacheConfig.builder().build())) {

        subscriber.set(test_key, test_value);
        assertEquals(test_value, subscriber.get(test_key));
        JedisPubSub jedisPubSub = new JedisPubSub() {
          private int count = 0;

          @Override
          public void onMessage(String channel, String message) {
            receivedMessages.add(message);
            if (message.startsWith(test_message) && count++ > 1) {
              this.unsubscribe(test_channel);
            }
          }
        };
        subscriber.subscribe(jedisPubSub, test_channel);
        subscriber.set(test_key, test_value);
        assertEquals(test_value, subscriber.get(test_key));
      }

      assertTrue(receivedMessages.size() > 1);
      receivedMessages.forEach(message -> assertTrue(message.startsWith(test_message)));
    }

    executor.shutdown();
    publisher.close();
  }
}
