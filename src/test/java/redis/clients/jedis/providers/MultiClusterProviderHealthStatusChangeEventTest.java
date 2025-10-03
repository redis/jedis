package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import redis.clients.jedis.MultiDatabaseConfig;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.MultiDatabaseConnectionProvider;
import redis.clients.jedis.mcf.MultiDatabaseConnectionProviderHelper;

/**
 * Tests for MultiDatabaseConnectionProvider event handling behavior during initialization and
 * throughout its lifecycle with HealthStatusChangeEvents.
 */
@ExtendWith(MockitoExtension.class)
public class MultiClusterProviderHealthStatusChangeEventTest {

  private HostAndPort endpoint1;
  private HostAndPort endpoint2;
  private HostAndPort endpoint3;
  private JedisClientConfig clientConfig;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("localhost", 6879);
    endpoint2 = new HostAndPort("localhost", 6880);
    endpoint3 = new HostAndPort("localhost", 6881);
    clientConfig = DefaultJedisClientConfig.builder().build();
  }

  private MockedConstruction<ConnectionPool> mockConnectionPool() {
    Connection mockConnection = mock(Connection.class);
    lenient().when(mockConnection.ping()).thenReturn(true);
    return mockConstruction(ConnectionPool.class, (mock, context) -> {
      when(mock.getResource()).thenReturn(mockConnection);
      doNothing().when(mock).close();
    });
  }

  @Test
  void postInit_unhealthy_active_sets_grace_and_fails_over() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      // Create clusters without health checks
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {

        assertFalse(provider.getDatabase(endpoint1).isInGracePeriod());
        assertEquals(provider.getDatabase(), provider.getDatabase(endpoint1));

        // This should process immediately since initialization is complete
        assertDoesNotThrow(() -> {
          MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
            HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        }, "Post-initialization events should be processed immediately");

        // Verify the cluster has changed according to the UNHEALTHY status
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(),
          "UNHEALTHY status on active cluster should cause a grace period");
        assertNotEquals(provider.getDatabase(), provider.getDatabase(endpoint1),
          "UNHEALTHY status on active cluster should cause a failover");
      }
    }
  }

  @Test
  void postInit_nonActive_changes_do_not_switch_active() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {
        // Verify initial state
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase(),
          "Should start with endpoint1 active");

        // Simulate multiple rapid events for the same endpoint (post-init behavior)
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // After first UNHEALTHY on active cluster: it enters grace period and provider fails over
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(),
          "Active cluster should enter grace period");
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase(),
          "Should fail over to endpoint2");

        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Healthy event for non-active cluster should not immediately revert active cluster
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase(),
          "Active cluster should remain endpoint2");
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(),
          "Grace period should still be in effect");

        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Further UNHEALTHY for non-active cluster is a no-op
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase(),
          "Active cluster unchanged");
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(), "Still in grace period");
      }
    }
  }

  @Test
  void init_selects_highest_weight_healthy_when_checks_disabled() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {
        // This test verifies that multiple endpoints are properly initialized

        // Verify both clusters are initialized properly
        assertNotNull(provider.getDatabase(endpoint1), "Database 1 should be available");
        assertNotNull(provider.getDatabase(endpoint2), "Database 2 should be available");

        // Both should be healthy (no health checks = assumed healthy)
        assertTrue(provider.getDatabase(endpoint1).isHealthy(), "Database 1 should be healthy");
        assertTrue(provider.getDatabase(endpoint2).isHealthy(), "Database 2 should be healthy");
      }
    }
  }

  @Test
  void init_single_cluster_initializes_and_is_healthy() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1 }).build();

      // This test verifies that the provider initializes correctly and doesn't lose events
      // In practice, with health checks disabled, no events should be generated during init
      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {
        // Verify successful initialization
        assertNotNull(provider.getDatabase(), "Provider should have initialized successfully");
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase(),
          "Should have selected the configured cluster");
        assertTrue(provider.getDatabase().isHealthy(),
          "Database should be healthy (assumed healthy with no health checks)");
      }
    }
  }

  // ========== POST-INITIALIZATION EVENT ORDERING TESTS ==========

  @Test
  void postInit_two_hop_failover_chain_respected() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster3 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint3, clientConfig).weight(0.2f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2, cluster3 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {
        // First event: endpoint1 (active) becomes UNHEALTHY -> failover to endpoint2, endpoint1
        // enters grace
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(),
          "Endpoint1 should be in grace after unhealthy");
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase(),
          "Should have failed over to endpoint2");

        // Second event: endpoint2 (now active) becomes UNHEALTHY -> failover to endpoint3
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        assertTrue(provider.getDatabase(endpoint2).isInGracePeriod(),
          "Endpoint2 should be in grace after unhealthy");
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase(),
          "Should have failed over to endpoint3");

        // Third event: endpoint1 becomes HEALTHY again -> no immediate switch due to grace period
        // behavior
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase(),
          "Active cluster should remain endpoint3");
      }
    }
  }

  @Test
  void postInit_rapid_events_respect_grace_and_keep_active_stable() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
          config)) {
        // Verify initial state
        assertEquals(HealthStatus.HEALTHY, provider.getDatabase(endpoint1).getHealthStatus(),
          "Should start as HEALTHY");

        // Send rapid sequence of events post-init
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY); // triggers failover and grace
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY); // non-active cluster becomes healthy
        MultiDatabaseConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY); // still non-active and in grace; no change

        // Final expectations: endpoint1 is in grace, provider remains on endpoint2
        assertTrue(provider.getDatabase(endpoint1).isInGracePeriod(),
          "Endpoint1 should be in grace period");
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase(),
          "Active cluster should remain endpoint2");
      }
    }
  }
}
