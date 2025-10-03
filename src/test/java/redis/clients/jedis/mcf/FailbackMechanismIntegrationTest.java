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
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDatabaseConfig;

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
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(false) // Disabled
              .failbackCheckInterval(100) // Short interval for testing
              .build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster2 unhealthy to force failover to cluster1
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster1 (only healthy option)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster2 healthy again (higher weight - would normally trigger failback)
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait longer than failback interval
        // Should still be on cluster1 since failback is disabled
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create clusters with different weights
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f) // Lower weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(100) // Short interval for testing
              .gracePeriod(100).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (lower weight, but only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster1 healthy again
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval + some buffer
        // Should have failed back to cluster1 (higher weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testNoFailbackToLowerWeightCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create three clusters with different weights to properly test no failback to lower weight
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f) // Lowest weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Medium weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster3 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2, cluster3 })
              .failbackSupported(true).failbackCheckInterval(100).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster3 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make cluster3 unhealthy to force failover to cluster2 (medium weight)
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (highest weight among healthy clusters)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster1 (lowest weight) healthy - this should NOT trigger failback
        // since we don't failback to lower weight clusters
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval
        // Should still be on cluster2 (no failback to lower weight cluster1)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightClusterImmediately() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(100).gracePeriod(50).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster1 healthy again
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check
        // Should have failed back to cluster1 immediately (higher weight, no stability period
        // required)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testUnhealthyClusterCancelsFailback() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(200).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster1 unhealthy to force failover to cluster2
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster1 healthy again (should trigger failback attempt)
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait a bit
        Thread.sleep(100);

        // Make cluster1 unhealthy again before failback completes
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Wait past the original failback interval
        // Should still be on cluster2 (failback was cancelled due to cluster1 becoming unhealthy)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }

  @Test
  void testMultipleClusterFailbackPriority() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lowest
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Medium
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster3 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2, cluster3 })
              .failbackSupported(true).failbackCheckInterval(100).gracePeriod(100).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster3 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make cluster3 unhealthy to force failover to cluster2 (next highest weight)
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (highest weight among healthy clusters)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster3 healthy again
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback
        // Should fail back to cluster3 (highest weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint3) == provider.getDatabase());
      }
    }
  }

  @Test
  void testGracePeriodDisablesClusterOnUnhealthy() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(100).gracePeriod(200) // 200ms grace
                                                           // period
              .build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Now make cluster2 unhealthy - it should be disabled for grace period
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to cluster1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Cluster2 should be in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());
      }
    }
  }

  @Test
  void testGracePeriodReEnablesClusterAfterPeriod() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(50) // Short interval for testing
              .gracePeriod(100) // Short grace period for testing
              .build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster2 unhealthy to start grace period and force failover
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to cluster1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Cluster2 should be in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());

        // Make cluster2 healthy again while it's still in grace period
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Should still be on cluster1 because cluster2 is in grace period
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Wait for grace period to expire
        // Cluster2 should no longer be in grace period
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> !provider.getDatabase(endpoint2).isInGracePeriod());

        // Wait for failback check to run
        // Should now failback to cluster2 (higher weight) since grace period has expired
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }
}
