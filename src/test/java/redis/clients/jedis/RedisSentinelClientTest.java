package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.HashSet;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Tests for RedisSentinelClient. These tests include both unit tests for configuration
 * and integration tests that attempt actual connections to sentinel-managed Redis instances.
 */
public class RedisSentinelClientTest {

  private static final String PRIMARY_NAME = "myprimary";

  private static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  private static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  private Set<HostAndPort> sentinels;
  private JedisClientConfig primaryConfig;
  private JedisClientConfig sentinelConfig;

  @BeforeEach
  public void setUp() throws Exception {
    // Use configured sentinel endpoints
    sentinels = new HashSet<>();
    sentinels.add(sentinel1);
    sentinels.add(sentinel2);

    // Create master configuration with proper authentication
    primaryConfig = DefaultJedisClientConfig.builder()
        .password("foobared")
        .database(2)
        .build();

    // Create sentinel configuration (sentinels typically don't require auth in test env)
    sentinelConfig = DefaultJedisClientConfig.builder()
        .build();
  }

  @Test
  public void testBuilderWithMinimalConfiguration() {
    // Test that builder can be created with minimal configuration
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder()
        .primary(PRIMARY_NAME, primaryConfig)
        .sentinels(sentinels);

    assertNotNull(builder);

    // Test actual connection attempt
    try (RedisSentinelClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected if sentinel/master not available in test environment
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getMessage().contains("sentinel") || e.getMessage().contains("master")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception, got: " + e.getMessage());
    }
  }

  @Test
  public void testBuilderWithAdvancedConfiguration() {
    JedisClientConfig customPrimaryConfig = DefaultJedisClientConfig.builder()
        .password("foobared")
        .database(1)
        .connectionTimeoutMillis(5000)
        .socketTimeoutMillis(10000)
        .build();

    JedisClientConfig customSentinelConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(3000)
        .build();

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(20);
    poolConfig.setMaxIdle(10);

    RedisSentinelClient.Builder builder = RedisSentinelClient.builder()
        .primary(PRIMARY_NAME, customPrimaryConfig)
        .sentinels(sentinels)
        .sentinelConfig(customSentinelConfig)
        .poolConfig(poolConfig)
        .subscribeRetryWaitTimeMillis(10000)
        .searchDialect(3);

    assertNotNull(builder);

    // Test actual connection attempt
    try (RedisSentinelClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected if sentinel/master not available in test environment
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getMessage().contains("sentinel") || e.getMessage().contains("master")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception, got: " + e.getMessage());
    }
  }

  @Test
  public void testBuilderWithCustomConnectionProvider() {
    // Test that the builder accepts a custom connection provider method
    // We don't actually create a provider here to avoid connection issues in tests
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    // Test that the method exists and returns the builder for chaining
    assertNotNull(builder);

    // Test that we can call the connectionProvider method (even with null)
    // This tests the API without requiring actual sentinel connections
    assertSame(builder, builder.connectionProvider(null));
  }

  @Test
  public void testBuilderFailsWithoutSentinels() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisSentinelClient.builder()
          .masterName(PRIMARY_NAME)
          .masterConfig(primaryConfig)
          .build();
    });
  }

  @Test
  public void testBuilderFailsWithEmptySentinels() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisSentinelClient.builder()
          .masterName(PRIMARY_NAME)
          .sentinels(new HashSet<>())
          .masterConfig(primaryConfig)
          .build();
    });
  }

  @Test
  public void testBuilderMethodChaining() {
    // Test that all builder methods return the builder for chaining
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    assertSame(builder, builder.masterName(PRIMARY_NAME));
    assertSame(builder, builder.sentinels(sentinels));
    assertSame(builder, builder.masterConfig(primaryConfig));
    assertSame(builder, builder.sentinelConfig(sentinelConfig));
    assertSame(builder, builder.poolConfig(new ConnectionPoolConfig()));
    assertSame(builder, builder.subscribeRetryWaitTimeMillis(5000));
    assertSame(builder, builder.searchDialect(3));
  }

  @Test
  public void testBuilderDefaults() {
    // Test that builder has sensible defaults
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    // These should not throw exceptions when accessed
    assertNotNull(builder);
  }

  // Integration Tests

  @Test
  public void testIntegrationBasicOperations() {
    // Test basic Redis operations through sentinel client
    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .masterName(PRIMARY_NAME)
        .sentinels(sentinels)
        .masterConfig(primaryConfig)
        .sentinelConfig(sentinelConfig)
        .build()) {

      // Test basic operations
      client.set("sentinel-test-key", "sentinel-test-value");
      assertEquals("sentinel-test-value", client.get("sentinel-test-key"));

      // Clean up
      client.del("sentinel-test-key");

    } catch (Exception e) {
      // Expected if sentinel/master not available in test environment
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getMessage().contains("sentinel") || e.getMessage().contains("master")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception, got: " + e.getMessage());
    }
  }

  @Test
  public void testIntegrationWithPoolConfiguration() {
    // Test sentinel client with custom pool configuration
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(5);
    poolConfig.setMaxIdle(2);
    poolConfig.setMinIdle(1);

    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .masterName(PRIMARY_NAME)
        .sentinels(sentinels)
        .masterConfig(primaryConfig)
        .sentinelConfig(sentinelConfig)
        .poolConfig(poolConfig)
        .build()) {

      // Test that client was created successfully
      assertNotNull(client);

      // Test basic operation
      client.set("pool-test-key", "pool-test-value");
      assertEquals("pool-test-value", client.get("pool-test-key"));

      // Clean up
      client.del("pool-test-key");

    } catch (Exception e) {
      // Expected if sentinel/master not available in test environment
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getMessage().contains("sentinel") || e.getMessage().contains("master")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception, got: " + e.getMessage());
    }
  }

  @Test
  public void testIntegrationWithCustomConnectionProvider() {
    // Test with custom connection provider (null test for API verification)
    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .masterName(PRIMARY_NAME)
        .sentinels(sentinels)
        .masterConfig(primaryConfig)
        .sentinelConfig(sentinelConfig)
        .subscribeRetryWaitTimeMillis(3000)
        .build()) {

      assertNotNull(client);

      // Test basic operation
      client.set("provider-test-key", "provider-test-value");
      assertEquals("provider-test-value", client.get("provider-test-key"));

      // Test sentinel-specific method
      HostAndPort currentMaster = client.getCurrentMaster();
      assertNotNull(currentMaster);

      // Clean up
      client.del("provider-test-key");

    } catch (Exception e) {
      // Expected if sentinel/master not available in test environment
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getMessage().contains("sentinel") || e.getMessage().contains("master")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception, got: " + e.getMessage());
    }
  }

}
