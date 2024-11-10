package redis.clients.jedis.csc;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersionRule;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.ClassRule;
import redis.clients.jedis.*;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
public class JedisClusterClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final Set<HostAndPort> hnp = new HashSet<>(HostAndPorts.getStableClusterServers());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("cluster").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

    @ClassRule
    public static RedisVersionRule   versionRule = new RedisVersionRule(hnp.iterator().next(), clientConfig.get());

    @Override
    protected JedisCluster createRegularJedis() {
    return new JedisCluster(hnp, clientConfig.get());
    }

    @Override
        protected JedisCluster createCachedJedis(CacheConfig cacheConfig) {
        return new JedisCluster(hnp, clientConfig.get(), cacheConfig);
    }
}
