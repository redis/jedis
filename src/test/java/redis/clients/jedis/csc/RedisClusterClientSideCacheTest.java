package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.util.RedisVersionCondition;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
@ResourceLock("stable-cluster")
public class RedisClusterClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  private static final Set<HostAndPort> hnp = new HashSet<>(HostAndPorts.getStableClusterServers());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("cluster").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(hnp.iterator().next(), clientConfig.get());

  @Override
  protected RedisClusterClient createRegularJedis() {
    return RedisClusterClient.builder()
        .nodes(hnp)
        .clientConfig(clientConfig.get())
        .build();
  }

  @Override
  protected RedisClusterClient createCachedJedis(CacheConfig cacheConfig) {
    return RedisClusterClient.builder()
        .nodes(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(cacheConfig)
        .build();
  }

}
