package redis.clients.jedis.csc;

import io.redis.test.annotations.SinceRedisVersion;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.*;
import redis.clients.jedis.util.RedisVersionCondition;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
public class RedisClusterClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-stable");

  private static final Set<HostAndPort> hnp = new HashSet<>(endpoint.getHostsAndPorts());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> endpoint.getClientConfigBuilder().resp3().build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(endpoint);

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
