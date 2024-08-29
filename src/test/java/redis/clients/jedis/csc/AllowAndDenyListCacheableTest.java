package redis.clients.jedis.csc;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.csc.util.AllowAndDenyListWithStringKeys;

public class AllowAndDenyListCacheableTest extends ClientSideCacheTestBase {

  private static CacheConfig createConfig(Cacheable cacheable) {
    return CacheConfig.builder().cacheable(cacheable).cacheClass(TestCache.class).build();
  }

  @Test
  public void none() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createConfig(new AllowAndDenyListWithStringKeys(null, null, null, null)), singleConnectionPoolConfig.get())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void whiteListCommand() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createConfig(new AllowAndDenyListWithStringKeys(singleton(Protocol.Command.GET), null, null, null)),
        singleConnectionPoolConfig.get())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void blackListCommand() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createConfig(new AllowAndDenyListWithStringKeys(null, singleton(Protocol.Command.GET), null, null)),
        singleConnectionPoolConfig.get())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, cache.getSize());
    }
  }

  @Test
  public void whiteListKey() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createConfig(new AllowAndDenyListWithStringKeys(null, null, singleton("foo"), null)), singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      Cache cache = jedis.getCache();
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void blackListKey() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createConfig(new AllowAndDenyListWithStringKeys(null, null, null, singleton("foo"))), singleConnectionPoolConfig.get())) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, cache.getSize());
    }
  }
}
