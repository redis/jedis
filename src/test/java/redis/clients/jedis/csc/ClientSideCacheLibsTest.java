package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

public class ClientSideCacheLibsTest {

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(1);

  protected Jedis control;

  @Before
  public void setUp() throws Exception {
    control = new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @Test
  public void guavaSimple() {
    GuavaClientSideCache guava = GuavaClientSideCache.builder().maximumSize(10).ttl(10)
        .hashFunction(com.google.common.hash.Hashing.farmHashFingerprint64()).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), guava)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void guavaMore() {

    com.google.common.cache.Cache guava = CacheBuilder.newBuilder().recordStats().build();

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new GuavaClientSideCache(guava),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertEquals(0, guava.size());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, guava.size());
      control.flushAll();
      assertEquals(1, guava.size());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, guava.size());
      jedis.ping();
      assertEquals(0, guava.size());
      assertNull(jedis.get("foo"));
      assertEquals(0, guava.size());
    }

    com.google.common.cache.CacheStats stats = guava.stats();
    assertEquals(1L, stats.hitCount());
    assertThat(stats.missCount(), Matchers.greaterThan(0L));
  }

  @Test
  public void caffeineSimple() {
    CaffeineClientSideCache caffeine = CaffeineClientSideCache.builder().maximumSize(10).ttl(10).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), caffeine)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void caffeineMore() {

    com.github.benmanes.caffeine.cache.Cache caffeine = Caffeine.newBuilder().recordStats().build();

    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new CaffeineClientSideCache(caffeine, new OpenHftCommandHasher()),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertEquals(0, caffeine.estimatedSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, caffeine.estimatedSize());
      control.flushAll();
      assertEquals(1, caffeine.estimatedSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, caffeine.estimatedSize());
      jedis.ping();
      assertEquals(0, caffeine.estimatedSize());
      assertNull(jedis.get("foo"));
      assertEquals(0, caffeine.estimatedSize());
    }

    com.github.benmanes.caffeine.cache.stats.CacheStats stats = caffeine.stats();
    assertEquals(1L, stats.hitCount());
    assertThat(stats.missCount(), Matchers.greaterThan(0L));
  }
}
