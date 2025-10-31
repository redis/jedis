package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static redis.clients.jedis.mcf.MultiDbConnectionProviderHelper.onHealthStatusChange;

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
  private JedisClientConfig databaseConfig;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("localhost", 6379);
    endpoint2 = new HostAndPort("localhost", 6380);
    databaseConfig = DefaultJedisClientConfig.builder().build();
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
  void testPeriodicFailbackCheckWithDisabledDatabase() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, databaseConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, databaseConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(100).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Start grace period for database2 manually
        provider.getDatabase(endpoint2).setGracePeriod();
        provider.getDatabase(endpoint2).setDisabled(true);

        // Force failover to database1 since database2 is disabled
        provider.switchToHealthyDatabase(SwitchReason.FORCED, provider.getDatabase(endpoint2));

        // Manually trigger periodic check
        MultiDbConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should still be on database1 (database2 is in grace period)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckWithHealthyDatabase() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, databaseConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, databaseConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(50).gracePeriod(100).build(); // Add
                                                                   // grace
                                                                   // period

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database2 unhealthy to force failover to database1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database1 (database2 is in grace period)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Verify database2 is in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());

        // Make database2 healthy again (but it's still in grace period)
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Trigger periodic check immediately - should still be on database1
        MultiDbConnectionProviderHelper.periodicFailbackCheck(provider);
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Wait for grace period to expire
        Thread.sleep(150);

        // Trigger periodic check after grace period expires
        MultiDbConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should have failed back to database2 (higher weight, grace period expired)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckWithFailbackDisabled() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, databaseConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, databaseConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(false) // Disabled
              .failbackCheckInterval(50).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight: 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database2 unhealthy to force failover to database1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database2 healthy again
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for stability period
        Thread.sleep(100);

        // Trigger periodic check
        MultiDbConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should still be on database1 (failback disabled)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testPeriodicFailbackCheckSelectsHighestWeightDatabase() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      HostAndPort endpoint3 = new HostAndPort("localhost", 6381);

      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, databaseConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, databaseConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database3 = MultiDbConfig.DatabaseConfig
          .builder(endpoint3, databaseConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2, database3 })
              .failbackSupported(true).failbackCheckInterval(50).gracePeriod(100).build(); // Add
                                                                                           // grace
                                                                                           // period

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database3 should be active (highest weight: 3.0f vs 2.0f vs 1.0f)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make database3 unhealthy to force failover to database2 (next highest weight)
        onHealthStatusChange(provider, endpoint3, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (weight 2.0f, higher than database1's 1.0f)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database2 unhealthy to force failover to database1
        onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database1 (only healthy databases left)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database2 and database3 healthy again
        onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
        onHealthStatusChange(provider, endpoint3, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for grace period to expire
        Thread.sleep(150);

        // Trigger periodic check
        MultiDbConnectionProviderHelper.periodicFailbackCheck(provider);

        // Should have failed back to database3 (highest weight, grace period expired)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());
      }
    }
  }
}
