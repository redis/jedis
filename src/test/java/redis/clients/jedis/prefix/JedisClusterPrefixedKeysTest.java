package redis.clients.jedis.prefix;

import java.util.stream.Collectors;
import java.util.Set;
import org.junit.Test;

import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;

public class JedisClusterPrefixedKeysTest extends PrefixedKeysTest<JedisCluster> {

  private static final JedisClientConfig CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("cluster").build();
  private static final Set<HostAndPort> NODES = HostAndPorts.getStableClusterServers().stream().collect(Collectors.toSet());

  @Override
  JedisCluster nonPrefixingJedis() {
    return new JedisCluster(NODES, CLIENT_CONFIG);
  }

  @Override
  @Test(expected = UnsupportedOperationException.class)
  public void prefixesKeysInTransaction() {
    super.prefixesKeysInTransaction();
  }
}
