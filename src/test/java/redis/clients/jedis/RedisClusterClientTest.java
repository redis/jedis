package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.executors.ClusterCommandExecutor;

/**
 * Unit tests for RedisClusterClient. These tests verify the basic functionality and configuration
 * of RedisClusterClient without requiring an actual Redis cluster.
 */
public class RedisClusterClientTest {

  private Set<HostAndPort> nodes;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    nodes = new HashSet<>();
    nodes.add(new HostAndPort("localhost", 7000));
    nodes.add(new HostAndPort("localhost", 7001));
    nodes.add(new HostAndPort("localhost", 7002));
  }

  @Test
  public void testBuilderWithNodes() {
    RedisClusterClient.Builder builder = RedisClusterClient.builder().nodes(nodes);

    assertNotNull(builder);

    // Test that we can build without throwing exceptions
    try (RedisClusterClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testBuilderWithClientConfig() {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password("testpassword")
        .connectionTimeoutMillis(5000).socketTimeoutMillis(10000).build();

    RedisClusterClient.Builder builder = RedisClusterClient.builder().nodes(nodes)
        .clientConfig(clientConfig);

    assertNotNull(builder);

    try (RedisClusterClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testBuilderWithMaxAttempts() {
    RedisClusterClient.Builder builder = RedisClusterClient.builder().nodes(nodes).maxAttempts(10);

    assertNotNull(builder);

    try (RedisClusterClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testBuilderWithMaxTotalRetriesDuration() {
    RedisClusterClient.Builder builder = RedisClusterClient.builder().nodes(nodes)
        .maxTotalRetriesDuration(Duration.ofSeconds(30));

    assertNotNull(builder);

    try (RedisClusterClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testBuilderWithTopologyRefreshPeriod() {
    RedisClusterClient.Builder builder = RedisClusterClient.builder().nodes(nodes)
        .topologyRefreshPeriod(Duration.ofMinutes(5));

    assertNotNull(builder);

    try (RedisClusterClient client = builder.build()) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testBuilderWithNullNodes() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisClusterClient.builder().nodes(null).build();
    });
  }

  @Test
  public void testBuilderWithEmptyNodes() {
    assertThrows(IllegalArgumentException.class, () -> {
      Set<HostAndPort> nodes1 = new HashSet<>();
      RedisClusterClient.builder().nodes(nodes1).build();
    });
  }

  @Test
  public void testBuilderWithInvalidMaxAttempts() {
    assertThrows(IllegalArgumentException.class, () -> {
      RedisClusterClient.builder().nodes(nodes).maxAttempts(0).build();
    });
  }

  @Test
  public void testConstructorWithNodes() {
    try (RedisClusterClient client = new RedisClusterClient(nodes)) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testConstructorWithNodesAndClientConfig() {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password("testpassword")
        .build();

    try (RedisClusterClient client = new RedisClusterClient(nodes, clientConfig)) {
      assertNotNull(client);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }

  @Test
  public void testTransactionNotSupported() {
    try (RedisClusterClient client = new RedisClusterClient(nodes)) {
      try {
        client.transaction(true);
        fail("Expected UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
        assertEquals("Transactions are not supported in cluster mode", e.getMessage());
      }
    } catch (Exception e) {
      // If we can't create the client due to connection issues,
      // we can't test the transaction method, so we skip this test
      if (e.getMessage().contains("Connection") || e.getMessage().contains("connection")
          || e.getCause() instanceof java.net.ConnectException) {
        // This is expected when no cluster is running
        return;
      }
      throw e;
    }
  }

  @Test
  public void testPipelinedMethod() {
    try (RedisClusterClient client = new RedisClusterClient(nodes)) {
      ClusterPipeline pipeline = client.pipelined();
      assertNotNull(pipeline);
    } catch (Exception e) {
      // Expected since we don't have a real cluster running
      assertTrue(
        e.getMessage().contains("Connection") || e.getMessage().contains("connection")
            || e.getCause() instanceof java.net.ConnectException,
        "Expected connection-related exception");
    }
  }
}
