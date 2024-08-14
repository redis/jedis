package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.locationtech.jts.util.Assert;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@RunWith(Parameterized.class)
public class JedisPooledClientSideCacheTest {

  private EndpointConfig endpoint;

  protected Jedis control;

  protected static final EndpointConfig sslEndpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

  protected Jedis sslControl;

  public JedisPooledClientSideCacheTest(EndpointConfig endpoint) {
    this.endpoint = endpoint;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { HostAndPorts.getRedisEndpoint("standalone1") },
        { HostAndPorts.getRedisEndpoint("standalone0-tls") },
    });
  }

  @BeforeClass
  public static void prepare() {
    setupTrustStore();
  }

  static void setupTrustStore() {
    setJvmTrustStore("src/test/resources/truststore.jceks", "jceks");
  }

  private static void setJvmTrustStore(String trustStoreFilePath, String trustStoreType) {
    assertTrue(String.format("Could not find trust store at '%s'.", trustStoreFilePath),
        new File(trustStoreFilePath).exists());
    System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
    System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
  }

  @Before
  public void setUp() throws Exception {

    control = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  private final Supplier<JedisClientConfig> clientConfig = () -> endpoint.getClientConfigBuilder().resp3().build();

  private final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig = () -> {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    return poolConfig;
  };

  private void sleep() {
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void simple() {
    CacheConfig cacheConfig = new CacheConfig.Builder().maxSize(1000).build();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cacheConfig)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      sleep();
      assertNull(jedis.get("foo"));
    }
  }

  @Test
  public void simpleWithSimpleMap() {
    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(map),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      control.del("foo");
      assertThat(map, Matchers.aMapWithSize(1));
      sleep();
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
      sleep();
      assertThat(map, Matchers.aMapWithSize(0));
      sleep();
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void flushAll() {
    CacheConfig cacheConfig = new CacheConfig.Builder().maxSize(1000).build();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cacheConfig)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.flushAll();
      sleep();
      assertNull(jedis.get("foo"));
    }
  }

  @Test
  public void flushAllWithSimpleMap() {
    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(map),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      control.flushAll();
      assertThat(map, Matchers.aMapWithSize(1));
      sleep();
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
      sleep();
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void testSequentialAccess() throws InterruptedException {
    int threadCount = 10;
    int iterations = 10000;

    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get())) {
      jedis.set("foo", "0");
    }

    ReentrantLock lock = new ReentrantLock(true);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    CountDownLatch latch = new CountDownLatch(threadCount);
    CacheConfig cacheConfig = new CacheConfig.Builder().maxSize(1000).build();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cacheConfig)) {

      // Submit multiple threads to perform concurrent operations
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

      // Verify the final value of "foo" in Redis
      String finalValue = control.get("foo");
      assertEquals(threadCount * iterations, Integer.parseInt(finalValue));
    }
  }

  @Test
  public void testConcurrentAccessWithStats() throws InterruptedException {
    int threadCount = 10;
    int iterations = 10000;

    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get())) {
      jedis.set("foo", "0");
    }

    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    // Create the shared mock instance of cache
    TestCache testCache = new TestCache();

    // Submit multiple threads to perform concurrent operations
    CountDownLatch latch = new CountDownLatch(threadCount);
    try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
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

      CacheStats stats = testCache.getStats();
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

    ConcurrentHashMap<CacheKey, CacheEntry> map = new ConcurrentHashMap<CacheKey, CacheEntry>();
    // Create the shared mock instance of cache
    TestCache testCache = new TestCache(maxSize, map, DefaultClientSideCacheable.INSTANCE);

    // Submit multiple threads to perform concurrent operations
    CountDownLatch latch = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
          for (int j = 0; j < iterations; j++) {
            // Simulate continious get and update operations and consume invalidation events meanwhile
            assertEquals("OK", jedis.set("foo" + j, "foo" + j));
            jedis.get("foo" + j);
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      });
    }

    // wait for all threads to complete
    latch.await();

    CacheStats stats = testCache.getStats();

    assertEquals(threadCount * iterations, stats.getMissCount() + stats.getHitCount());
    assertEquals(stats.getMissCount(), stats.getLoadCount());
    assertEquals(threadCount * iterations, stats.getNonCacheableCount());
    assertTrue(maxSize >= testCache.getSize());
  }

  @Test
  public void testEvictionPolicy() throws InterruptedException {
    int maxSize = 100;
    int expectedEvictions = 20;
    int touchOffset = 10;

    HashMap<CacheKey, CacheEntry> map = new HashMap<CacheKey, CacheEntry>();
    TestCache testCache = new TestCache(maxSize, map, DefaultClientSideCacheable.INSTANCE);

    // fill the cache for maxSize
    try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
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

        assertTrue(map.containsKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      // check expected evictions are done till the offset
      for (int i = 0; i < touchOffset; i++) {
        assertTrue(!map.containsKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      /// check expected evictions are done after the touched keys
      for (int i = touchOffset + expectedEvictions; i < (2 * expectedEvictions); i++) {
        assertTrue(!map.containsKey(new CacheKey(new CommandObjects().get("foo" + i))));
      }

      assertEquals(maxSize, testCache.getSize());
    }
  }

  @Test
  public void testEvictionPolicyMultithreaded() throws InterruptedException {
    int NUMBER_OF_THREADS = 100;
    int TOTAL_OPERATIONS = 1000000;
    int NUMBER_OF_DISTINCT_KEYS = 53;
    int MAX_SIZE = 20;
    List<Exception> exceptions = new ArrayList<>();

    TestCache cache = new TestCache(MAX_SIZE, new HashMap<>(), DefaultClientSideCacheable.INSTANCE);
    List<Thread> tds = new ArrayList<>();
    final AtomicInteger ind = new AtomicInteger();
    try (JedisPooled jedis = new TestJedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache)) {
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

      Assert.equals(MAX_SIZE, cache.getSize());
      Assert.equals(0, exceptions.size());
    }
  }

}
