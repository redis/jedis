package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hamcrest.Matchers;
import org.junit.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.csc.CacheConfig.CacheType;

public class CaffeineClientSideCacheTest extends ClientSideCacheTestBase {

  @Test
  public void simple() {
    CacheConfig caffeineConfig = new CacheConfig.Builder().cacheType(CacheType.CAFFEINE).build();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), caffeineConfig)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertEquals(null, jedis.get("foo"));
    }
  }

  @Test
  public void individualCommandsAndThenStats() {

    CaffeineClientSideCache caffeine = new CaffeineClientSideCache(100);

    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), caffeine, singleConnectionPoolConfig.get()) {
    }) {
      control.set("foo", "bar");
      assertEquals(0, caffeine.getSize());
      assertEquals("bar", jedis.get("foo")); // cache miss
      assertEquals(1, caffeine.getSize());
      control.flushAll();
      assertEquals(1, caffeine.getSize());
      assertEquals(null, jedis.get("foo")); // cache miss
      assertEquals(0, caffeine.getSize());
      jedis.ping();
      assertEquals(0, caffeine.getSize());
      assertNull(jedis.get("foo")); // cache miss
      assertEquals(0, caffeine.getSize());
    }

    assertEquals(0, caffeine.getStats().getHitCount());
    assertEquals(caffeine.getStats().getMissCount(), 3);
  }

  @Test
  public void maximumSizeExact() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    CaffeineClientSideCache caffeine = new CaffeineClientSideCache(1);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), caffeine)) {
      assertEquals(0, caffeine.getSize());
      jedis.get("k1");
      assertEquals(1, caffeine.getSize());
      assertEquals(0, caffeine.getStats().getEvictCount());
      jedis.get("k2");
      assertEquals(1, caffeine.getSize());
      assertEquals(1, caffeine.getStats().getEvictCount());
    }
  }

  @Test
  public void maximumSize() {
    final int maxSize = 10;
    final int maxEstimatedSize = 10;
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    CaffeineClientSideCache caffeine = new CaffeineClientSideCache(maxSize);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), caffeine)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
        assertThat(caffeine.getSize(), Matchers.lessThanOrEqualTo(maxEstimatedSize));
      }
    }
    assertThat(caffeine.getStats().getEvictCount(), Matchers.greaterThanOrEqualTo((long) count - maxEstimatedSize));
  }

}
