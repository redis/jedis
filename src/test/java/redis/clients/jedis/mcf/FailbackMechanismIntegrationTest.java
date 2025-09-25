package redis.clients.jedis.mcf;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;

@ExtendWith(MockitoExtension.class)
class FailbackMechanismIntegrationTest {

  private HostAndPort endpoint1;
  private HostAndPort endpoint2;
  private HostAndPort endpoint3;
  private JedisClientConfig clientConfig;
  private static Duration FIFTY_MILLISECONDS = Duration.ofMillis(50);

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("localhost", 6379);
    endpoint2 = new HostAndPort("localhost", 6380);
    endpoint3 = new HostAndPort("localhost", 6381);
    clientConfig = DefaultJedisClientConfig.builder().build();
  }

  private MockedConstruction<TrackingConnectionPool> mockPool() {
    Connection mockConnection = mock(Connection.class);
    lenient().when(mockConnection.ping()).thenReturn(true);
    return mockConstruction(TrackingConnectionPool.class, (mock, context) -> {
      when(mock.getResource()).thenReturn(mockConnection);
      doNothing().when(mock).close();
    });
  }

  @Test
  void testFailbackDisabledDoesNotPerformFailback() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create clusters with different weights
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(false) // Disabled
              .failbackCheckInterval(100) // Short interval for testing
              .build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster2 unhealthy to force failover to cluster1
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster1 (only healthy option)
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Make cluster2 healthy again (higher weight - would normally trigger failback)
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait longer than failback interval
        // Should still be on cluster1 since failback is disabled
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint1) == provider.getCluster());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create clusters with different weights
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(1.0f) // Lower weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(true).failbackCheckInterval(100) // Short interval for testing
              .gracePeriod(100).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (lower weight, but only healthy option)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster1 healthy again
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval + some buffer
        // Should have failed back to cluster1 (higher weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint1) == provider.getCluster());
      }
    }
  }

  @Test
  void testNoFailbackToLowerWeightCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create three clusters with different weights to properly test no failback to lower weight
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f) // Lowest weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Medium weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster3 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2, cluster3 })
              .failbackSupported(true).failbackCheckInterval(100).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster3 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint3), provider.getCluster());

        // Make cluster3 unhealthy to force failover to cluster2 (medium weight)
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (highest weight among healthy clusters)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster1 (lowest weight) healthy - this should NOT trigger failback
        // since we don't failback to lower weight clusters
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval
        // Should still be on cluster2 (no failback to lower weight cluster1)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint2) == provider.getCluster());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightClusterImmediately() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(true).failbackCheckInterval(100).gracePeriod(50).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (only healthy option)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster1 healthy again
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check
        // Should have failed back to cluster1 immediately (higher weight, no stability period
        // required)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint1) == provider.getCluster());
      }
    }
  }

  @Test
  void testUnhealthyClusterCancelsFailback() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(true).failbackCheckInterval(200).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (only healthy option)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster1 healthy again (should trigger failback attempt)
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait a bit
        Thread.sleep(100);

        // Make cluster1 unhealthy again before failback completes
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Wait past the original failback interval
        // Should still be on cluster2 (failback was cancelled due to cluster1 becoming unhealthy)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint2) == provider.getCluster());
      }
    }
  }

  @Test
  void testMultipleClusterFailbackPriority() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lowest
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Medium
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster3 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2, cluster3 })
              .failbackSupported(true).failbackCheckInterval(100).gracePeriod(100).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster3 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint3), provider.getCluster());

        // Make cluster3 unhealthy to force failover to cluster2 (next highest weight)
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (highest weight among healthy clusters)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster3 healthy again
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback
        // Should fail back to cluster3 (highest weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint3) == provider.getCluster());
      }
    }
  }

  @Test
  void testGracePeriodDisablesClusterOnUnhealthy() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(true).failbackCheckInterval(100).gracePeriod(200) // 200ms grace
                                                                                   // period
              .build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Now make cluster2 unhealthy - it should be disabled for grace period
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to cluster1
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Cluster2 should be in grace period
        assertTrue(provider.getCluster(endpoint2).isInGracePeriod());
      }
    }
  }

  @Test
  void testGracePeriodReEnablesClusterAfterPeriod() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 })
              .failbackSupported(true).failbackCheckInterval(50) // Short interval for testing
              .gracePeriod(100) // Short grace period for testing
              .build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getCluster(endpoint2), provider.getCluster());

        // Make cluster2 unhealthy to start grace period and force failover
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to cluster1
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Cluster2 should be in grace period
        assertTrue(provider.getCluster(endpoint2).isInGracePeriod());

        // Make cluster2 healthy again while it's still in grace period
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Should still be on cluster1 because cluster2 is in grace period
        assertEquals(provider.getCluster(endpoint1), provider.getCluster());

        // Wait for grace period to expire
        // Cluster2 should no longer be in grace period
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> !provider.getCluster(endpoint2).isInGracePeriod());

        // Wait for failback check to run
        // Should now failback to cluster2 (higher weight) since grace period has expired
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getCluster(endpoint2) == provider.getCluster());
      }
    }
  }
}
