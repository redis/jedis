package redis.clients.jedis.prefix;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisPooled;

public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest<JedisPooled> {

  private static final EndpointConfig ENDPOINT = HostAndPorts.getRedisEndpoint("standalone1");

  @Override
  JedisPooled nonPrefixingJedis() {
    return new JedisPooled(ENDPOINT.getHostAndPort(), ENDPOINT.getClientConfigBuilder().timeoutMillis(500).build());
  }
}
