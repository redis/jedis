package redis.clients.jedis.prefix;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.ResourceLock;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClusterClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@ResourceLock(value = Endpoints.CLUSTER_STABLE)
public class RedisClusterPrefixedKeysTest extends PrefixedKeysTest<RedisClusterClient> {

  private static EndpointConfig endpoint;
  private static JedisClientConfig CLIENT_CONFIG;
  private static Set<HostAndPort> NODES;

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint(Endpoints.CLUSTER_STABLE);
    CLIENT_CONFIG = endpoint.getClientConfigBuilder().build();
    NODES = new HashSet<>(endpoint.getHostsAndPorts());
  }

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
