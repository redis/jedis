package redis.clients.jedis.mcf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.exceptions.JedisValidationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class MultiDbConnectionProviderDynamicEndpointUnitTest {

  private MultiDbConnectionProvider provider;
  private JedisClientConfig clientConfig;
  private final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("standalone0");
  private final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("standalone1");

  @BeforeEach
  void setUp() {
    clientConfig = DefaultJedisClientConfig.builder().build();

    // Create initial provider with endpoint1
    DatabaseConfig initialConfig = createDatabaseConfig(endpoint1.getHostAndPort(), 1.0f);

    MultiDbConfig multiConfig = new MultiDbConfig.Builder(new DatabaseConfig[] { initialConfig })
        .build();

    provider = new MultiDbConnectionProvider(multiConfig);
  }

  // Helper method to create database configurations
  private DatabaseConfig createDatabaseConfig(HostAndPort hostAndPort, float weight) {
    // Disable health check for unit tests to avoid real connections
    return DatabaseConfig.builder(hostAndPort, clientConfig).weight(weight)
        .healthCheckEnabled(false).build();
  }

  @Test
  void testAddNewDatabase() {
    DatabaseConfig newConfig = createDatabaseConfig(endpoint2.getHostAndPort(), 2.0f);

    // Should not throw exception
    assertDoesNotThrow(() -> provider.add(newConfig));

    // Verify the database was added by checking it can be retrieved
    assertNotNull(provider.getDatabase(endpoint2.getHostAndPort()));
  }

  @Test
  void testAddDuplicateDatabase() {
    DatabaseConfig duplicateConfig = createDatabaseConfig(endpoint1.getHostAndPort(), 2.0f);

    // Should throw validation exception for duplicate endpoint
    assertThrows(JedisValidationException.class, () -> provider.add(duplicateConfig));
  }

  @Test
  void testAddNullDatabaseConfig() {
    // Should throw validation exception for null config
    assertThrows(JedisValidationException.class, () -> provider.add(null));
  }

  @Test
  void testRemoveExistingDatabase() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.ping()).thenReturn(true);

    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool(mockConnection)) {
      // Create initial provider with endpoint1
      DatabaseConfig dbConfig1 = createDatabaseConfig(endpoint1.getHostAndPort(), 1.0f);

      MultiDbConfig multiConfig = MultiDbConfig.builder(new DatabaseConfig[] { dbConfig1 }).build();

      try (MultiDbConnectionProvider providerWithMockedPool = new MultiDbConnectionProvider(
          multiConfig)) {

        // Add endpoint2 as second database
        DatabaseConfig newConfig = createDatabaseConfig(endpoint2.getHostAndPort(), 2.0f);
        providerWithMockedPool.add(newConfig);

        // Now remove endpoint1 (original database)
        assertDoesNotThrow(() -> providerWithMockedPool.remove(endpoint1.getHostAndPort()));

        // Verify endpoint1 was removed
        assertNull(providerWithMockedPool.getDatabase(endpoint1.getHostAndPort()));
        // Verify endpoint2 still exists
        assertNotNull(providerWithMockedPool.getDatabase(endpoint2.getHostAndPort()));
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
  void testRemoveNonExistentDatabase() {
    HostAndPort nonExistentEndpoint = new HostAndPort("localhost", 9999);

    // Should throw validation exception for non-existent endpoint
    assertThrows(JedisValidationException.class, () -> provider.remove(nonExistentEndpoint));
  }

  @Test
  void testRemoveLastRemainingDatabase() {
    // Should throw validation exception when trying to remove the last database
    assertThrows(JedisValidationException.class, () -> provider.remove(endpoint1.getHostAndPort()));
  }

  @Test
  void testRemoveNullEndpoint() {
    // Should throw validation exception for null endpoint
    assertThrows(JedisValidationException.class, () -> provider.remove(null));
  }

  @Test
  void testAddAndRemoveMultipleDatabases() {
    // Add endpoint2 as second database
    DatabaseConfig config2 = createDatabaseConfig(endpoint2.getHostAndPort(), 2.0f);

    // Create a third endpoint for this test
    HostAndPort endpoint3 = new HostAndPort("localhost", 6381);
    DatabaseConfig config3 = createDatabaseConfig(endpoint3, 3.0f);

    provider.add(config2);
    provider.add(config3);

    // Verify all databases exist
    assertNotNull(provider.getDatabase(endpoint1.getHostAndPort()));
    assertNotNull(provider.getDatabase(endpoint2.getHostAndPort()));
    assertNotNull(provider.getDatabase(endpoint3));

    // Remove endpoint2
    provider.remove(endpoint2.getHostAndPort());

    // Verify correct database was removed
    assertNull(provider.getDatabase(endpoint2.getHostAndPort()));
    assertNotNull(provider.getDatabase(endpoint1.getHostAndPort()));
    assertNotNull(provider.getDatabase(endpoint3));
  }

  @Test
  void testActiveDatabaseHandlingOnAdd() {
    // The initial database should be active
    assertNotNull(provider.getDatabase());

    // Add endpoint2 with higher weight
    DatabaseConfig newConfig = createDatabaseConfig(endpoint2.getHostAndPort(), 5.0f);
    provider.add(newConfig);

    // Active database should still be valid (implementation may or may not switch)
    assertNotNull(provider.getDatabase());
  }

  @Test
  void testActiveDatabaseHandlingOnRemove() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.ping()).thenReturn(true);

    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockPool(mockConnection)) {
      // Create initial provider with endpoint1
      DatabaseConfig dbConfig1 = createDatabaseConfig(endpoint1.getHostAndPort(), 1.0f);

      MultiDbConfig multiConfig = MultiDbConfig.builder(new DatabaseConfig[] { dbConfig1 }).build();

      try (MultiDbConnectionProvider providerWithMockedPool = new MultiDbConnectionProvider(
          multiConfig)) {

        // Add endpoint2 as second database
        DatabaseConfig newConfig = createDatabaseConfig(endpoint2.getHostAndPort(), 2.0f);
        providerWithMockedPool.add(newConfig);

        // Get current active database
        Object initialActiveDb = providerWithMockedPool.getDatabase();
        assertNotNull(initialActiveDb);

        // Remove endpoint1 (original database, might be active)
        providerWithMockedPool.remove(endpoint1.getHostAndPort());

        // Should still have an active database
        assertNotNull(providerWithMockedPool.getDatabase());
      }
    }
  }
}
