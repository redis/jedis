package redis.clients.jedis.prefix;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClusterClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
public class RedisClusterPrefixedKeysTest extends PrefixedKeysTest<RedisClusterClient> {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-stable");
  private static final JedisClientConfig CLIENT_CONFIG = endpoint.getClientConfigBuilder().build();
  private static final Set<HostAndPort> NODES = new HashSet<>(endpoint.getHostsAndPorts());

  @Override
  RedisClusterClient nonPrefixingJedis() {
    return RedisClusterClient.builder()
        .nodes(NODES)
        .clientConfig(CLIENT_CONFIG)
        .build();
  }

  @Override
  @Test
  public void prefixesKeysInTransaction() {
    assertThrows(UnsupportedOperationException.class, () -> super.prefixesKeysInTransaction());
  }
}
