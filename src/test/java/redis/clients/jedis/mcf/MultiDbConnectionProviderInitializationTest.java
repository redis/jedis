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
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.exceptions.JedisValidationException;

/**
 * Tests for MultiDbConnectionProvider initialization edge cases
 */
@ExtendWith(MockitoExtension.class)
public class MultiDbConnectionProviderInitializationTest {

  private HostAndPort endpoint1;
  private HostAndPort endpoint2;
  private HostAndPort endpoint3;
  private JedisClientConfig clientConfig;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("fake", 6379);
    endpoint2 = new HostAndPort("fake", 6380);
    endpoint3 = new HostAndPort("fake", 6381);
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
      // Create databases with mixed health check configuration
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false) // No health
                                     // check
          .build();

      DatabaseConfig db2 = DatabaseConfig.builder(endpoint2, clientConfig).weight(2.0f)
          .healthCheckStrategySupplier(PingStrategy.DEFAULT) // With
                                                             // health
                                                             // check
          .build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db2 })
          .initializationPolicy(InitializationPolicy.BuiltIn.ONE_AVAILABLE).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should initialize successfully
        assertNotNull(provider.getDatabase());

        // Should select db1 (no health check, assumed healthy) or db2 based on weight
        // Since db2 has higher weight and health checks, it should be selected if healthy
        assertTrue(provider.getDatabase() == provider.getDatabase(endpoint1)
            || provider.getDatabase() == provider.getDatabase(endpoint2));
      }
    }
  }

  @Test
  void testInitializationWithAllHealthChecksDisabled() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      // Create databases with no health checks
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false).build();

      DatabaseConfig db22 = DatabaseConfig.builder(endpoint2, clientConfig).weight(3.0f) // Higher
                                                                                         // weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db22 }).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should select db22 (highest weight, no health checks)
        assertEquals(provider.getDatabase(endpoint2), provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationWithSingleDatabase() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      DatabaseConfig db = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db }).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should select the only available db
        assertEquals(provider.getDatabase(endpoint1), provider.getDatabase());
      }
    }
  }

  @Test
  void testErrorHandlingWithNullConfiguration() {
    assertThrows(JedisValidationException.class, () -> {
      new MultiDbConnectionProvider(null);
    });
  }

  @Test
  void testErrorHandlingWithEmptyDatabaseArray() {
    assertThrows(JedisValidationException.class, () -> {
      new MultiDbConfig.Builder(new DatabaseConfig[0]).build();
    });
  }

  @Test
  void testErrorHandlingWithNullDatabaseConfig() {
    assertThrows(IllegalArgumentException.class, () -> {
      new MultiDbConfig.Builder(new DatabaseConfig[] { null }).build();
    });
  }

  @Test
  void testInitializationWithZeroWeights() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(0.0f) // Zero
                                                                                        // weight
          .healthCheckEnabled(false).build();

      DatabaseConfig db2 = DatabaseConfig.builder(endpoint2, clientConfig).weight(0.0f) // Zero
                                                                                        // weight
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db2 }).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should still initialize and select one of the databases
        assertNotNull(provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationWithOneAvailablePolicy() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false).build();

      DatabaseConfig db2 = DatabaseConfig.builder(endpoint2, clientConfig).weight(2.0f)
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db2 })
          .initializationPolicy(InitializationPolicy.BuiltIn.ONE_AVAILABLE).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should initialize successfully with ONE_AVAILABLE policy
        assertNotNull(provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationWithAllAvailablePolicy() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false).build();

      DatabaseConfig db2 = DatabaseConfig.builder(endpoint2, clientConfig).weight(2.0f)
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db2 })
          .initializationPolicy(InitializationPolicy.BuiltIn.ALL_AVAILABLE).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should initialize successfully with ALL_AVAILABLE policy when all health checks
        // are disabled
        assertNotNull(provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationWithMajorityAvailablePolicy() {
    try (MockedConstruction<ConnectionPool> mockedPool = mockPool()) {
      DatabaseConfig db1 = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
          .healthCheckEnabled(false).build();

      DatabaseConfig db2 = DatabaseConfig.builder(endpoint2, clientConfig).weight(2.0f)
          .healthCheckEnabled(false).build();

      DatabaseConfig db3 = DatabaseConfig.builder(endpoint3, clientConfig).weight(3.0f)
          .healthCheckEnabled(false).build();

      MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db1, db2, db3 })
          .initializationPolicy(InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE).build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        // Should initialize successfully with MAJORITY_AVAILABLE policy
        assertNotNull(provider.getDatabase());
        // Should select db3 (highest weight)
        assertEquals(provider.getDatabase(endpoint3), provider.getDatabase());
      }
    }
  }

  @Test
  void testInitializationPolicyNullThrowsException() {
    DatabaseConfig db = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
        .healthCheckEnabled(false).build();

    assertThrows(IllegalArgumentException.class, () -> {
      new MultiDbConfig.Builder(new DatabaseConfig[] { db }).initializationPolicy(null).build();
    });
  }

  @Test
  void testInitializationPolicyIsConfigured() {
    DatabaseConfig db = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
        .healthCheckEnabled(false).build();

    MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db })
        .initializationPolicy(InitializationPolicy.BuiltIn.ALL_AVAILABLE).build();

    assertEquals(InitializationPolicy.BuiltIn.ALL_AVAILABLE, config.getInitializationPolicy());
  }

  @Test
  void testInitializationPolicyDefaultValue() {
    DatabaseConfig db = DatabaseConfig.builder(endpoint1, clientConfig).weight(1.0f)
        .healthCheckEnabled(false).build();

    MultiDbConfig config = new MultiDbConfig.Builder(new DatabaseConfig[] { db }).build();

    // Default should be MAJORITY_AVAILABLE
    assertEquals(InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE, config.getInitializationPolicy());
  }
}
