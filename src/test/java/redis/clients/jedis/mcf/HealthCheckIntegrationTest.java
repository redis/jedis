package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.scenario.RecommendedSettings;

public class HealthCheckIntegrationTest {

  private final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("standalone0");
  private final JedisClientConfig clientConfig = endpoint1.getClientConfigBuilder()
      .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
      .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

  @Test
  public void testDisableHealthCheck() {
    // No health check strategy supplier means health check is disabled
    MultiClusterPooledConnectionProvider customProvider = getMCCF(null);
    try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
      // Verify that the client can connect and execute commands
      String result = customClient.ping();
      assertEquals("PONG", result);
    }
  }

  @Test
  public void testDefaultStrategySupplier() {
    // Create a default strategy supplier that creates EchoStrategy instances
    MultiClusterClientConfig.StrategySupplier defaultSupplier = (hostAndPort,
        jedisClientConfig) -> {
      return new EchoStrategy(hostAndPort, jedisClientConfig);
    };
    MultiClusterPooledConnectionProvider customProvider = getMCCF(defaultSupplier);
    try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
      // Verify that the client can connect and execute commands
      String result = customClient.ping();
      assertEquals("PONG", result);
    }
  }

  @Test
  public void testCustomStrategySupplier() {
    // Create a StrategySupplier that uses the JedisClientConfig when available
    MultiClusterClientConfig.StrategySupplier strategySupplier = (hostAndPort,
        jedisClientConfig) -> {
      return new HealthCheckStrategy() {

        @Override
        public int getInterval() {
          return 500;
        }

        @Override
        public int getTimeout() {
          return 500;
        }

        @Override
        public HealthStatus doHealthCheck(Endpoint endpoint) {
          // Create connection per health check to avoid resource leak
          try (UnifiedJedis pinger = new UnifiedJedis(hostAndPort, jedisClientConfig)) {
            String result = pinger.ping();
            return "PONG".equals(result) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
          } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
          }
        }

      };
    };

    MultiClusterPooledConnectionProvider customProvider = getMCCF(strategySupplier);
    try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
      // Verify that the client can connect and execute commands
      String result = customClient.ping();
      assertEquals("PONG", result);
    }
  }

  private MultiClusterPooledConnectionProvider getMCCF(
      MultiClusterClientConfig.StrategySupplier strategySupplier) {
    Function<ClusterConfig.Builder, ClusterConfig.Builder> modifier = builder -> strategySupplier == null
        ? builder.healthCheckEnabled(false)
        : builder.healthCheckStrategySupplier(strategySupplier);

    List<ClusterConfig> clusterConfigs = Arrays.stream(new EndpointConfig[] { endpoint1 })
        .map(e -> modifier
            .apply(MultiClusterClientConfig.ClusterConfig.builder(e.getHostAndPort(), clientConfig))
            .build())
        .collect(Collectors.toList());

    MultiClusterClientConfig mccf = new MultiClusterClientConfig.Builder(clusterConfigs)
        .retryMaxAttempts(1).retryWaitDuration(1)
        .circuitBreakerSlidingWindowType(SlidingWindowType.COUNT_BASED)
        .circuitBreakerSlidingWindowSize(1).circuitBreakerFailureRateThreshold(100)
        .circuitBreakerSlidingWindowMinCalls(1).build();

    return new MultiClusterPooledConnectionProvider(mccf);
  }
}
