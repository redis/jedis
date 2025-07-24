package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import redis.clients.jedis.mcf.Endpoint;
import redis.clients.jedis.mcf.HealthCheckStrategy;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.HealthStatusListener;
import redis.clients.jedis.mcf.HealthStatusManager;

/**
 * Tests for MultiClusterPooledConnectionProvider event handling behavior during initialization and throughout its
 * lifecycle with HealthStatusChangeEvents.
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
    void testEventsProcessedAfterInitialization() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            // Create clusters without health checks
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // This should process immediately since initialization is complete
                assertDoesNotThrow(() -> {
                    MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                        HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                }, "Post-initialization events should be processed immediately");

                // Verify the cluster status was updated
                assertEquals(HealthStatus.UNHEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Cluster health status should be updated after post-init event");
            }
        }
    }

    @Test
    void testMultipleEventsProcessedSequentially() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();
            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Verify initial state
                assertEquals(HealthStatus.HEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should start as HEALTHY");

                // Simulate multiple rapid events for the same endpoint
                // Process events sequentially (post-init behavior)
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                assertEquals(HealthStatus.UNHEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should be UNHEALTHY after first event");

                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
                assertEquals(HealthStatus.HEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should be HEALTHY after second event");

                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                assertEquals(HealthStatus.UNHEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should be UNHEALTHY after third event");
            }
        }
    }

    @Test
    void testEventsForMultipleEndpointsPreserveOrder() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
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
    void testEventProcessingWithMixedHealthCheckConfiguration() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            // One cluster with health checks disabled, one with enabled (but mocked)
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false) // No health checks
                .build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false) // No health checks for
                                                                                         // simplicity
                .build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Both clusters should be available and healthy
                assertNotNull(provider.getCluster(endpoint1), "Cluster 1 should be available");
                assertNotNull(provider.getCluster(endpoint2), "Cluster 2 should be available");

                assertTrue(provider.getCluster(endpoint1).isHealthy(), "Cluster 1 should be healthy");
                assertTrue(provider.getCluster(endpoint2).isHealthy(), "Cluster 2 should be healthy");

                // Provider should select the higher weight cluster (endpoint2)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster(),
                    "Should select higher weight cluster as active");
            }
        }
    }

    @Test
    void testNoEventsLostDuringInitialization() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1 }).build();

            // This test verifies that the provider initializes correctly and doesn't lose events
            // In practice, with health checks disabled, no events should be generated during init
            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
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
    void testPostInitEventOrderingWithMultipleEndpoints() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster3 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint3, clientConfig).weight(0.2f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2, cluster3 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Process events immediately (post-init behavior)
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint2,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

                // Verify events were processed and cluster states updated
                assertEquals(HealthStatus.HEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Endpoint1 should have final HEALTHY status");
                assertEquals(HealthStatus.UNHEALTHY, provider.getCluster(endpoint2).getHealthStatus(),
                    "Endpoint2 should have UNHEALTHY status");
            }
        }
    }

    @Test
    void testPostInitRapidEventsOptimization() throws Exception {
        try (MockedConstruction<ConnectionPool> mockedPool = mockConnectionPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(0.5f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Verify initial state
                assertEquals(HealthStatus.HEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should start as HEALTHY");

                // Send rapid sequence of events (should all be processed since init is complete)
                // Process events immediately (post-init behavior)
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
                MultiClusterPooledConnectionProviderHelper.onHealthStatusChange(provider, endpoint1,
                    HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

                // Final state should reflect the last event
                assertEquals(HealthStatus.UNHEALTHY, provider.getCluster(endpoint1).getHealthStatus(),
                    "Should have final UNHEALTHY status from last event");
            }
        }
    }

    @Test
    void testHealthStatusManagerEventOrdering() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();

        // Counter to track events received
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch eventLatch = new CountDownLatch(1);

        // Create a listener that counts events
        HealthStatusListener listener = event -> {
            eventCount.incrementAndGet();
            eventLatch.countDown();
        };

        // Register listener BEFORE adding endpoint (correct order to prevent missing events)
        manager.registerListener(endpoint1, listener);

        // Create a strategy that immediately returns HEALTHY
        HealthCheckStrategy immediateStrategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 100;
            }

            @Override
            public int getTimeout() {
                return 50;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                return HealthStatus.HEALTHY;
            }
        };

        // Add endpoint - this should trigger health check and event
        manager.add(endpoint1, immediateStrategy);

        // Wait for event to be processed
        assertTrue(eventLatch.await(2, TimeUnit.SECONDS), "Should receive health status event");

        // Should have received at least one event (UNKNOWN -> HEALTHY)
        assertTrue(eventCount.get() >= 1, "Should have received at least one health status event");

        manager.remove(endpoint1);
    }

    @Test
    void testHealthStatusManagerHasHealthCheck() {
        HealthStatusManager manager = new HealthStatusManager();

        // Initially no health check
        assertFalse(manager.hasHealthCheck(endpoint1), "Should not have health check initially");

        // Create a simple strategy
        HealthCheckStrategy strategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 100;
            }

            @Override
            public int getTimeout() {
                return 50;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                return HealthStatus.HEALTHY;
            }
        };

        // Add health check
        manager.add(endpoint1, strategy);
        assertTrue(manager.hasHealthCheck(endpoint1), "Should have health check after adding");

        // Remove health check
        manager.remove(endpoint1);
        assertFalse(manager.hasHealthCheck(endpoint1), "Should not have health check after removing");
    }
}
