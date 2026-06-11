package redis.clients.jedis.sch;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Test that UnifiedJedis proactively rebinds to new target when receiving MOVING notifications.
 * Uses mock TCP servers to simulate Redis cluster slot migration scenarios.
 */
@Tag("sch")
public class RebindMockTest {

  private TcpMockServer mockServer1;
  private TcpMockServer mockServer2;

  private final int socketTimeoutMs = 5000;

  DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
      .socketTimeoutMillis(socketTimeoutMs).protocol(RedisProtocol.RESP3)
      .maintNotificationsConfig(
        MaintenanceNotificationsConfig.builder().mode(MaintenanceNotificationsConfig.Mode.AUTO) // Enable
                                                                                                // maintenance
                                                                                                // notifications
                                                                                                // (includes
                                                                                                // proactive
                                                                                                // rebind)
            .build())
      .build();

  HostAndPort server1Address;
  HostAndPort server2Address;

  @BeforeEach
  public void setUp() throws IOException {
    // Start tcpmockedserver1
    mockServer1 = new TcpMockServer();
    mockServer1.start();

    // Start tcpmockedserver2
    mockServer2 = new TcpMockServer();
    mockServer2.start();

    // Use 127.0.0.1 (not "localhost") so the receiver's resolved peer and the configured host's
    // resolution match deterministically — avoids the IPv4/IPv6 split that the new post-DNS
    // SocketAddressMapper compares strictly.
    server1Address = new HostAndPort("127.0.0.1", mockServer1.getPort());
    server2Address = new HostAndPort("127.0.0.1", mockServer2.getPort());

    System.out.println("MockServer1 started on port: " + mockServer1.getPort());
    System.out.println("MockServer2 started on port: " + mockServer2.getPort());
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
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    connectionPoolConfig.setMaxTotal(1);

    // 1. Create UnifiedJedis client and connect it to mockedserver1=
    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {

      // 1. Perform a PING command to initiate a connection
      String response1 = unifiedJedis.ping();
      assertEquals("PONG", response1);

      // Verify initial connection to server1
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. Send MOVING notification on server1 -> MOVING 30 localhost:port2
      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // 3. Perform PING command
      // This should trigger read of the MOVING notification and rebind to server2
      // the ping command itself should be executed against server1
      // the used connection should be closed after the ping command is executed
      String response2 = unifiedJedis.ping();
      assertEquals("PONG", response2);

      // Verify initial connection to server1
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      // drop connection to server1 to ensure server1
      mockServer1.stop();

      // 4. Perform PING command
      // Folowup ping command should be executed against server2

      String response3 = unifiedJedis.ping();
      assertEquals("PONG", response3);

      // Verify that connection has moved to server2
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testActiveConnectionShouldBeDisposedOnRebind() {
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    connectionPoolConfig.setMaxTotal(1);

    // 1. Create UnifiedJedis client and connect it to mockedserver1
    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {
      Pool<Connection> pool = unifiedJedis.getPool();

      // 1. Test setup - 1 active connection, 0 idle connection
      Connection activeConnection = unifiedJedis.getPool().getResource();
      assertEquals(1, pool.getNumActive());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(0, pool.getNumIdle());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. Send MOVING notification on server1 -> MOVING 30 localhost:port2
      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // 3. Active connection should be still usable until closed and returned to the pools
      assertTrue(activeConnection.ping());

      // 4. When closed connection should be disposed and not returned to the pool
      activeConnection.close();
      assertEquals(1, pool.getDestroyedCount());
      assertEquals(0, pool.getNumActive());

      // 5. Wait for connection to be closed on server1
      await().pollDelay(Duration.ofMillis(1)).timeout(Duration.ofMillis(10))
          .until(() -> mockServer1.getConnectedClientCount() == 0);
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      // 6. Next command should create a new connection to server2
      String response2 = unifiedJedis.ping();
      assertEquals("PONG", response2);
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testIdleConnectionShouldBeDisposedOnRebind() {
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    connectionPoolConfig.setMaxTotal(2);
    connectionPoolConfig.setMinIdle(1);

    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {
      Pool<Connection> pool = unifiedJedis.getPool();

      // 1. Test setup - 1 active connection, 1 idle connection
      Connection activeConnection = unifiedJedis.getPool().getResource();
      Connection idleConnection = unifiedJedis.getPool().getResource();
      idleConnection.close();

      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. Send MOVING notification on server1 -> MOVING 30 localhost:port2
      String server2Address = "localhost:" + mockServer2.getPort();
      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address);

      // 3. perform a command on active connection to trigger rebind
      assertTrue(activeConnection.ping());

      // 4. All IDLE connection's should be closed & disposed
      assertEquals(0, pool.getNumIdle());
      assertEquals(1, pool.getNumActive());

      // 5. Wait for connection to be closed on server1
      await().pollDelay(Duration.ofMillis(1)).timeout(Duration.ofMillis(10))
          .until(() -> mockServer1.getConnectedClientCount() == 1);
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 6. Next command should create a new connection to server2
      String response2 = unifiedJedis.ping();
      assertEquals("PONG", response2);
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testNewPoolConnectionsCreatedAgainstMovingTarget() {
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    connectionPoolConfig.setMaxTotal(1);

    // Create UnifiedJedis with connection pooling enabled
    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {

      // 1. Test setup - 1 active connection
      Connection activeConnection = unifiedJedis.getPool().getResource();

      // Verify initial connection to server1
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. Send MOVING notification on server1 -> MOVING 30 localhost:port2
      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // 3. perform a command on active connection to trigger rebind
      assertTrue(activeConnection.ping());
      activeConnection.close();

      // 4. Initiate a new connection from the pool
      Connection newConnection = unifiedJedis.getPool().getResource();
      assertTrue(newConnection.ping());

      // Verify that the new connection landed on server2 (the SocketAddressMapper remapped the
      // resolved peer). Under the new post-DNS design Connection.getHostAndPort() returns the
      // configured value; connected-client counts are the source of truth.
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testPoolConnectionsWithProactiveRebindDisabled() {
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();

    // Verify that with maintenance notifications disabled, connections stay on original server
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .from(this.clientConfig)
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder()
            .mode(MaintenanceNotificationsConfig.Mode.DISABLED) // Disable maintenance notifications
            .build())
        .build();
    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {
      Pool<Connection> pool = unifiedJedis.getPool();

      // 1. Test setup - 1 active connection, 1 idle connection
      Connection activeConnection = unifiedJedis.getPool().getResource();
      Connection idleConnection = unifiedJedis.getPool().getResource();
      idleConnection.close();

      // Verify initial connection to server1
      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. Send MOVING notification on server1 -> MOVING 30 localhost:port2
      mockServer1.sendPushMessageToAll("MOVING", 30, 60, server2Address.toString());

      // 3. Perform PING command
      // This should trigger read of the MOVING notification processing
      assertTrue(activeConnection.ping());

      // Verify initial connection to server1
      assertEquals(1, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(0, pool.getDestroyedCount());
      assertEquals(2, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());
    }
  }

  @Test
  public void testRebindRevertsToOriginalEndpointAfterTtl() {
    ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    connectionPoolConfig.setMaxTotal(1);

    try (RedisClient unifiedJedis = RedisClient.builder().poolConfig(connectionPoolConfig)
        .clientConfig(clientConfig).hostAndPort(server1Address).build()) {

      // 1. Initial connection on server1
      assertEquals("PONG", unifiedJedis.ping());
      assertEquals(1, mockServer1.getConnectedClientCount());
      assertEquals(0, mockServer2.getConnectedClientCount());

      // 2. MOVING to server2 with a short 1s grace window
      mockServer1.sendPushMessageToAll("MOVING", 30, 1, server2Address.toString());

      // 3. Within the grace window the rebind takes effect: connection moves to server2
      assertEquals("PONG", unifiedJedis.ping());
      assertEquals(0, mockServer1.getConnectedClientCount());
      assertEquals(1, mockServer2.getConnectedClientCount());

      // 4. After the grace window expires, new connections target the ORIGINAL endpoint again.
      // clear() simulates the natural connection turnover the design relies on for re-spread.
      Pool<Connection> pool = unifiedJedis.getPool();
      await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(200))
          .untilAsserted(() -> {
            pool.clear();
            assertEquals("PONG", unifiedJedis.ping());
            assertEquals(1, mockServer1.getConnectedClientCount(),
              "after TTL expiry, new connections must target the original endpoint");
            assertEquals(0, mockServer2.getConnectedClientCount());
          });
    }
  }

}
