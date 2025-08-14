package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static redis.clients.jedis.providers.MultiClusterPooledConnectionProviderHelper.onHealthStatusChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProviderHelper;

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
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).failbackSupported(true)
                    .failbackCheckInterval(100).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());

                // Start grace period for cluster2 manually
                provider.getCluster(endpoint2).setGracePeriod();
                provider.getCluster(endpoint2).setDisabled(true);

                // Force failover to cluster1 since cluster2 is disabled
                provider.iterateActiveCluster(SwitchReason.FORCED);

                // Manually trigger periodic check
                MultiClusterPooledConnectionProviderHelper.periodicFailbackCheck(provider);

                // Should still be on cluster1 (cluster2 is in grace period)
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());
            }
        }
    }

    @Test
    void testPeriodicFailbackCheckWithHealthyCluster() throws InterruptedException {
        try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).failbackSupported(true)
                    .failbackCheckInterval(50).gracePeriod(100).build(); // Add grace period

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());

                // Make cluster2 unhealthy to force failover to cluster1
                onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

                // Should now be on cluster1 (cluster2 is in grace period)
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());

                // Verify cluster2 is in grace period
                assertTrue(provider.getCluster(endpoint2).isInGracePeriod());

                // Make cluster2 healthy again (but it's still in grace period)
                onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

                // Trigger periodic check immediately - should still be on cluster1
                MultiClusterPooledConnectionProviderHelper.periodicFailbackCheck(provider);
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());

                // Wait for grace period to expire
                Thread.sleep(150);

                // Trigger periodic check after grace period expires
                MultiClusterPooledConnectionProviderHelper.periodicFailbackCheck(provider);

                // Should have failed back to cluster2 (higher weight, grace period expired)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());
            }
        }
    }

    @Test
    void testPeriodicFailbackCheckWithFailbackDisabled() throws InterruptedException {
        try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).failbackSupported(false) // Disabled
                    .failbackCheckInterval(50).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Initially, cluster2 should be active (highest weight: 2.0f vs 1.0f)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());

                // Make cluster2 unhealthy to force failover to cluster1
                onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

                // Should now be on cluster1
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());

                // Make cluster2 healthy again
                onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

                // Wait for stability period
                Thread.sleep(100);

                // Trigger periodic check
                MultiClusterPooledConnectionProviderHelper.periodicFailbackCheck(provider);

                // Should still be on cluster1 (failback disabled)
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());
            }
        }
    }

    @Test
    void testPeriodicFailbackCheckSelectsHighestWeightCluster() throws InterruptedException {
        try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool()) {
            HostAndPort endpoint3 = new HostAndPort("localhost", 6381);

            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster3 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint3, clientConfig).weight(3.0f) // Highest weight
                .healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2, cluster3 }).failbackSupported(true)
                    .failbackCheckInterval(50).gracePeriod(100).build(); // Add grace period

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Initially, cluster3 should be active (highest weight: 3.0f vs 2.0f vs 1.0f)
                assertEquals(provider.getCluster(endpoint3), provider.getCluster());

                // Make cluster3 unhealthy to force failover to cluster2 (next highest weight)
                onHealthStatusChange(provider, endpoint3, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

                // Should now be on cluster2 (weight 2.0f, higher than cluster1's 1.0f)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());

                // Make cluster2 unhealthy to force failover to cluster1
                onHealthStatusChange(provider, endpoint2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

                // Should now be on cluster1 (only healthy cluster left)
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());

                // Make cluster2 and cluster3 healthy again
                onHealthStatusChange(provider, endpoint2, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
                onHealthStatusChange(provider, endpoint3, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);

                // Wait for grace period to expire
                Thread.sleep(150);

                // Trigger periodic check
                MultiClusterPooledConnectionProviderHelper.periodicFailbackCheck(provider);

                // Should have failed back to cluster3 (highest weight, grace period expired)
                assertEquals(provider.getCluster(endpoint3), provider.getCluster());
            }
        }
    }
}
