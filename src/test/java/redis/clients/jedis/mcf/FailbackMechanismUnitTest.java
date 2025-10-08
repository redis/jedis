package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;

@ExtendWith(MockitoExtension.class)
class FailbackMechanismUnitTest {

  private HostAndPort endpoint1;
  private JedisClientConfig clientConfig;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("localhost", 6379);
    clientConfig = DefaultJedisClientConfig.builder().build();
  }

  @Test
  void testFailbackCheckIntervalConfiguration() {
    // Test default value
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    MultiDbConfig defaultConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).build();

    assertEquals(120000, defaultConfig.getFailbackCheckInterval());

    // Test custom value
    MultiDbConfig customConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackCheckInterval(3000).build();

    assertEquals(3000, customConfig.getFailbackCheckInterval());
  }

  @Test
  void testFailbackSupportedConfiguration() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    // Test default (should be true)
    MultiDbConfig defaultConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).build();

    assertTrue(defaultConfig.isFailbackSupported());

    // Test disabled
    MultiDbConfig disabledConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackSupported(false).build();

    assertFalse(disabledConfig.isFailbackSupported());
  }

  @Test
  void testFailbackCheckIntervalValidation() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    // Test zero interval (should be allowed)
    MultiDbConfig zeroConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackCheckInterval(0).build();

    assertEquals(0, zeroConfig.getFailbackCheckInterval());

    // Test negative interval (should be allowed - implementation decision)
    MultiDbConfig negativeConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackCheckInterval(-1000).build();

    assertEquals(-1000, negativeConfig.getFailbackCheckInterval());
  }

  @Test
  void testBuilderChaining() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    // Test that builder methods can be chained
    MultiDbConfig config = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackSupported(true)
            .failbackCheckInterval(2000).retryOnFailover(true).build();

    assertTrue(config.isFailbackSupported());
    assertEquals(2000, config.getFailbackCheckInterval());
    assertTrue(config.isRetryOnFailover());
  }

  @Test
  void testGracePeriodConfiguration() {
    // Test default value
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    MultiDbConfig defaultConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).build();

    assertEquals(60000, defaultConfig.getGracePeriod());

    // Test custom value
    MultiDbConfig customConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).gracePeriod(5000).build();

    assertEquals(5000, customConfig.getGracePeriod());
  }

  @Test
  void testGracePeriodValidation() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    // Test zero grace period (should be allowed)
    MultiDbConfig zeroConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).gracePeriod(0).build();

    assertEquals(0, zeroConfig.getGracePeriod());

    // Test negative grace period (should be allowed - implementation decision)
    MultiDbConfig negativeConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).gracePeriod(-1000).build();

    assertEquals(-1000, negativeConfig.getGracePeriod());
  }

  @Test
  void testGracePeriodBuilderChaining() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(endpoint1, clientConfig).healthCheckEnabled(false).build();

    // Test that builder methods can be chained
    MultiDbConfig config = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).failbackSupported(true)
            .failbackCheckInterval(2000).gracePeriod(8000).retryOnFailover(true).build();

    assertTrue(config.isFailbackSupported());
    assertEquals(2000, config.getFailbackCheckInterval());
    assertEquals(8000, config.getGracePeriod());
    assertTrue(config.isRetryOnFailover());
  }
}
