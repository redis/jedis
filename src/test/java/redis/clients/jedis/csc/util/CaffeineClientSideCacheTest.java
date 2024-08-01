package redis.clients.jedis.csc.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.csc.ClientSideCacheTestBase;

public class CaffeineClientSideCacheTest extends ClientSideCacheTestBase {

  @Test
  public void simple() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new CaffeineClientSideCache())) {
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
        new CaffeineClientSideCache(caffeine), singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertEquals(0, caffeine.estimatedSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, caffeine.estimatedSize());
      control.flushAll();
      assertEquals(1, caffeine.estimatedSize());
      assertNull(jedis.get("foo"));
      assertEquals(0, caffeine.estimatedSize());
      control.set("foo", "bar2");
      assertEquals("bar2", jedis.get("foo"));
      assertEquals(1, caffeine.estimatedSize());
    }

    CacheStats stats = caffeine.stats();
    assertEquals(1L, stats.hitCount());
    assertThat(stats.missCount(), Matchers.greaterThan(0L));
  }

  @Test
  public void maximumSize() {
    final long maxSize = 10;
    final long maxEstimatedSize = 53;
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    Cache caffeine = Caffeine.newBuilder().maximumSize(maxSize).recordStats().build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new CaffeineClientSideCache(caffeine))) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
        assertThat(caffeine.estimatedSize(), Matchers.lessThanOrEqualTo(maxEstimatedSize));
      }
    }
    assertThat(caffeine.stats().evictionCount(), Matchers.greaterThanOrEqualTo(count - maxEstimatedSize));
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
