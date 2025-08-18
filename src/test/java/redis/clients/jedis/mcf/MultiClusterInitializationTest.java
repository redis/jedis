package redis.clients.jedis.mcf;

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
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;

/**
 * Tests for MultiClusterPooledConnectionProvider initialization edge cases
 */
@ExtendWith(MockitoExtension.class)
public class MultiClusterInitializationTest {

    private HostAndPort endpoint1;
    private HostAndPort endpoint2;
    private HostAndPort endpoint3;
    private JedisClientConfig clientConfig;

    @BeforeEach
    void setUp() {
        endpoint1 = new HostAndPort("localhost", 6379);
        endpoint2 = new HostAndPort("localhost", 6380);
        endpoint3 = new HostAndPort("localhost", 6381);
        clientConfig = DefaultJedisClientConfig.builder().build();
    }

    private MockedConstruction<ConnectionPool> mockPool() {
        Connection mockConnection = mock(Connection.class);
        lenient().when(mockConnection.ping()).thenReturn(true);
        return mockConstruction(ConnectionPool.class, (mock, context) -> {
            when(mock.getResource()).thenReturn(mockConnection);
            doNothing().when(mock).close();
        });
    }

    @Test
    void testInitializationWithMixedHealthCheckConfiguration() {
        try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
            // Create clusters with mixed health check configuration
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false) // No health check
                .build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(2.0f)
                .healthCheckStrategySupplier(EchoStrategy.DEFAULT) // With  health check
                .build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Should initialize successfully
                assertNotNull(provider.getCluster());

                // Should select cluster1 (no health check, assumed healthy) or cluster2 based on weight
                // Since cluster2 has higher weight and health checks, it should be selected if healthy
                assertTrue(provider.getCluster() == provider.getCluster(endpoint1)
                    || provider.getCluster() == provider.getCluster(endpoint2));
            }
        }
    }

    @Test
    void testInitializationWithAllHealthChecksDisabled() {
        try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
            // Create clusters with no health checks
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(3.0f) // Higher weight
                .healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Should select cluster2 (highest weight, no health checks)
                assertEquals(provider.getCluster(endpoint2), provider.getCluster());
            }
        }
    }

    @Test
    void testInitializationWithSingleCluster() {
        try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
            MultiClusterClientConfig.ClusterConfig cluster = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Should select the only available cluster
                assertEquals(provider.getCluster(endpoint1), provider.getCluster());
            }
        }
    }

    @Test
    void testErrorHandlingWithNullConfiguration() {
        assertThrows(JedisValidationException.class, () -> {
            new MultiClusterPooledConnectionProvider(null);
        });
    }

    @Test
    void testErrorHandlingWithEmptyClusterArray() {
        assertThrows(JedisValidationException.class, () -> {
            new MultiClusterClientConfig.Builder(new MultiClusterClientConfig.ClusterConfig[0]).build();
        });
    }

    @Test
    void testErrorHandlingWithNullClusterConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MultiClusterClientConfig.Builder(new MultiClusterClientConfig.ClusterConfig[] { null }).build();
        });
    }

    @Test
    void testInitializationWithZeroWeights() {
        try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
            MultiClusterClientConfig.ClusterConfig cluster1 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint1, clientConfig).weight(0.0f) // Zero weight
                .healthCheckEnabled(false).build();

            MultiClusterClientConfig.ClusterConfig cluster2 = MultiClusterClientConfig.ClusterConfig
                .builder(endpoint2, clientConfig).weight(0.0f) // Zero weight
                .healthCheckEnabled(false).build();

            MultiClusterClientConfig config = new MultiClusterClientConfig.Builder(
                new MultiClusterClientConfig.ClusterConfig[] { cluster1, cluster2 }).build();

            try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(config)) {
                // Should still initialize and select one of the clusters
                assertNotNull(provider.getCluster());
            }
        }
    }
}
