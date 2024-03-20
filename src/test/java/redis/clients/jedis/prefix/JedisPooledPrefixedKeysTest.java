package redis.clients.jedis.prefix;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest<JedisPooled> {

  private static final HostAndPort ADDRESS = HostAndPorts.getRedisServers().get(1);
  private static final JedisClientConfig CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("foobared").build();

  @Override
  JedisPooled nonPrefixingJedis() {
    return new JedisPooled(ADDRESS, CLIENT_CONFIG);
  }
}
