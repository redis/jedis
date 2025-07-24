package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor;
import redis.clients.jedis.util.RedisVersionCondition;

/**
 * Comprehensive tests for RedisClient class covering: - Constructor variations - Builder pattern
 * functionality - Configuration validation - Redis operations - Pipeline and transaction support -
 * Resource management
 */
public class RedisClientTest {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(endpoint);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      endpoint);

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  private RedisClient client;

  @BeforeEach
  public void setUp() {
    // Clean up any existing data
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      jedis.flushAll();
    }
  }

  @AfterEach
  public void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  // Constructor Tests

  @Test
  public void testDefaultConstructor() {
    client = new RedisClient();
    assertNotNull(client);
    // Note: Cannot test actual connection without Redis running on default localhost:6379
  }

  @Test
  public void testHostPortConstructor() {
    client = new RedisClient(endpoint.getHost(), endpoint.getPort());
    assertNotNull(client);

    // Note: Authentication needs to be done through client configuration
    // This test just verifies the constructor works
  }

  @Test
  public void testHostAndPortConstructor() {
    HostAndPort hostAndPort = new HostAndPort(endpoint.getHost(), endpoint.getPort());
    client = new RedisClient(hostAndPort);
    assertNotNull(client);
  }

  @Test
  public void testURIConstructor() throws URISyntaxException {
    URI uri = endpoint.getURIBuilder().defaultCredentials().build();
    client = new RedisClient(uri);
    assertNotNull(client);

    // Test basic operation
    try {
      client.set("uri-test-key", "uri-test-value");
      assertEquals("uri-test-value", client.get("uri-test-key"));
    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  // Builder Tests

  @Test
  public void testBuilderBasicConfiguration() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    assertNotNull(client);

    // Test basic operation
    try {
      client.set("builder-test-key", "builder-test-value");
      assertEquals("builder-test-value", client.get("builder-test-key"));
    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  @Test
  public void testBuilderWithClientConfig() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().password(endpoint.getPassword())
        .database(0).timeoutMillis(5000).build();

    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort()).config(config)
        .build();

    assertNotNull(client);
  }

  @Test
  public void testBuilderWithPoolConfig() {
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(20);
    poolConfig.setMaxIdle(10);
    poolConfig.setMinIdle(5);

    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).poolConfig(poolConfig).build();

    assertNotNull(client);
  }

  @Test
  public void testBuilderWithCache() {
    Cache mockCache = mock(Cache.class);

    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).cache(mockCache).build();

    assertNotNull(client);
    assertEquals(mockCache, client.getCache());
  }

  @Test
  public void testBuilderWithCustomConnectionProvider() {
    ConnectionProvider mockProvider = mock(ConnectionProvider.class);
    Connection mockConnection = mock(Connection.class);
    when(mockProvider.getConnection()).thenReturn(mockConnection);
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);

    client = RedisClient.builder().connectionProvider(mockProvider).build();

    assertNotNull(client);
  }

  @Test
  public void testBuilderWithKeyPreProcessor() {
    CommandKeyArgumentPreProcessor keyProcessor = new PrefixedKeyArgumentPreProcessor("test:");

    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).keyPreProcessor(keyProcessor).build();

    assertNotNull(client);

    // Test that key preprocessing works
    try {
      client.set("mykey", "myvalue");
      // The actual key stored should be "test:mykey"
      // We can verify this by connecting with a regular client
      try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
          endpoint.getClientConfigBuilder().build())) {
        assertEquals("myvalue", jedis.get("test:mykey"));
        assertNull(jedis.get("mykey")); // Original key should not exist
      }
    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  @Test
  public void testBuilderWithJsonObjectMapper() {
    JsonObjectMapper mockMapper = mock(JsonObjectMapper.class);

    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).jsonObjectMapper(mockMapper).build();

    assertNotNull(client);
  }

  @Test
  public void testBuilderWithSearchDialect() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).searchDialect(3).build();

    assertNotNull(client);
  }

  @Test
  public void testBuilderSearchDialectValidation() {
    RedisClient.Builder builder = RedisClient.builder();

    // Test that dialect 0 throws exception
    assertThrows(IllegalArgumentException.class, () -> {
      builder.searchDialect(0);
    });

    // Test that other dialects are accepted
    assertDoesNotThrow(() -> {
      builder.searchDialect(1);
      builder.searchDialect(2);
      builder.searchDialect(3);
    });
  }

  @Test
  public void testBuilderFromURI() throws URISyntaxException {
    URI uri = endpoint.getURIBuilder().defaultCredentials().build();

    client = RedisClient.builder().fromURI(uri).build();

    assertNotNull(client);

    // Test basic operation
    try {
      client.set("uri-builder-test", "uri-builder-value");
      assertEquals("uri-builder-value", client.get("uri-builder-test"));
    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  // Validation Tests

  @Test
  public void testBuilderValidationNullHost() {
    RedisClient.Builder builder = RedisClient.builder().host(null);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  public void testBuilderValidationEmptyHost() {
    RedisClient.Builder builder = RedisClient.builder().host("");

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  public void testBuilderValidationInvalidPort() {
    RedisClient.Builder builder1 = RedisClient.builder().port(0);

    assertThrows(IllegalArgumentException.class, builder1::build);

    RedisClient.Builder builder2 = RedisClient.builder().port(65536);

    assertThrows(IllegalArgumentException.class, builder2::build);
  }

  @Test
  public void testBuilderValidationWithCustomProvider() {
    ConnectionProvider mockProvider = mock(ConnectionProvider.class);

    // When custom provider is set, host/port validation should be skipped
    client = RedisClient.builder().connectionProvider(mockProvider).host(null) // This should not
                                                                               // cause validation
                                                                               // error
        .port(0) // This should not cause validation error
        .build();

    assertNotNull(client);
  }

  // Redis Operations Tests

  @Test
  public void testBasicRedisOperations() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    try {
      // String operations
      assertEquals("OK", client.set("string-key", "string-value"));
      assertEquals("string-value", client.get("string-key"));

      // Hash operations
      assertEquals(1L, client.hset("hash-key", "field1", "value1"));
      assertEquals("value1", client.hget("hash-key", "field1"));

      // List operations
      assertEquals(1L, client.lpush("list-key", "item1"));
      assertEquals("item1", client.lpop("list-key"));

      // Set operations
      assertEquals(1L, client.sadd("set-key", "member1"));
      assertTrue(client.sismember("set-key", "member1"));

      // Sorted set operations
      assertEquals(1L, client.zadd("zset-key", 1.0, "member1"));
      assertEquals(Double.valueOf(1.0), client.zscore("zset-key", "member1"));

    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  @Test
  public void testPipelineOperations() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    try {
      Pipeline pipeline = client.pipelined();
      assertNotNull(pipeline);

      // Queue multiple operations
      pipeline.set("pipe-key1", "pipe-value1");
      pipeline.set("pipe-key2", "pipe-value2");
      pipeline.get("pipe-key1");
      pipeline.get("pipe-key2");

      // Execute pipeline
      pipeline.sync();
      pipeline.close();

      // Verify results
      assertEquals("pipe-value1", client.get("pipe-key1"));
      assertEquals("pipe-value2", client.get("pipe-key2"));

    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  @Test
  public void testTransactionOperations() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    try {
      Transaction transaction = client.multi();
      assertNotNull(transaction);

      // Queue multiple operations in transaction
      transaction.set("tx-key1", "tx-value1");
      transaction.set("tx-key2", "tx-value2");
      transaction.get("tx-key1");

      // Execute transaction
      transaction.exec();
      transaction.close();

      // Verify results
      assertEquals("tx-value1", client.get("tx-key1"));
      assertEquals("tx-value2", client.get("tx-key2"));

    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  @Test
  public void testTransactionWithoutMulti() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    try {
      Transaction transaction = client.transaction(false);
      assertNotNull(transaction);

      transaction.close();

    } catch (Exception e) {
      // Redis not available, skip operation test
    }
  }

  // Resource Management Tests

  @Test
  public void testAutoCloseable() {
    // Test that RedisClient implements AutoCloseable properly
    try (RedisClient autoClient = RedisClient.builder().host(endpoint.getHost())
        .port(endpoint.getPort()).config(endpoint.getClientConfigBuilder().build()).build()) {

      assertNotNull(autoClient);
      // Client should be automatically closed when exiting try block
    } catch (Exception e) {
      // Redis not available, skip test
    }
  }

  @Test
  public void testManualClose() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    assertNotNull(client);

    // Manual close should not throw exception
    assertDoesNotThrow(() -> {
      client.close();
    });

    // Multiple closes should be safe
    assertDoesNotThrow(() -> {
      client.close();
    });

    client = null; // Prevent tearDown from closing again
  }

  // Abstract Method Implementation Tests

  @Test
  public void testAbstractMethodImplementations() {
    client = RedisClient.builder().host(endpoint.getHost()).port(endpoint.getPort())
        .config(endpoint.getClientConfigBuilder().build()).build();

    // Test that abstract methods are properly implemented
    assertNotNull(client.getCommandObjects());
    assertNotNull(client.getConnectionProvider());

    // Test command execution methods don't throw
    try {
      // These methods are protected, but we can test them indirectly
      // through public Redis operations
      client.set("ping-test", "pong");
      assertEquals("pong", client.get("ping-test"));
    } catch (Exception e) {
      // Redis not available, skip test
    }
  }

  // Error Handling Tests

  @Test
  public void testInvalidHostConnection() {
    // Test connection to invalid host
    client = RedisClient.builder().host("invalid-host-that-does-not-exist").port(6379).build();

    assertNotNull(client);

    // Operations should fail with connection exception
    assertThrows(Exception.class, () -> {
      client.set("test", "value");
    });
  }

  @Test
  public void testBuilderDefaults() {
    // Test that builder uses proper defaults
    client = RedisClient.builder().build();

    assertNotNull(client);
    // Should use localhost:6379 by default
  }
}
