package redis.clients.jedis.providers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.HealthCheckStrategy;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.SwitchReason;
import redis.clients.jedis.mcf.ProbingPolicy.BuiltIn;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @see MultiClusterPooledConnectionProvider
 */
public class MultiClusterPooledConnectionProviderTest {

  private final EndpointConfig endpointStandalone0 = HostAndPorts.getRedisEndpoint("standalone0");
  private final EndpointConfig endpointStandalone1 = HostAndPorts.getRedisEndpoint("standalone1");

  private MultiClusterPooledConnectionProvider provider;

  @BeforeEach
  public void setUp() {

    ClusterConfig[] clusterConfigs = new ClusterConfig[2];
    clusterConfigs[0] = ClusterConfig.builder(endpointStandalone0.getHostAndPort(),
      endpointStandalone0.getClientConfigBuilder().build()).weight(0.5f).build();
    clusterConfigs[1] = ClusterConfig.builder(endpointStandalone1.getHostAndPort(),
      endpointStandalone1.getClientConfigBuilder().build()).weight(0.3f).build();

    provider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(clusterConfigs).build());
  }

  @AfterEach
  public void destroy() {
    provider.close();
    provider = null;
  }

  @Test
  public void testCircuitBreakerForcedTransitions() {

    CircuitBreaker circuitBreaker = provider.getClusterCircuitBreaker();
    circuitBreaker.getState();

    if (CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState()))
      circuitBreaker.transitionToClosedState();

    circuitBreaker.transitionToForcedOpenState();
    assertEquals(CircuitBreaker.State.FORCED_OPEN, circuitBreaker.getState());

    circuitBreaker.transitionToClosedState();
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  public void testIterateActiveCluster() throws InterruptedException {
    waitForClustersToGetHealthy(provider.getCluster(endpointStandalone0.getHostAndPort()),
      provider.getCluster(endpointStandalone1.getHostAndPort()));

    Endpoint e2 = provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK);
    assertEquals(endpointStandalone1.getHostAndPort(), e2);
  }

  @Test
  public void testIterateActiveClusterOutOfRange() {
    waitForClustersToGetHealthy(provider.getCluster(endpointStandalone0.getHostAndPort()),
      provider.getCluster(endpointStandalone1.getHostAndPort()));

    provider.setActiveCluster(endpointStandalone0.getHostAndPort());
    provider.getCluster().setDisabled(true);

    Endpoint e2 = provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK);
    provider.getCluster().setDisabled(true);

    assertEquals(endpointStandalone1.getHostAndPort(), e2);
    // Should throw an exception
    assertThrows(JedisConnectionException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK));
  }

  @Test
  public void testCanIterateOnceMore() {
    waitForClustersToGetHealthy(provider.getCluster(endpointStandalone0.getHostAndPort()),
      provider.getCluster(endpointStandalone1.getHostAndPort()));

    provider.setActiveCluster(endpointStandalone0.getHostAndPort());
    provider.getCluster().setDisabled(true);
    provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK);

    assertFalse(provider.canIterateOnceMore());
  }

  private void waitForClustersToGetHealthy(Cluster... clusters) {
    Awaitility.await().pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
        .atMost(Durations.TWO_SECONDS)
        .until(() -> Arrays.stream(clusters).allMatch(Cluster::isHealthy));
  }

  @Test
  public void testRunClusterFailoverPostProcessor() {
    ClusterConfig[] clusterConfigs = new ClusterConfig[2];
    clusterConfigs[0] = ClusterConfig
        .builder(new HostAndPort("purposefully-incorrect", 0000),
          DefaultJedisClientConfig.builder().build())
        .weight(0.5f).healthCheckEnabled(false).build();
    clusterConfigs[1] = ClusterConfig
        .builder(new HostAndPort("purposefully-incorrect", 0001),
          DefaultJedisClientConfig.builder().build())
        .weight(0.4f).healthCheckEnabled(false).build();

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clusterConfigs);

    // Configures a single failed command to trigger an open circuit on the next subsequent failure
    builder.circuitBreakerSlidingWindowSize(1);
    builder.circuitBreakerSlidingWindowMinCalls(1);

    AtomicBoolean isValidTest = new AtomicBoolean(false);

    MultiClusterPooledConnectionProvider localProvider = new MultiClusterPooledConnectionProvider(
        builder.build());
    localProvider.setClusterSwitchListener(a -> {
      isValidTest.set(true);
    });

    try (UnifiedJedis jedis = new UnifiedJedis(localProvider)) {

      // This will fail due to unable to connect and open the circuit which will trigger the post
      // processor
      try {
        jedis.get("foo");
      } catch (Exception e) {
      }

    }

    assertTrue(isValidTest.get());
  }

  @Test
  public void testSetActiveMultiClusterIndexEqualsZero() {
    assertThrows(JedisValidationException.class, () -> provider.setActiveCluster(null)); // Should
                                                                                         // throw an
                                                                                         // exception
  }

  @Test
  public void testSetActiveMultiClusterIndexLessThanZero() {
    assertThrows(JedisValidationException.class, () -> provider.setActiveCluster(null)); // Should
                                                                                         // throw an
                                                                                         // exception
  }

  @Test
  public void testSetActiveMultiClusterIndexOutOfRange() {
    assertThrows(JedisValidationException.class, () -> provider.setActiveCluster(new Endpoint() {
      @Override
      public String getHost() {
        return "purposefully-incorrect";
      }

      @Override
      public int getPort() {
        return 0000;
      }
    })); // Should throw an exception
  }

  @Test
  public void testConnectionPoolConfigApplied() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(8);
    poolConfig.setMaxIdle(4);
    poolConfig.setMinIdle(1);
    ClusterConfig[] clusterConfigs = new ClusterConfig[2];
    clusterConfigs[0] = new ClusterConfig(endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
    clusterConfigs[1] = new ClusterConfig(endpointStandalone1.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
    try (
        MultiClusterPooledConnectionProvider customProvider = new MultiClusterPooledConnectionProvider(
            new MultiClusterClientConfig.Builder(clusterConfigs).build())) {
      MultiClusterPooledConnectionProvider.Cluster activeCluster = customProvider.getCluster();
      ConnectionPool connectionPool = activeCluster.getConnectionPool();
      assertEquals(8, connectionPool.getMaxTotal());
      assertEquals(4, connectionPool.getMaxIdle());
      assertEquals(1, connectionPool.getMinIdle());
    }
  }

  @Test
  @Timeout(5)
  void testHealthChecksStopAfterProviderClose() throws InterruptedException {
    AtomicInteger healthCheckCount = new AtomicInteger(0);

    // Custom strategy that counts health checks
    HealthCheckStrategy countingStrategy = new redis.clients.jedis.mcf.TestHealthCheckStrategy(
        redis.clients.jedis.mcf.HealthCheckStrategy.Config.builder().interval(5).timeout(50)
            .policy(BuiltIn.ANY_SUCCESS).build(),
        e -> {
          healthCheckCount.incrementAndGet();
          return HealthStatus.HEALTHY;
        });

    // Create new provider with health check strategy (don't use the setUp() provider)
    ClusterConfig config = ClusterConfig
        .builder(endpointStandalone0.getHostAndPort(),
          endpointStandalone0.getClientConfigBuilder().build())
        .healthCheckStrategy(countingStrategy).build();

    MultiClusterPooledConnectionProvider testProvider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(Collections.singletonList(config)).build());

    try {
      // Wait for some health checks to occur
      Awaitility.await().atMost(Durations.ONE_SECOND).until(() -> healthCheckCount.get() > 2);

      int checksBeforeClose = healthCheckCount.get();

      // Close provider
      testProvider.close();

      // Wait longer than health check interval
      Thread.sleep(100);

      int checksAfterClose = healthCheckCount.get();

      // Health check count should not increase after close
      assertEquals(checksBeforeClose, checksAfterClose,
        "Health checks should stop after provider is closed");

    } finally {
      // Ensure cleanup even if test fails
      testProvider.close();
    }
  }
}
