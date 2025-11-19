package redis.clients.jedis.csc;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ClientKillParams;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class RedisClientSideCacheTestBase extends UnifiedJedisClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  @Override
  protected RedisClient createRegularJedis() {
    return RedisClient.builder()
        .hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().build())
        .build();
  }

  @Override
  protected RedisClient createCachedJedis(CacheConfig cacheConfig) {
    return RedisClient.builder()
        .hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().resp3().build())
        .cacheConfig(cacheConfig)
        .build();
  }

  @Test
  public void clearIfOneDiesTest() {
    try (RedisClient jedis = createCachedJedis(CacheConfig.builder().build())) {
      Cache cache = jedis.getCache();
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

      try (Jedis killer = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build())) {
        killer.clientKill(ClientKillParams.clientKillParams().type(ClientType.NORMAL).skipMe(ClientKillParams.SkipMe.YES));
      }

      try {
        jedis.get("foo");
      } catch (JedisConnectionException jce) {
        // expected
      }
      assertEquals(0, cache.getSize());
    }
  }
}
