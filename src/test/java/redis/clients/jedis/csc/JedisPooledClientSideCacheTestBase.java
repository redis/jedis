package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ClientKillParams;

public class JedisPooledClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  protected Jedis control;

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
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
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
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void cacheNotEmptyTest() {
    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(map),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
    }
  }

  @Test
  public void cacheUsedTest() {
    HashMap<CacheKey, CacheEntry> map = new HashMap<>();
    Cache cache = new TestCache(map);
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache,
        singleConnectionPoolConfig.get())) {
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
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), new TestCache(),
        singleConnectionPoolConfig.get())) {
      jedis.set("a", "AA");
      jedis.set("b", "BB");
      jedis.set("c", "CC");

      List<String> expected = Arrays.asList("AA", "BB", "CC");

      List<String> reply1 = jedis.mget("a", "b", "c");
      List<String> reply2 = jedis.mget("a", "b", "c");

      assertEquals(expected, reply1);
      assertEquals(expected, reply2);
      assertEquals(reply1, reply2);
      assertNotSame(reply1, reply2);
    }
  }

  @Test
  public void invalidationTest() {
    Cache cache = new TestCache();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache,
        singleConnectionPoolConfig.get())) {
      jedis.set("1", "one");
      jedis.set("2", "two");
      jedis.set("3", "three");

      assertEquals(0, cache.getSize());
      assertEquals(0, cache.getStats().getInvalidationCount());

      List<String> reply1 = jedis.mget("1", "2", "3");
      assertEquals(Arrays.asList("one", "two", "three"), reply1);
      assertEquals(1, cache.getSize());
      assertEquals(0, cache.getStats().getInvalidationCount());

      jedis.set("1", "new-one");
      List<String> reply2 = jedis.mget("1", "2", "3");
      assertEquals(Arrays.asList("new-one", "two", "three"), reply2);

      assertEquals(1, cache.getSize());
      assertEquals(1, cache.getStats().getInvalidationCount());
    }
  }

  @Test
  public void getNumEntriesTest() {
    Cache cache = new TestCache();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache,
        singleConnectionPoolConfig.get())) {

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
    Cache cache = new TestCache();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig.get(), cache,
        singleConnectionPoolConfig.get())) {

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
  public void clearIfOneDiesTest() {
    Cache cache = new TestCache();
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().resp3().clientName("client-to-kill").build(), cache,
        singleConnectionPoolConfig.get())) {

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

      control.clientKill(ClientKillParams.clientKillParams().type(ClientType.NORMAL).skipMe(ClientKillParams.SkipMe.YES));

      try {
        jedis.get("foo");
      } catch (JedisConnectionException jce) {
        // expected
      }
      assertEquals(0, cache.getSize());
    }
  }
}
