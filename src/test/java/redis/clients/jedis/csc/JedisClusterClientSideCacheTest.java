package redis.clients.jedis.csc;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import today.bonfire.oss.sop.SimpleObjectPoolConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class JedisClusterClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final Set<HostAndPort> hnp = new HashSet<>(HostAndPorts.getStableClusterServers());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("cluster").build();

  private static final Supplier<SimpleObjectPoolConfig> singleConnectionPoolConfig
      = () -> {
    var poolConfig = ConnectionPoolConfig.builder();
    poolConfig.maxPoolSize(1);
    return poolConfig.build();
  };

  @Override
  protected JedisCluster createRegularJedis() {
    return new JedisCluster(hnp, clientConfig.get());
  }

  @Override
  protected JedisCluster createCachedJedis(CacheConfig cacheConfig) {
    return new JedisCluster(hnp, clientConfig.get(), cacheConfig);
  }

}
