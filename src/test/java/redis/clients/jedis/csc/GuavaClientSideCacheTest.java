package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.util.JedisURIHelper;

public class GuavaClientSideCacheTest extends ClientSideCacheTestBase {

  @Test
  public void simple() {
    GuavaClientSideCache guava = GuavaClientSideCache.builder().maximumSize(10).ttl(10).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), guava)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertEquals(null, jedis.get("foo"));
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
      assertEquals(null, jedis.get("foo"));
      assertEquals(0, guava.size());
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
