package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hamcrest.Matchers;
import org.junit.Test;
import redis.clients.jedis.JedisPooled;

public class GuavaClientSideCacheTest extends ClientSideCacheTestBase {

  @Test
  public void simple() {
    GuavaClientSideCache guava = new GuavaClientSideCache(10);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), guava)) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertEquals(null, jedis.get("foo"));
    }
  }

  @Test
  public void individualCommandsAndThenStats() {

    GuavaClientSideCache guava = new GuavaClientSideCache(10000);

    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), guava,
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertEquals(0, guava.getSize());
      assertEquals("bar", jedis.get("foo")); // cache miss
      assertEquals(1, guava.getSize());
      control.flushAll();
      assertEquals(1, guava.getSize());
      assertEquals(null, jedis.get("foo")); // cache miss
      assertEquals(0, guava.getSize());
      jedis.ping();
      assertEquals(0, guava.getSize());
      assertNull(jedis.get("foo")); // cache miss
      assertEquals(0, guava.getSize());
    }

    assertEquals(0, guava.getStats().getHitCount());
    assertEquals(guava.getStats().getMissCount(), 3);
  }

  @Test
  public void maximumSizeExact() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    GuavaClientSideCache guava = new GuavaClientSideCache(1);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), guava)) {
      assertEquals(0, guava.getSize());
      jedis.get("k1");
      assertEquals(1, guava.getSize());
      assertEquals(0, guava.getStats().getEvictCount());
      jedis.get("k2");
      assertEquals(1, guava.getSize());
      assertEquals(1, guava.getStats().getEvictCount());
    }
  }

  @Test
  public void maximumSize() {
    final int maxSize = 10;
    final int maxEstimatedSize = 40;
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    GuavaClientSideCache guava = new GuavaClientSideCache(maxSize);
    try (JedisPooled jedis = new TestJedisPooled(hnp, clientConfig.get(), guava)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
        assertThat(guava.getSize(), Matchers.lessThanOrEqualTo(maxEstimatedSize));
      }
    }
    assertThat(guava.getStats().getEvictCount(), Matchers.greaterThan((long) count - maxEstimatedSize));
  }

}
