package redis.clients.jedis.csc;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.csc.util.AllowAndDenyListWithStringKeys;

public class AllowAndDenyListCacheableTest extends ClientSideCacheTestBase {

  private static CacheConfig createConfig(Cacheable cacheable) {
    return CacheConfig.builder().cacheable(cacheable).cacheClass(TestCache.class).build();
  }

  @Test
  public void none() {
    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(createConfig(new AllowAndDenyListWithStringKeys(null, null, null, null)))
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void whiteListCommand() {
    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(createConfig(new AllowAndDenyListWithStringKeys(singleton(Protocol.Command.GET), null, null, null)))
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void blackListCommand() {
    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(createConfig(new AllowAndDenyListWithStringKeys(null, singleton(Protocol.Command.GET), null, null)))
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, cache.getSize());
    }
  }

  @Test
  public void whiteListKey() {
    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(createConfig(new AllowAndDenyListWithStringKeys(null, null, singleton("foo"), null)))
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      control.set("foo", "bar");
      Cache cache = jedis.getCache();
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, cache.getSize());
    }
  }

  @Test
  public void blackListKey() {
    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(createConfig(new AllowAndDenyListWithStringKeys(null, null, null, singleton("foo"))))
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();
      control.set("foo", "bar");
      assertEquals(0, cache.getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, cache.getSize());
    }
  }
}
