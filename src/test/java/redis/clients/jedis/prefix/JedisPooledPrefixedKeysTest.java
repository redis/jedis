package redis.clients.jedis.prefix;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

@Tag("integration")
@ResourceLock("standalone1")
public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest<RedisClient> {

  private static final EndpointConfig ENDPOINT = HostAndPorts.getRedisEndpoint("standalone1");

  @Override
  RedisClient nonPrefixingJedis() {
    return RedisClient.builder()
        .hostAndPort(ENDPOINT.getHostAndPort())
        .clientConfig(ENDPOINT.getClientConfigBuilder().timeoutMillis(500).build())
        .build();
  }
}
