package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.*;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.util.RedisVersionCondition;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
public class RedisMultiDbClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone0"));

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @Override
  protected UnifiedJedis createRegularJedis() {
    return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().build()).build();
  }

  @Override
  protected MultiDbClient createCachedJedis(CacheConfig cacheConfig) {
    DatabaseConfig dbConfig = DatabaseConfig
        .builder(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build()).build();

    MultiDbConfig multiDbConfig = MultiDbConfig.builder().database(dbConfig).build();

    return MultiDbClient.builder().multiDbConfig(multiDbConfig)
        .clientConfig(endpoint.getClientConfigBuilder().build()).cacheConfig(cacheConfig).build();
  }

  @Test
  public void clearIfOneDiesTest() {
    try (MultiDbClient jedis = createCachedJedis(CacheConfig.builder().build())) {
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

      try (Jedis killer = new Jedis(endpoint.getHostAndPort(),
          endpoint.getClientConfigBuilder().serverDefaultProtocol().build())) {
        killer.clientKill(ClientKillParams.clientKillParams().type(ClientType.NORMAL)
            .skipMe(ClientKillParams.SkipMe.YES));
      }
      jedis.get("foo");

      assertEquals(1, cache.getSize());
    }
  }
}
