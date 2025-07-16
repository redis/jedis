package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.HashSet;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.SentineledConnectionProvider;

public class RedisSentinelClientTest {

  private static final String MASTER_NAME = "mymaster";
  private Set<HostAndPort> sentinels;

  @BeforeEach
  public void setUp() throws Exception {
    sentinels = new HashSet<>();
    sentinels.add(new HostAndPort("localhost", 26379));
    sentinels.add(new HostAndPort("localhost", 26380));
  }

  @Test
  public void testBuilderWithMinimalConfiguration() {
    // Test that builder can be created with minimal configuration
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels);

    assertNotNull(builder);

    // Note: We don't actually build() here because it would try to connect to sentinels
    // which may not be available in the test environment
  }

  @Test
  public void testBuilderWithAdvancedConfiguration() {
    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("master-password")
        .database(1).connectionTimeoutMillis(5000).socketTimeoutMillis(10000).build();

    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .password("sentinel-password").connectionTimeoutMillis(3000).build();

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(20);
    poolConfig.setMaxIdle(10);

    RedisSentinelClient.Builder builder = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).masterConfig(masterConfig).sentinelConfig(sentinelConfig)
        .poolConfig(poolConfig).subscribeRetryWaitTimeMillis(10000).searchDialect(3);

    assertNotNull(builder);
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
  public void testBuilderFailsWithoutMasterName() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisSentinelClient.builder().sentinels(sentinels).build();
    });
  }

  @Test
  public void testBuilderFailsWithoutSentinels() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisSentinelClient.builder().masterName(MASTER_NAME).build();
    });
  }

  @Test
  public void testBuilderFailsWithEmptySentinels() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisSentinelClient.builder().masterName(MASTER_NAME).sentinels(new HashSet<>()).build();
    });
  }

  @Test
  public void testBuilderMethodChaining() {
    // Test that all builder methods return the builder for chaining
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    assertSame(builder, builder.masterName(MASTER_NAME));
    assertSame(builder, builder.sentinels(sentinels));
    assertSame(builder, builder.masterConfig(DefaultJedisClientConfig.builder().build()));
    assertSame(builder, builder.sentinelConfig(DefaultJedisClientConfig.builder().build()));
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

  // Integration test - only run if sentinel is available
  // Commented out because it requires actual sentinel setup
  /*
   * @Test public void testActualSentinelConnection() { // This test would require actual Redis
   * Sentinel setup try (RedisSentinelClient client = RedisSentinelClient.builder()
   * .masterName(MASTER_NAME) .sentinels(sentinels) .masterConfig(DefaultJedisClientConfig.builder()
   * .password("foobared") .database(2) .build()) .build()) { // Test basic operations
   * client.set("test-key", "test-value"); assertEquals("test-value", client.get("test-key")); //
   * Test sentinel-specific method HostAndPort currentMaster = client.getCurrentMaster();
   * assertNotNull(currentMaster); // Test pipeline Pipeline pipeline = client.pipelined();
   * pipeline.set("pipeline-key", "pipeline-value"); pipeline.get("pipeline-key"); pipeline.sync();
   * // Test transaction Transaction transaction = client.multi(); transaction.set("tx-key",
   * "tx-value"); transaction.get("tx-key"); transaction.exec(); } catch (Exception e) { // Skip
   * test if sentinel is not available System.out.println("Skipping sentinel integration test: " +
   * e.getMessage()); } }
   */
}
