package redis.clients.jedis.csc;

import org.junit.BeforeClass;
import redis.clients.jedis.HostAndPorts;

public class JedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

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
      throw new RuntimeException("Interrupted while sleeping", e);
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

    // ... because a command execution should either result in hit or miss
    assertEquals(threadCount * iterations, stats.getMissCount() + stats.getHitCount());

    // ... because all set commands are not cachable
    assertEquals(threadCount * iterations, stats.getNonCacheableCount());

    // ... because we would most probably have unsuccessful loads, due to several threads attempting to load the same key
    assertThat("Miss count is greater or equal to load count",
            stats.getMissCount() >= stats.getLoadCount());

    // ... because we would have misses caused by invalidations from the server as well
    assertThat("Miss count is greater than invalidations by checks",
            stats.getInvalidationByChecksCount() < stats.getMissCount());
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
          throw new RuntimeException("Caught an exception while running the test", e);
        } finally {
          latch.countDown();
        }
      });
    }

    // wait for all threads to complete
    latch.await();

    CacheStats stats = testCache.getStats();

    // ... because a command execution should either result in hit or miss
    assertEquals(threadCount * iterations, stats.getMissCount() + stats.getHitCount());

    // ... because all set commands are not cachable
    assertEquals(threadCount * iterations, stats.getNonCacheableCount());

    // ... because we would most probably have unsuccessful loads, due to several threads attempting to load the same key
    assertThat("Miss count is greater or equal to load count",
            stats.getMissCount() >= stats.getLoadCount());

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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), testCache)) {
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache)) {
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
