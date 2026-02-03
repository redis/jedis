package redis.clients.jedis.csc;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.SinceRedisVersion;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.*;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.TestEnvUtil;

import org.junit.jupiter.api.parallel.ResourceLock;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
@ResourceLock(value = Endpoints.CLUSTER_STABLE)
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public class RedisClusterClientSideCacheTest extends UnifiedJedisClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  private static Set<HostAndPort> hnp;

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> endpoint.getClientConfigBuilder().resp3().build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint(Endpoints.CLUSTER_STABLE));

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint(Endpoints.CLUSTER_STABLE);
    hnp = new HashSet<>(endpoint.getHostsAndPorts());
  }

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
