package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static redis.clients.jedis.mcf.MultiDatabaseConnectionProviderHelper.onHealthStatusChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;

@ExtendWith(MockitoExtension.class)
class PeriodicFailbackTest {

  private HostAndPort endpoint1;
  private HostAndPort endpoint2;
  private JedisClientConfig clientConfig;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("localhost", 6379);
    endpoint2 = new HostAndPort("localhost", 6380);
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
  void testPeriodicFailbackCheckWithDisabledCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig cluster1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig cluster2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(100).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Start grace period for cluster2 manually
        provider.getDatabase(endpoint2).setGracePeriod();
        provider.getDatabase(endpoint2).setDisabled(true);

        // Force failover to cluster1 since cluster2 is disabled
        provider.switchToHealthyDatabase(SwitchReason.FORCED, provider.getDatabase(endpoint2));

        // Manually trigger periodic check
        MultiDatabaseConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should still be on cluster1 (cluster2 is in grace period)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckWithHealthyCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig cluster1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig cluster2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(true)
              .failbackCheckInterval(50).gracePeriod(100).build(); // Add
                                                                   // grace
                                                                   // period

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster2 unhealthy to force failover to cluster1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster1 (cluster2 is in grace period)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Verify cluster2 is in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());

        // Make cluster2 healthy again (but it's still in grace period)
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Trigger periodic check immediately - should still be on cluster1
        MultiDatabaseConnectionProviderHelper.periodicFailbackCheck(provider);
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Wait for grace period to expire
        Thread.sleep(150);

        // Trigger periodic check after grace period expires
        MultiDatabaseConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should have failed back to cluster2 (higher weight, grace period expired)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckWithFailbackDisabled() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig cluster1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig cluster2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { cluster1, cluster2 }).failbackSupported(false) // Disabled
              .failbackCheckInterval(50).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster2 unhealthy to force failover to cluster1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster2 healthy again
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for stability period
        Thread.sleep(100);

        // Trigger periodic check
        MultiDatabaseConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should still be on cluster1 (failback disabled)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckSelectsHighestWeightCluster() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      HostAndPort endpoint3 = new HostAndPort("localhost", 6381);

      MultiDbConfig.DatabaseConfig cluster1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig cluster2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig cluster3 = MultiDbConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { cluster1, cluster2, cluster3 })
              .failbackSupported(true).failbackCheckInterval(50).gracePeriod(100).build(); // Add
                                                                                           // grace
                                                                                           // period

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Initially, cluster3 should be active (highest weight: 3.0f vs 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make cluster3 unhealthy to force failover to cluster2 (next highest weight)
        onHealthStatusChange(provider, endpoint3, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster2 (weight 2.0f, higher than cluster1's 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make cluster2 unhealthy to force failover to cluster1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on cluster1 (only healthy cluster left)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make cluster2 and cluster3 healthy again
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
        onHealthStatusChange(provider, endpoint3, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for grace period to expire
        Thread.sleep(150);

        // Trigger periodic check
        MultiDatabaseConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should have failed back to cluster3 (highest weight, grace period expired)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());
      }
    }
  }
}
