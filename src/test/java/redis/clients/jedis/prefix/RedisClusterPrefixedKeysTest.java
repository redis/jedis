package redis.clients.jedis.prefix;

import java.util.stream.Collectors;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClusterClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.parallel.ResourceLock;

@Tag("integration")
@ResourceLock("stable-cluster")
public class RedisClusterPrefixedKeysTest extends PrefixedKeysTest<RedisClusterClient> {

  private static final JedisClientConfig CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("cluster").build();
  private static final Set<HostAndPort> NODES = HostAndPorts.getStableClusterServers().stream().collect(Collectors.toSet());

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
