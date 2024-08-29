package redis.clients.jedis.csc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ClientKillParams;

public abstract class JedisPooledClientSideCacheTestBase extends UnifiedJedisClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  @Override
  protected JedisPooled createRegularJedis() {
    return new JedisPooled(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
  }

  @Override
  protected JedisPooled createCachedJedis(CacheConfig cacheConfig) {
    return new JedisPooled(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().resp3().build(), cacheConfig);
  }

  @Test
  public void clearIfOneDiesTest() {
    try (JedisPooled jedis = createCachedJedis(CacheConfig.builder().build())) {
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
