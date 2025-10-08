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
import redis.clients.jedis.MultiDbConfig;

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
      // Create databases with different weights
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(false) // Disabled
              .failbackCheckInterval(100) // Short interval for testing
              .build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database2 unhealthy to force failover to database1
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database1 (only healthy option)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database2 healthy again (higher weight - would normally trigger failback)
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait longer than failback interval
        // Should still be on database1 since failback is disabled
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightDatabase() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create databases with different weights
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f) // Lower weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(100) // Short interval for testing
              .gracePeriod(100).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database1 unhealthy to force failover to database2
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (lower weight, but only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database1 healthy again
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval + some buffer
        // Should have failed back to database1 (higher weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testNoFailbackToLowerWeightDatabase() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      // Create three databases with different weights to properly test no failback to lower weight
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f) // Lowest weight
          .healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f) // Medium weight
          .healthCheckEnabled(false).build();

      MultiDbConfig.DatabaseConfig database3 = MultiDbConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2, database3 })
              .failbackSupported(true).failbackCheckInterval(100).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database3 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make database3 unhealthy to force failover to database2 (medium weight)
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (highest weight among healthy databases)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database1 (lowest weight) healthy - this should NOT trigger failback
        // since we don't failback to lower weight databases
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check interval
        // Should still be on database2 (no failback to lower weight database1)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }

  @Test
  void testFailbackToHigherWeightDatabaseImmediately() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(100).gracePeriod(50).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database1 unhealthy to force failover to database2
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database1 healthy again
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback check
        // Should have failed back to database1 immediately (higher weight, no stability period
        // required)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint1) == provider.getDatabase());
      }
    }
  }

  @Test
  void testUnhealthyDatabaseCancelsFailback() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(200).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database1 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Make database1 unhealthy to force failover to database2
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (only healthy option)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database1 healthy again (should trigger failback attempt)
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait a bit
        Thread.sleep(100);

        // Make database1 unhealthy again before failback completes
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Wait past the original failback interval
        // Should still be on database2 (failback was cancelled due to database1 becoming unhealthy)
        await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }

  @Test
  void testMultipleDatabaseFailbackPriority() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lowest
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Medium
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database3 = MultiDbConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2, database3 })
              .failbackSupported(true).failbackCheckInterval(100).gracePeriod(100).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database3 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());

        // Make database3 unhealthy to force failover to database2 (next highest weight)
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should now be on database2 (highest weight among healthy databases)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database3 healthy again
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint3,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Wait for failback
        // Should fail back to database3 (highest weight)
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint3) == provider.getDatabase());
      }
    }
  }

  @Test
  void testGracePeriodDisablesDatabaseOnUnhealthy() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(100).gracePeriod(200) // 200ms grace
                                                           // period
              .build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Now make database2 unhealthy - it should be disabled for grace period
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to database1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Database2 should be in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());
      }
    }
  }

  @Test
  void testGracePeriodReEnablesDatabaseAfterPeriod() throws InterruptedException {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
      MultiDbConfig.DatabaseConfig database1 = MultiDbConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build(); // Lower
                                                                                            // weight

      MultiDbConfig.DatabaseConfig database2 = MultiDbConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build(); // Higher
                                                                                            // weight

      MultiDbConfig config = new MultiDbConfig.Builder(
          new MultiDbConfig.DatabaseConfig[] { database1, database2 }).failbackSupported(true)
              .failbackCheckInterval(50) // Short interval for testing
              .gracePeriod(100) // Short grace period for testing
              .build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Initially, database2 should be active (highest weight)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());

        // Make database2 unhealthy to start grace period and force failover
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Should failover to database1
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Database2 should be in grace period
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod());

        // Make database2 healthy again while it's still in grace period
        MultiDbConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Should still be on database1 because database2 is in grace period
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());

        // Wait for grace period to expire
        // Database2 should no longer be in grace period
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> !provider.getDatabase(endpoint2).isInGracePeriod());

        // Wait for failback check to run
        // Should now failback to database2 (higher weight) since grace period has expired
        await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(FIFTY_MILLISECONDS)
            .until(() -> provider.getDatabase(endpoint2) == provider.getDatabase());
      }
    }
  }
}
