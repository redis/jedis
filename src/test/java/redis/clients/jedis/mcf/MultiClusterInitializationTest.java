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
import redis.clients.jedis.MultiDatabaseConfig;
import redis.clients.jedis.exceptions.JedisValidationException;

/**
 * Tests for MultiDatabaseConnectionProvider initialization edge cases
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
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false) // No health
                                                                                   // check
          .build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(2.0f)
          .healthCheckStrategySupplier(EchoStrategy.DEFAULT) // With
                                                             // health
                                                             // check
          .build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Should initialize successfully
        assertNotNull(provider.getDatabase());

        // Should select cluster1 (no health check, assumed healthy) or cluster2 based on weight
        // Since cluster2 has higher weight and health checks, it should be selected if healthy
        assertTrue(provider.getDatabase() == provider.getDatabase(endpoint1)
            || provider.getDatabase() == provider.getDatabase(endpoint2));
      }
    }
  }

  @Test
  void testInitializationWithAllHealthChecksDisabled() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      // Create clusters with no health checks
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(3.0f) // Higher weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Should select cluster2 (highest weight, no health checks)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationWithSingleCluster() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(1.0f).healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Should select the only available cluster
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testErrorHandlingWithNullConfiguration() {
    assertThrows(JedisValidationException.class, () -> {
      new MultiDatabaseConnectionProvider(null);
    });
  }

  @Test
  void testErrorHandlingWithEmptyClusterArray() {
    assertThrows(JedisValidationException.class, () -> {
      new MultiDatabaseConfig.Builder(new MultiDatabaseConfig.DatabaseConfig[0]).build();
    });
  }

  @Test
  void testErrorHandlingWithNullDatabaseConfig() {
    assertThrows(IllegalArgumentException.class, () -> {
      new MultiDatabaseConfig.Builder(new MultiDatabaseConfig.DatabaseConfig[] { null }).build();
    });
  }

  @Test
  void testInitializationWithZeroWeights() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      MultiDatabaseConfig.DatabaseConfig cluster1 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint1, clientConfig).weight(0.0f) // Zero weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig.DatabaseConfig cluster2 = MultiDatabaseConfig.DatabaseConfig
          .builder(endpoint2, clientConfig).weight(0.0f) // Zero weight
          .healthCheckEnabled(false).build();

      MultiDatabaseConfig config = new MultiDatabaseConfig.Builder(
          new MultiDatabaseConfig.DatabaseConfig[] { cluster1, cluster2 }).build();

      try (MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(config)) {
        // Should still initialize and select one of the clusters
        assertNotNull(provider.getDatabase());
      }
    }
  }
}
