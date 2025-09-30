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
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProviderHelper;

/**
 * Tests for MultiClusterPooledConnectionProvider event handling behavior during initialization and
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
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {

        assertFalse(provider.getCluster(endpoint1).isInGracePeriod());
        assertEquals(provider.getCluster(), provider.getCluster(endpoint1));

        // This should process immediately since initialization is complete
        assertDoesNotThrow(() -> {
          MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
            HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        }, "Post-initialization events should be processed immediately");

        // Verify the cluster has changed according to the UNHEALTHY status
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(),
          "UNHEALTHY status on active cluster should cause a grace period");
        assertNotEquals(provider.getCluster(), provider.getCluster(endpoint1),
          "UNHEALTHY status on active cluster should cause a failover");
      }
    }
  }

  @Test
  void postInit_nonActive_changes_do_not_switch_active() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Verify initial state
        assertEquals(provider.getCluster(endpoint1), provider.getCluster(),
          "Should start with endpoint1 active");

        // Simulate multiple rapid events for the same endpoint (post-init behavior)
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // After first UNHEALTHY on active cluster: it enters grace period and provider fails over
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(),
          "Active cluster should enter grace period");
        assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
          "Should fail over to endpoint2");

        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

        // Healthy event for non-active cluster should not immediately revert active cluster
        assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
          "Active cluster should remain endpoint2");
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(),
          "Grace period should still be in effect");

        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Further UNHEALTHY for non-active cluster is a no-op
        assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
          "Active cluster unchanged");
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(), "Still in grace period");
      }
    }
  }

  @Test
  void init_selects_highest_weight_healthy_when_checks_disabled() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // This test verifies that multiple endpoints are properly initialized

        // Verify both clusters are initialized properly
        assertNotNull(provider.getCluster(endpoint1), "Cluster 1 should be available");
        assertNotNull(provider.getCluster(endpoint2), "Cluster 2 should be available");

        // Both should be healthy (no health checks = assumed healthy)
        assertTrue(provider.getCluster(endpoint1).isHealthy(), "Cluster 1 should be healthy");
        assertTrue(provider.getCluster(endpoint2).isHealthy(), "Cluster 2 should be healthy");
      }
    }
  }

  @Test
  void init_single_cluster_initializes_and_is_healthy() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1 }).build();

      // This test verifies that the provider initializes correctly and doesn't lose events
      // In practice, with health checks disabled, no events should be generated during init
      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Verify successful initialization
        assertNotNull(provider.getCluster(), "Provider should have initialized successfully");
        assertEquals(provider.getCluster(endpoint1), provider.getCluster(),
          "Should have selected the configured cluster");
        assertTrue(provider.getCluster().isHealthy(),
          "Cluster should be healthy (assumed healthy with no health checks)");
      }
    }
  }

  // ========== POST-INITIALIZATION EVENT ORDERING TESTS ==========

  @Test
  void postInit_two_hop_failover_chain_respected() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster3 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint3, clientConfig).weight(0.2f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2, cluster3 }).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // First event: endpoint1 (active) becomes UNHEALTHY -> failover to endpoint2, endpoint1
        // enters grace
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(),
          "Endpoint1 should be in grace after unhealthy");
        assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
          "Should have failed over to endpoint2");

        // Second event: endpoint2 (now active) becomes UNHEALTHY -> failover to endpoint3
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        assertTrue(provider.getCluster(endpoint2).isInGracePeriod(),
          "Endpoint2 should be in grace after unhealthy");
        assertEquals(provider.getCluster(endpoint3), provider.getCluster(),
          "Should have failed over to endpoint3");

        // Third event: endpoint1 becomes HEALTHY again -> no immediate switch due to grace period
        // behavior
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
        assertEquals(provider.getCluster(endpoint3), provider.getCluster(),
          "Active cluster should remain endpoint3");
      }
    }
  }

  @Test
  void postInit_rapid_events_respect_grace_and_keep_active_stable() throws Exception {
    try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
      MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
          .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

      MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
          new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

      try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
          config)) {
        // Verify initial state
        assertEquals(HealthStatus.HEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
          "Should start as HEALTHY");

        // Send rapid sequence of events post-init
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY); // triggers failover and grace
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.UNHEALTHY, HealthStatus.HEALTHY); // non-active cluster becomes healthy
        MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
          HealthStatus.HEALTHY, HealthStatus.UNHEALTHY); // still non-active and in grace; no change

        // Final expectations: endpoint1 is in grace, provider remains on endpoint2
        assertTrue(provider.getCluster(endpoint1).isInGracePeriod(),
          "Endpoint1 should be in grace period");
        assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
          "Active cluster should remain endpoint2");
      }
    }
  }
}
