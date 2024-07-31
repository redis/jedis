package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

  @Test
  public void simple() {
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache())) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertEquals(null, jedis.get("foo"));
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
      assertEquals(null, jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
      jedis.ping();
      assertThat(map, Matchers.aMapWithSize(0));
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
      assertEquals(null, jedis.get("foo"));
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
      assertEquals(null, jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
      jedis.ping();
      assertThat(map, Matchers.aMapWithSize(0));
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    int threadCount = 10;
    int iterations = 1000;

    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get())) {
      String finalValue = jedis.set("foo", "0");
    }

    ReentrantLock lock = new ReentrantLock(true);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    // Create the shared mock instance of cache
    TestCache mockedCache = Mockito.spy(new TestCache());

    // Submit multiple threads to perform concurrent operations
    CountDownLatch latch = new CountDownLatch(10);
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), mockedCache)) {
          for (int j = 0; j < iterations; j++) {
            lock.lock();
            try {
              // Simulate continious get and update operations and consume invalidation events meanwhile
              Integer value = new Integer(jedis.get("foo")) + 1;
              jedis.set("foo", value.toString());
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

}
