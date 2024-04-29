package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.hash.Hashing;

import java.util.concurrent.TimeUnit;
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

public class GuavaClientSideCacheLibsTest {

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
  public void simple() {
    GuavaClientSideCache guava = GuavaClientSideCache.builder().maximumSize(10).ttl(10)
        .hashFunction(Hashing.farmHashFingerprint64()).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), guava)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void individualCommandsAndThenStats() {

    Cache guava = CacheBuilder.newBuilder().recordStats().build();

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

    CacheStats stats = guava.stats();
    assertEquals(1L, stats.hitCount());
    assertThat(stats.missCount(), Matchers.greaterThan(0L));
  }

  @Test
  public void maximumSizeExact() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    Cache guava = CacheBuilder.newBuilder().maximumSize(1).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new GuavaClientSideCache(guava))) {
      assertEquals(0, guava.size());
      jedis.get("k1");
      assertEquals(1, guava.size());
      assertEquals(0, guava.stats().evictionCount());
      jedis.get("k2");
      assertEquals(1, guava.size());
      assertEquals(1, guava.stats().evictionCount());
    }
  }

  @Test
  public void maximumSize() {
    final long maxSize = 10;
    final long maxEstimatedSize = 40;
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    Cache guava = CacheBuilder.newBuilder().maximumSize(maxSize).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new GuavaClientSideCache(guava))) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
        assertThat(guava.size(), Matchers.lessThanOrEqualTo(maxEstimatedSize));
      }
    }
    assertThat(guava.stats().evictionCount(), Matchers.greaterThan(count - maxEstimatedSize));
  }

  @Test
  public void timeToLive() throws InterruptedException {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    Cache guava = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new GuavaClientSideCache(guava))) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }
    assertThat(guava.size(), Matchers.equalTo((long) count));
    assertThat(guava.stats().evictionCount(), Matchers.equalTo(0L));

    TimeUnit.SECONDS.sleep(2);
    guava.cleanUp();
    assertThat(guava.size(), Matchers.equalTo(0L));
    assertThat(guava.stats().evictionCount(), Matchers.equalTo((long) count));
  }

}
