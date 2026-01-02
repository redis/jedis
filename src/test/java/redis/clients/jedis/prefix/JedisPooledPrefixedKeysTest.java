package redis.clients.jedis.prefix;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeAll;

@Tag("integration")
public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest<RedisClient> {

  private static EndpointConfig ENDPOINT;

  @BeforeAll
  public static void prepareEndpoint() {
    ENDPOINT = Endpoints.getRedisEndpoint("standalone1");
  }

  @Override
  RedisClient nonPrefixingJedis() {
    return RedisClient.builder()
        .hostAndPort(ENDPOINT.getHostAndPort())
        .clientConfig(ENDPOINT.getClientConfigBuilder().timeoutMillis(500).build())
        .build();
  }
}
