package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.mcf.ProbePolicy.BuiltIn;
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
      return new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(500)
          .timeout(500).numProbes(1).policy(BuiltIn.ANY_SUCCESS).build(), (endpoint) -> {
            // Create connection per health check to avoid resource leak
            try (UnifiedJedis pinger = new UnifiedJedis(hostAndPort, jedisClientConfig)) {
              String result = pinger.ping();
              return "PONG".equals(result) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
            } catch (Exception e) {
              return HealthStatus.UNHEALTHY;
            }
          });
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

  // ========== Probe Logic Integration Tests ==========
  @Test
  public void testProbingLogic_RealHealthCheckWithProbes() throws InterruptedException {
    // Create a strategy that fails first few times then succeeds
    AtomicInteger attemptCount = new AtomicInteger(0);

    StrategySupplier strategySupplier = (hostAndPort, jedisClientConfig) -> {
      // Fast interval, short timeout, 3 probes, short delay
      return new TestHealthCheckStrategy(100, 50, 3, BuiltIn.ANY_SUCCESS, 20, (endpoint) -> {
        int attempt = attemptCount.incrementAndGet();
        if (attempt <= 2) {
          // First 2 attempts fail
          throw new RuntimeException("Simulated failure on attempt " + attempt);
        }
        // Third attempt succeeds - do actual health check
        try (UnifiedJedis jedis = new UnifiedJedis(hostAndPort, jedisClientConfig)) {
          String result = jedis.echo("HealthCheck");
          return "HealthCheck".equals(result) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        } catch (Exception e) {
          return HealthStatus.UNHEALTHY;
        }
      });
    };

    CountDownLatch healthyLatch = new CountDownLatch(1);
    HealthStatusManager manager = new HealthStatusManager();

    // Register listener to detect when health becomes HEALTHY
    manager.registerListener(endpoint1.getHostAndPort(), event -> {
      if (event.getNewStatus() == HealthStatus.HEALTHY) {
        healthyLatch.countDown();
      }
    });

    // Add health check with strategy
    HealthCheck healthCheck = manager.add(endpoint1.getHostAndPort(),
      strategySupplier.get(endpoint1.getHostAndPort(), clientConfig));

    try {
      // Wait for health check to eventually succeed after probes
      assertTrue(healthyLatch.await(5, TimeUnit.SECONDS),
        "Health check should succeed after probes");

      assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus());

      // Verify that multiple attempts were made (should be 3: 2 failures + 1 success)
      assertTrue(attemptCount.get() >= 3,
        "Should have made at least 3 probes, but made: " + attemptCount.get());

    } finally {
      manager.remove(endpoint1.getHostAndPort());
    }
  }

  @Test
  public void testProbingLogic_ExhaustProbesAndStayUnhealthy() throws InterruptedException {
    // Create a strategy that always fails
    AtomicInteger attemptCount = new AtomicInteger(0);

    StrategySupplier alwaysFailSupplier = (hostAndPort, jedisClientConfig) -> {
      // Fast interval, short timeout, 3 probes, short delay
      return new TestHealthCheckStrategy(100, 50, 3, BuiltIn.ANY_SUCCESS, 10, (endpoint) -> {
        attemptCount.incrementAndGet();
        throw new RuntimeException("Always fails");
      });
    };

    CountDownLatch unhealthyLatch = new CountDownLatch(1);
    HealthStatusManager manager = new HealthStatusManager();

    // Register listener to detect when health becomes UNHEALTHY
    manager.registerListener(endpoint1.getHostAndPort(), event -> {
      if (event.getNewStatus() == HealthStatus.UNHEALTHY) {
        unhealthyLatch.countDown();
      }
    });

    // Add health check with always-fail strategy
    HealthCheck healthCheck = manager.add(endpoint1.getHostAndPort(),
      alwaysFailSupplier.get(endpoint1.getHostAndPort(), clientConfig));

    try {
      // Wait for health check to fail after exhausting probes
      assertTrue(unhealthyLatch.await(3, TimeUnit.SECONDS),
        "Health check should become UNHEALTHY after exhausting probes");

      assertEquals(HealthStatus.UNHEALTHY, healthCheck.getStatus());

      // Verify that all attempts were made (should 3 probes)
      assertTrue(attemptCount.get() >= 3,
        "Should have made at least 3 attempts , but made: " + attemptCount.get());

    } finally {
      manager.remove(endpoint1.getHostAndPort());
    }
  }
}
