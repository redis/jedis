package redis.clients.jedis.mcf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class MultiClusterDynamicEndpointUnitTest {

    private MultiClusterPooledConnectionProvider provider;
    private JedisClientConfig clientConfig;
    private final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("standalone0");
    private final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("standalone1");

    @BeforeEach
    void setUp() {
        clientConfig = DefaultJedisClientConfig.builder().build();

        // Create initial provider with endpoint1
        ClusterConfig initialConfig = createClusterConfig(endpoint1.getHostAndPort(), 1.0f);

        MultiClusterClientConfig multiConfig = new MultiClusterClientConfig.Builder(
            new ClusterConfig[] { initialConfig }).build();

        provider = new MultiClusterPooledConnectionProvider(multiConfig);
    }

    // Helper method to create cluster configurations
    private ClusterConfig createClusterConfig(HostAndPort hostAndPort, float weight) {
        // Disable health check for unit tests to avoid real connections
        return ClusterConfig.builder(hostAndPort, clientConfig).weight(weight).healthCheckEnabled(false).build();
    }

    @Test
    void testAddNewCluster() {
        ClusterConfig newConfig = createClusterConfig(endpoint2.getHostAndPort(), 2.0f);

        // Should not throw exception
        assertDoesNotThrow(() -> provider.add(newConfig));

        // Verify the cluster was added by checking it can be retrieved
        assertNotNull(provider.getCluster(endpoint2.getHostAndPort()));
    }

    @Test
    void testAddDuplicateCluster() {
        ClusterConfig duplicateConfig = createClusterConfig(endpoint1.getHostAndPort(), 2.0f);

        // Should throw validation exception for duplicate endpoint
        assertThrows(JedisValidationException.class, () -> provider.add(duplicateConfig));
    }

    @Test
    void testAddNullClusterConfig() {
        // Should throw validation exception for null config
        assertThrows(JedisValidationException.class, () -> provider.add(null));
    }

    @Test
    void testRemoveExistingCluster() {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.ping()).thenReturn(true);

        try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool(mockConnection)) {
            // Create initial provider with endpoint1
            ClusterConfig clusterConfig1 = createClusterConfig(endpoint1.getHostAndPort(), 1.0f);

            MultiClusterClientConfig multiConfig = MultiClusterClientConfig
                .builder(new ClusterConfig[] { clusterConfig1 }).build();

            try (MultiClusterPooledConnectionProvider providerWithMockedPool = new MultiClusterPooledConnectionProvider(
                multiConfig)) {

                // Add endpoint2 as second cluster
                ClusterConfig newConfig = createClusterConfig(endpoint2.getHostAndPort(), 2.0f);
                providerWithMockedPool.add(newConfig);

                // Now remove endpoint1 (original cluster)
                assertDoesNotThrow(() -> providerWithMockedPool.remove(endpoint1.getHostAndPort()));

                // Verify endpoint1 was removed
                assertNull(providerWithMockedPool.getCluster(endpoint1.getHostAndPort()));
                // Verify endpoint2 still exists
                assertNotNull(providerWithMockedPool.getCluster(endpoint2.getHostAndPort()));
            }
        }
    }

    private MockedConstruction<TrackingConnectionPool> mockPool(Connection mockConnection) {
        return mockConstruction(TrackingConnectionPool.class, (mock, context) -> {
            when(mock.getResource()).thenReturn(mockConnection);
            doNothing().when(mock).close();
        });
    }

    @Test
    void testRemoveNonExistentCluster() {
        HostAndPort nonExistentEndpoint = new HostAndPort("localhost", 9999);

        // Should throw validation exception for non-existent endpoint
        assertThrows(JedisValidationException.class, () -> provider.remove(nonExistentEndpoint));
    }

    @Test
    void testRemoveLastRemainingCluster() {
        // Should throw validation exception when trying to remove the last cluster
        assertThrows(JedisValidationException.class, () -> provider.remove(endpoint1.getHostAndPort()));
    }

    @Test
    void testRemoveNullEndpoint() {
        // Should throw validation exception for null endpoint
        assertThrows(JedisValidationException.class, () -> provider.remove(null));
    }

    @Test
    void testAddAndRemoveMultipleClusters() {
        // Add endpoint2 as second cluster
        ClusterConfig config2 = createClusterConfig(endpoint2.getHostAndPort(), 2.0f);

        // Create a third endpoint for this test
        HostAndPort endpoint3 = new HostAndPort("localhost", 6381);
        ClusterConfig config3 = createClusterConfig(endpoint3, 3.0f);

        provider.add(config2);
        provider.add(config3);

        // Verify all clusters exist
        assertNotNull(provider.getCluster(endpoint1.getHostAndPort()));
        assertNotNull(provider.getCluster(endpoint2.getHostAndPort()));
        assertNotNull(provider.getCluster(endpoint3));

        // Remove endpoint2
        provider.remove(endpoint2.getHostAndPort());

        // Verify correct cluster was removed
        assertNull(provider.getCluster(endpoint2.getHostAndPort()));
        assertNotNull(provider.getCluster(endpoint1.getHostAndPort()));
        assertNotNull(provider.getCluster(endpoint3));
    }

    @Test
    void testActiveClusterHandlingOnAdd() {
        // The initial cluster should be active
        assertNotNull(provider.getCluster());

        // Add endpoint2 with higher weight
        ClusterConfig newConfig = createClusterConfig(endpoint2.getHostAndPort(), 5.0f);
        provider.add(newConfig);

        // Active cluster should still be valid (implementation may or may not switch)
        assertNotNull(provider.getCluster());
    }

    @Test
    void testActiveClusterHandlingOnRemove() {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.ping()).thenReturn(true);

        try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool(mockConnection)) {
            // Create initial provider with endpoint1
            ClusterConfig clusterConfig1 = createClusterConfig(endpoint1.getHostAndPort(), 1.0f);

            MultiClusterClientConfig multiConfig = MultiClusterClientConfig
                .builder(new ClusterConfig[] { clusterConfig1 }).build();

            try (MultiClusterPooledConnectionProvider providerWithMockedPool = new MultiClusterPooledConnectionProvider(
                multiConfig)) {

                // Add endpoint2 as second cluster
                ClusterConfig newConfig = createClusterConfig(endpoint2.getHostAndPort(), 2.0f);
                providerWithMockedPool.add(newConfig);

                // Get current active cluster
                Object initialActiveCluster = providerWithMockedPool.getCluster();
                assertNotNull(initialActiveCluster);

                // Remove endpoint1 (original cluster, might be active)
                providerWithMockedPool.remove(endpoint1.getHostAndPort());

                // Should still have an active cluster
                assertNotNull(providerWithMockedPool.getCluster());
            }
        }
    }
}
