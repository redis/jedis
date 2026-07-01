package redis.clients.jedis.sch;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Client-agnostic SCH (Smart Client Handoff) rebind conformance suite. Subclasses bind a client
 * variant (standalone {@code RedisClient}, {@code MultiDbClient}, ...) by implementing
 * {@link #createClient} and {@link #poolOf}; every variant runs the same MOVING redirect, eviction,
 * relaxation and revert assertions against two {@link TcpMockServer}s, so no client can pass while
 * silently mis-wiring an SCH effect.
 */
@Tag("sch")
public abstract class AbstractRebindBehaviorTest {

  protected TcpMockServer mockServer1;
  protected TcpMockServer mockServer2;

  protected final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
      .socketTimeoutMillis(5000).protocol(RedisProtocol.RESP3).build();

  protected final MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig
      .builder().mode(MaintenanceNotificationsConfig.Mode.AUTO).build();

  protected HostAndPort server1Address;
  protected HostAndPort server2Address;

  /**
   * Build the client under test for {@code hostAndPort} with the given maintenance and pool config.
   */
  protected abstract UnifiedJedis createClient(HostAndPort hostAndPort,
      JedisClientConfig clientConfig, MaintenanceNotificationsConfig maintConfig,
      GenericObjectPoolConfig<Connection> poolConfig);

  /** Test-only access to the pool backing {@code client}'s active endpoint (no public API). */
  protected abstract Pool<Connection> poolOf(UnifiedJedis client);

  @BeforeEach
  public void setUp() throws IOException {
    mockServer1 = new TcpMockServer();
    mockServer1.start();
    mockServer2 = new TcpMockServer();
    mockServer2.start();
    // 127.0.0.1 (not "localhost") so the receiver's resolved peer and the configured host resolve
    // identically — avoids the IPv4/IPv6 split the post-DNS SocketAddressMapper compares strictly.
    server1Address = new HostAndPort("127.0.0.1", mockServer1.getPort());
    server2Address = new HostAndPort("127.0.0.1", mockServer2.getPort());
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer1 != null) {
      mockServer1.stop();
    }
    if (mockServer2 != null) {
      mockServer2.stop();
    }
  }

  @Test
  public void testProactiveRebind() throws Exception {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (
        UnifiedJedis client = createClient(server1Address, clientConfig, maintConfig, poolConfig)) {
      assertEquals("PONG", client.ping());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // Triggers the MOVING read and rebind; the next connection targets server2.
      assertEquals("PONG", client.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      mockServer1.stop();
      assertEquals("PONG", client.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testActiveConnectionShouldBeDisposedOnRebind() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (
        UnifiedJedis client = createClient(server1Address, clientConfig, maintConfig, poolConfig)) {
      Pool<Connection> pool = poolOf(client);

      Connection activeConnection = pool.getResource();
      assertEquals(1, pool.getNumActive());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(0, pool.getNumIdle());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // Active connection stays usable until closed, then is discarded (not pooled).
      assertTrue(activeConnection.ping());
      activeConnection.close();
      assertEquals(1, pool.getDestroyedCount());
      assertEquals(0, pool.getNumActive());

      await().atMost(Duration.ofSeconds(1)).pollInterval(Duration.ofMillis(20))
          .until(() -> mockServer1.getConnectedClientCount() == 0);
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      assertEquals("PONG", client.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testIdleConnectionShouldBeDisposedOnRebind() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(2);
    poolConfig.setMinIdle(1);

    try (
        UnifiedJedis client = createClient(server1Address, clientConfig, maintConfig, poolConfig)) {
      Pool<Connection> pool = poolOf(client);

      Connection activeConnection = pool.getResource();
      Connection idleConnection = pool.getResource();
      idleConnection.close();

      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      mockServer1.sendPushMessageToAll("MOVING", 30, 60, "localhost:" + mockServer2.getPort());

      // Command on the active connection triggers the rebind; idle connections are evicted.
      assertTrue(activeConnection.ping());
      assertEquals(0, pool.getNumIdle());
      assertEquals(1, pool.getNumActive());

      await().atMost(Duration.ofSeconds(1)).pollInterval(Duration.ofMillis(20))
          .until(() -> mockServer1.getConnectedClientCount() == 1);
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      assertEquals("PONG", client.ping());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testNewPoolConnectionsCreatedAgainstMovingTarget() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (
        UnifiedJedis client = createClient(server1Address, clientConfig, maintConfig, poolConfig)) {
      Pool<Connection> pool = poolOf(client);

      Connection activeConnection = pool.getResource();
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      assertTrue(activeConnection.ping());
      activeConnection.close();

      // A fresh pooled connection must land on server2 (post-DNS SocketAddressMapper remap).
      Connection newConnection = pool.getResource();
      assertTrue(newConnection.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testPoolConnectionsWithProactiveRebindDisabled() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();

    MaintenanceNotificationsConfig disabledMaint = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.DISABLED).build();
    try (UnifiedJedis client = createClient(server1Address, clientConfig, disabledMaint,
      poolConfig)) {
      Pool<Connection> pool = poolOf(client);

      Connection activeConnection = pool.getResource();
      Connection idleConnection = pool.getResource();
      idleConnection.close();

      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // Disabled: the MOVING is ignored — no discard, no redirect.
      assertTrue(activeConnection.ping());
      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testRebindRevertsToOriginalEndpointAfterTtl() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (
        UnifiedJedis client = createClient(server1Address, clientConfig, maintConfig, poolConfig)) {
      assertEquals("PONG", client.ping());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // MOVING to server2 with a short 1s grace window.
      mockServer1.sendPushMessageToAll("MOVING", 30, 1, server2Address.toString());

      assertEquals("PONG", client.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      // After the window expires, new connections target the ORIGINAL endpoint again; clear()
      // simulates the natural turnover the design relies on for re-spread.
      Pool<Connection> pool = poolOf(client);
      await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(200))
          .untilAsserted(() -> {
            pool.clear();
            assertEquals("PONG", client.ping());
            assertEquals(1, mockServer1.getConnectedClientCount(),
              "after TTL expiry, new connections must target the original endpoint");
            assertEquals(0, mockServer2.getConnectedClientCount());
          });
    }
  }
}
