package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache())) {
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(map),
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache())) {
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(map),
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

    // Create the shared mock instance of cache
    TestCache testCache = new TestCache();

    // Submit multiple threads to perform concurrent operations
    CountDownLatch latch = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
          for (int j = 0; j < iterations; j++) {
            lock.lock();
            try {
              // Simulate continious get and update operations and consume invalidation events meanwhile
              Integer value = new Integer(jedis.get("foo"));
              assertEquals(control.get("foo"), value.toString());
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get())) {
      String finalValue = jedis.get("foo");
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
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
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
        try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
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

    ConcurrentHashMap<CacheKey, CacheEntry> map = new ConcurrentHashMap<CacheKey, CacheEntry>();
    TestCache testCache = new TestCache(maxSize, map, DefaultClientSideCacheable.INSTANCE);

    // fill the cache for maxSize
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
      for (int i = 0; i < maxSize; i++) {
        jedis.set("foo" + i, "bar" + i);
        assertEquals("bar" + i, jedis.get("foo" + i));
      }
    }

    // touch a set of keys to prevent from eviction
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
      for (int i = touchOffset; i < touchOffset + expectedEvictions; i++) {
        assertEquals("bar" + i, jedis.get("foo" + i));
      }
    }

    // add more keys to trigger eviction
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
      for (int i = maxSize; i < maxSize + expectedEvictions; i++) {
        jedis.set("foo" + i, "bar" + i);
        assertEquals("bar" + i, jedis.get("foo" + i));
      }
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
