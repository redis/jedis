package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.HashSet;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.SentineledConnectionProvider;

/**
 * Test class to verify that the AbstractRedisClientBuilder works correctly with both
 * RedisClient.Builder and RedisSentinelClient.Builder.
 */
public class AbstractRedisClientBuilderTest {

  @Test
  public void testRedisClientBuilderInheritance() {
    // Test that RedisClient.Builder inherits common methods from AbstractRedisClientBuilder
    RedisClient.Builder builder = RedisClient.builder();

    // Test method chaining with inherited methods
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(20);

    CommandKeyArgumentPreProcessor keyProcessor = key -> "test:" + key;

    RedisClient.Builder result = builder.host("localhost").port(6379).poolConfig(poolConfig)
        .keyPreProcessor(keyProcessor).searchDialect(3);

    // Verify method chaining returns the correct builder type
    assertSame(builder, result);
    assertNotNull(builder);
  }

  @Test
  public void testRedisSentinelClientBuilderInheritance() {
    // Test that RedisSentinelClient.Builder inherits common methods from AbstractRedisClientBuilder
    Set<HostAndPort> sentinels = new HashSet<>();
    sentinels.add(new HostAndPort("localhost", 26379));

    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    // Test method chaining with inherited methods
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(15);

    CommandKeyArgumentPreProcessor keyProcessor = key -> "sentinel:" + key;

    RedisSentinelClient.Builder result = builder.masterName("mymaster").sentinels(sentinels)
        .poolConfig(poolConfig).keyPreProcessor(keyProcessor).searchDialect(2);

    // Verify method chaining returns the correct builder type
    assertSame(builder, result);
    assertNotNull(builder);
  }

  @Test
  public void testCommonMethodsAvailableOnBothBuilders() {
    // Test that both builders have the same common methods available
    RedisClient.Builder redisBuilder = RedisClient.builder();
    RedisSentinelClient.Builder sentinelBuilder = RedisSentinelClient.builder();

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    CommandKeyArgumentPreProcessor keyProcessor = key -> "prefix:" + key;

    // Test that both builders support the same common configuration methods
    assertNotNull(redisBuilder.poolConfig(poolConfig));
    assertNotNull(sentinelBuilder.poolConfig(poolConfig));

    assertNotNull(redisBuilder.keyPreProcessor(keyProcessor));
    assertNotNull(sentinelBuilder.keyPreProcessor(keyProcessor));

    assertNotNull(redisBuilder.searchDialect(3));
    assertNotNull(sentinelBuilder.searchDialect(3));
  }

  @Test
  public void testSearchDialectValidation() {
    // Test that search dialect validation works for both builders
    RedisClient.Builder redisBuilder = RedisClient.builder();
    RedisSentinelClient.Builder sentinelBuilder = RedisSentinelClient.builder();

    // Test that dialect 0 is rejected for both builders
    assertThrows(IllegalArgumentException.class, () -> {
      redisBuilder.searchDialect(0);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      sentinelBuilder.searchDialect(0);
    });

    // Test that valid dialects are accepted
    assertNotNull(redisBuilder.searchDialect(1));
    assertNotNull(redisBuilder.searchDialect(2));
    assertNotNull(redisBuilder.searchDialect(3));

    assertNotNull(sentinelBuilder.searchDialect(1));
    assertNotNull(sentinelBuilder.searchDialect(2));
    assertNotNull(sentinelBuilder.searchDialect(3));
  }

  @Test
  public void testBuilderSpecificMethods() {
    // Test that each builder still has its specific methods
    RedisClient.Builder redisBuilder = RedisClient.builder();
    RedisSentinelClient.Builder sentinelBuilder = RedisSentinelClient.builder();

    // RedisClient.Builder specific methods
    assertNotNull(redisBuilder.host("localhost"));
    assertNotNull(redisBuilder.port(6379));
    assertNotNull(redisBuilder.config(DefaultJedisClientConfig.builder().build()));

    // RedisSentinelClient.Builder specific methods
    Set<HostAndPort> sentinels = new HashSet<>();
    sentinels.add(new HostAndPort("localhost", 26379));

    assertNotNull(sentinelBuilder.masterName("mymaster"));
    assertNotNull(sentinelBuilder.sentinels(sentinels));
    assertNotNull(sentinelBuilder.masterConfig(DefaultJedisClientConfig.builder().build()));
    assertNotNull(sentinelBuilder.sentinelConfig(DefaultJedisClientConfig.builder().build()));
  }

  @Test
  public void testConnectionProviderOverride() {
    // Test that RedisSentinelClient.Builder properly overrides connectionProvider method
    RedisSentinelClient.Builder builder = RedisSentinelClient.builder();

    // Test that the connectionProvider method exists and returns the correct type
    // We don't actually create a provider to avoid connection issues in tests
    assertNotNull(builder);

    // Test that we can call the connectionProvider method (even with null)
    // This tests the API without requiring actual sentinel connections
    RedisSentinelClient.Builder result = builder.connectionProvider(null);
    assertSame(builder, result);
  }

  @Test
  public void testAbstractBuilderGenerics() {
    // Test that the generic types work correctly
    RedisClient.Builder redisBuilder = RedisClient.builder();
    RedisSentinelClient.Builder sentinelBuilder = RedisSentinelClient.builder();

    // Verify that the builders are instances of AbstractRedisClientBuilder
    assertTrue(redisBuilder instanceof AbstractRedisClientBuilder);
    assertTrue(sentinelBuilder instanceof AbstractRedisClientBuilder);

    // Verify that method chaining preserves the correct types
    RedisClient.Builder redisResult = redisBuilder.searchDialect(2);
    RedisSentinelClient.Builder sentinelResult = sentinelBuilder.searchDialect(2);

    assertSame(redisBuilder, redisResult);
    assertSame(sentinelBuilder, sentinelResult);
  }
}
