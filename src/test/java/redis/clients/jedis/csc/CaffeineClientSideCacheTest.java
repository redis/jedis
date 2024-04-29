package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

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

public class CaffeineClientSideCacheTest {
  
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
    CaffeineClientSideCache caffeine = CaffeineClientSideCache.builder().maximumSize(10).ttl(10).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), caffeine)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }
  
  @Test
  public void individualCommandsAndThenStats() {
    
    Cache caffeine = Caffeine.newBuilder().recordStats().build();
    
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
    
    CacheStats stats = caffeine.stats();
    assertEquals(1L, stats.hitCount());
    assertThat(stats.missCount(), Matchers.greaterThan(0L));
  }

  @Test
  public void maximumSize() {
    final long maxSize = 10;
    final long maxEstimatedSize = 40;
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    Cache caffeine = Caffeine.newBuilder().maximumSize(maxSize).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new CaffeineClientSideCache(caffeine))) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
        assertThat(caffeine.estimatedSize(), Matchers.lessThan(maxEstimatedSize));
      }
    }
    assertThat(caffeine.stats().evictionCount(), Matchers.greaterThan(count - maxEstimatedSize));
  }

  @Test
  public void timeToLive() throws InterruptedException {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    Cache caffeine = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new CaffeineClientSideCache(caffeine))) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }
    assertThat(caffeine.estimatedSize(), Matchers.equalTo((long) count));
    assertThat(caffeine.stats().evictionCount(), Matchers.equalTo(0L));

    TimeUnit.SECONDS.sleep(2);
    caffeine.cleanUp();
    assertThat(caffeine.estimatedSize(), Matchers.equalTo(0L));
    assertThat(caffeine.stats().evictionCount(), Matchers.equalTo((long) count));
  }

}
