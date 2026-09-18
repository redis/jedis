package redis.clients.jedis.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.util.Pool;

/**
 * Tests for {@link PooledConnectionProvider}.
 */
public class PooledConnectionProviderIT {

  private static EndpointConfig endpoint;
  private PooledConnectionProvider provider;

  @BeforeAll
  public static void setUpClass() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @BeforeEach
  public void setUp() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(2);
    provider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), poolConfig);
  }

  @AfterEach
  public void tearDown() {
    if (provider != null) {
      provider.close();
    }
  }

  /**
   * Verifies that getConnectionMap() returns a Pool, not a Connection. This prevents connection
   * leaks when callers expect pool-based connections.
   */
  @Test
  public void getConnectionMapReturnsPool() {
    Map<?, ?> connectionMap = provider.getConnectionMap();

    assertEquals(1, connectionMap.size());
    Object value = connectionMap.values().iterator().next();
    assertThat(value, instanceOf(Pool.class));
    assertThat(value, sameInstance(provider.getPool()));
  }

  /**
   * Verifies that getPrimaryNodesConnectionMap() returns a Pool, not a Connection. This is the fix
   * for the connection leak issue - previously it fell through to the default interface
   * implementation which borrowed a connection from the pool.
   */
  @Test
  public void getPrimaryNodesConnectionMapReturnsPool() {
    Map<?, ?> connectionMap = provider.getPrimaryNodesConnectionMap();

    assertEquals(1, connectionMap.size());
    Object value = connectionMap.values().iterator().next();
    assertThat(value, instanceOf(Pool.class));
    assertThat(value, sameInstance(provider.getPool()));
  }

  /**
   * Verifies that getPrimaryNodesConnectionMap() does not leak connections. With a pool of size 1,
   * if getPrimaryNodesConnectionMap() borrowed a connection without returning it, subsequent
   * getConnection() calls would block/fail.
   */
  @Test
  @Timeout(value = 1)
  public void getPrimaryNodesConnectionMapDoesNotLeakConnections() {
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    try (PooledConnectionProvider sut = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), config)) {

      Pool<?> pool = sut.getPool();
      assertThat(pool.getNumActive(), equalTo(0));

      // Call getPrimaryNodesConnectionMap() - should NOT borrow a connection
      Map<?, ?> cm = sut.getPrimaryNodesConnectionMap();

      // Should have one entry with the pool
      assertEquals(1, cm.size());
      // No connections should be borrowed
      assertThat(pool.getNumActive(), equalTo(0));

      // Should still be able to get a connection since none are leaked
      try (redis.clients.jedis.Connection conn = sut.getConnection()) {
        assertThat(pool.getNumActive(), equalTo(1));
        conn.ping();
      }

      // Connection returned to pool
      assertThat(pool.getNumActive(), equalTo(0));
    }
  }

  /**
   * Verifies that the connection map key is the HostAndPort.
   */
  @Test
  public void connectionMapKeyIsHostAndPort() {
    Map<?, ?> connectionMap = provider.getPrimaryNodesConnectionMap();

    Object key = connectionMap.keySet().iterator().next();
    assertThat(key, instanceOf(HostAndPort.class));
    assertThat(key, equalTo(endpoint.getHostAndPort()));
  }
}
