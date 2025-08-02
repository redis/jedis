package redis.clients.jedis.upgrade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceEventHandler;
import redis.clients.jedis.MaintenanceEventHandlerImpl;
import redis.clients.jedis.MaintenanceEventListener;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.TimeoutOptions;
import redis.clients.jedis.util.ReflectionTestUtils;
import redis.clients.jedis.util.server.CommandHandler;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

/**
 * Test that connection adaptive timeout works as expected. Usses a mock TCP server to send
 * Maintenance push messages to the client in controllable manner.
 */
@Tag("upgrade")
public class ConnectionAdaptiveTimeoutTest {

  private final int originalTimeoutMs = 2000;
  private final Duration relaxedTimeout = Duration.ofSeconds(10);
  private final Duration relaxedBlockingTimeout = Duration.ofSeconds(15);
  private final CommandObjects commandObjects = new CommandObjects();
  private final CommandHandler mockHandler = Mockito.mock(CommandHandler.class);
  private TcpMockServer mockServer;
  private Connection connection;

  @BeforeEach
  public void setUp() throws IOException {
    // Start the mock TCP server
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler(mockHandler);
    mockServer.start();

    // Create client configuration with relaxed timeout and maintenance event handler
    TimeoutOptions timeoutOptions = TimeoutOptions.builder()
        .proactiveTimeoutsRelaxing(relaxedTimeout)
        .proactiveBlockingTimeoutsRelaxing(relaxedBlockingTimeout).build();

    MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

    MaintenanceEventListener testListener = new MaintenanceEventListener() {
      @Override
      public void onMigrating() {
        System.out.println("MIGRATING");
      }
    };
    maintenanceEventHandler.addListener(testListener);

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs).timeoutOptions(timeoutOptions)
        .maintenanceEventHandler(maintenanceEventHandler).protocol(RedisProtocol.RESP3).build();

    // Create connection to the mock server
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
    connection = new Connection(hostAndPort, clientConfig);
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (connection != null && connection.isConnected()) {
      connection.close();
    }
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Test
  public void testMigratingPushMessage() throws SocketException {
    Socket socket = ReflectionTestUtils.getField(connection, "socket");

    assertTrue(connection.isConnected());
    assertEquals(originalTimeoutMs, connection.getSoTimeout());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    // First send MIGRATING to activate relaxed timeout
    mockServer.sendMigratingPushToAll();
    assertTrue(connection.ping());
    assertTrue(connection.isRelaxedTimeoutActive());
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

    mockServer.sendMigratedPushToAll();
    assertTrue(connection.ping());
    assertFalse(connection.isRelaxedTimeoutActive());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
  }

  @Test
  public void testFailoverPushMessage() throws SocketException {
    Socket socket = ReflectionTestUtils.getField(connection, "socket");

    assertTrue(connection.isConnected());
    assertEquals(originalTimeoutMs, connection.getSoTimeout());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    // First send MIGRATING to activate relaxed timeout
    mockServer.sendFailingOverPushToAll();
    assertTrue(connection.ping());
    assertTrue(connection.isRelaxedTimeoutActive());
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

    mockServer.sendFailedOverPushToAll();
    assertTrue(connection.ping());
    assertFalse(connection.isRelaxedTimeoutActive());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
  }

  @Test
  public void testDisabledTimeoutRelaxationDoesNotApplyRelaxedTimeout() throws Exception {
    // Create a connection with disabled timeout relaxation
    Connection disabledConnection = createConnectionWithDisabledTimeoutRelaxation();
    Socket disabledSocket = ReflectionTestUtils.getField(disabledConnection, "socket");

    try {
      assertTrue(disabledConnection.isConnected());
      assertEquals(originalTimeoutMs, disabledConnection.getSoTimeout());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Verify that relaxed timeout is disabled
      assertFalse(disabledConnection.isRelaxedTimeoutActive());

      // Send MIGRATING push message - should NOT activate relaxed timeout
      mockServer.sendMigratingPushToAll();

      assertTrue(disabledConnection.ping());

      // Verify that relaxed timeout was NOT activated
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Send FAILING_OVER push message - should also NOT activate relaxed timeout
      mockServer.sendFailingOverPushToAll();

      assertTrue(disabledConnection.ping());

      // Verify that relaxed timeout is still NOT activated
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Send MIGRATED and FAILED_OVER messages - timeout should remain unchanged
      mockServer.sendMigratedPushToAll();
      mockServer.sendFailedOverPushToAll();

      assertTrue(disabledConnection.ping());
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

    } finally {
      if (disabledConnection.isConnected()) {
        disabledConnection.close();
      }
    }
  }

  @Test
  public void testManualRelaxTimeoutsCallWithDisabledTimeoutRelaxation() throws Exception {
    // Create a connection with disabled timeout relaxation
    Connection disabledConnection = createConnectionWithDisabledTimeoutRelaxation();
    Socket disabledSocket = ReflectionTestUtils.getField(disabledConnection, "socket");

    try {
      assertTrue(disabledConnection.isConnected());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Manually call relaxTimeouts() - should have no effect when disabled
      disabledConnection.relaxTimeouts();

      // Relaxed timeout should fallback to original timeout, if relaxed timeout is disabled
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Verify connection still works
      assertTrue(disabledConnection.ping());

    } finally {
      if (disabledConnection.isConnected()) {
        disabledConnection.close();
      }
    }
  }

  @Test
  public void testDefaultTimeoutOptionsDisablesRelaxedTimeout() throws Exception {
    // Create a connection with null timeout options
    Connection defaultTimeoutConnection = createConnectionWithDefaultTimeoutOptions();
    Socket nullTimeoutSocket = ReflectionTestUtils.getField(defaultTimeoutConnection, "socket");

    try {
      assertTrue(defaultTimeoutConnection.isConnected());
      assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

      // Verify that relaxed timeout is disabled
      assertFalse(defaultTimeoutConnection.isRelaxedTimeoutActive());

      // Send maintenance push messages - should NOT activate relaxed timeout
      mockServer.sendMigratingPushToAll();

      assertTrue(defaultTimeoutConnection.ping());

      // Relaxed timeout's are disabled by default
      assertFalse(defaultTimeoutConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

      // Manual call should also have no effect
      defaultTimeoutConnection.relaxTimeouts();
      assertFalse(defaultTimeoutConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

    } finally {
      if (defaultTimeoutConnection.isConnected()) {
        defaultTimeoutConnection.close();
      }
    }
  }

  @Test
  public void testRelaxedBlockingTimeoutAppliedDuringBlockingCommand()
      throws IOException, InterruptedException {

    // Verify initial timeout
    Socket socket = ReflectionTestUtils.getField(connection, "socket");
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    CountDownLatch blpopLatch = new CountDownLatch(1);
    CountDownLatch blpopLatchAfter = new CountDownLatch(1);
    doAnswer(invocation -> {
      blpopLatch.countDown();
      return RespResponse.arrayOfBulkStrings("popped-item");
    }).when(mockHandler).handleCommand(eq("BLPOP"), anyList(), anyString());

    // Send MIGRATING push notification which should trigger relaxTimeouts()
    mockServer.sendMigratingPushToAll();

    Thread t1 = new Thread(() -> {
      connection.executeCommand(commandObjects.blpop(5, "test:blpop:key"));
      blpopLatchAfter.countDown();
    });
    t1.start();

    // Verify that relaxed blocking timeout was applied
    blpopLatch.await();
    assertTrue(connection.isRelaxedTimeoutActive(),
      "Relaxed timeout should be active during blocking command");
    assertEquals((int) relaxedBlockingTimeout.toMillis(), socket.getSoTimeout(),
      "Socket timeout should be relaxed blocking timeout during blocking command");

    blpopLatchAfter.await();
    assertTrue(connection.isRelaxedTimeoutActive(),
      "Relaxed timeout should be still active after blocking command");
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout(),
      "Socket timeout should be restored to relaxed timeout for non blocking command");

    // Send MIGRATED push notification to disable relaxed timeout
    mockServer.sendMigratedPushToAll();
    connection.executeCommand(commandObjects.ping());

    assertFalse(connection.isRelaxedTimeoutActive(),
      "Relaxed timeout should be disabled after MIGRATED");
    assertEquals(originalTimeoutMs, socket.getSoTimeout(),
      "Socket timeout should be restored to original timeout");
  }

  /**
   * Helper method to create a connection with disabled timeout relaxation.
   */
  private Connection createConnectionWithDisabledTimeoutRelaxation() {
    // Create configuration with disabled timeout relaxation
    TimeoutOptions disabledTimeoutOptions = TimeoutOptions.builder()
        .proactiveTimeoutsRelaxing(TimeoutOptions.DISABLED_TIMEOUT)
        .proactiveBlockingTimeoutsRelaxing(TimeoutOptions.DISABLED_TIMEOUT).build();

    MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs).timeoutOptions(disabledTimeoutOptions)
        .maintenanceEventHandler(maintenanceEventHandler).protocol(RedisProtocol.RESP3).build();

    // Create connection to the mock server
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
    Connection disabledConnection = new Connection(hostAndPort, clientConfig);
    disabledConnection.connect();

    return disabledConnection;
  }

  /**
   * Helper method to create a connection with null timeout options.
   */
  private Connection createConnectionWithDefaultTimeoutOptions() {
    MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs)
        // Note: not setting timeoutOptions, so it will be null
        .maintenanceEventHandler(maintenanceEventHandler).protocol(RedisProtocol.RESP3).build();

    // Create connection to the mock server
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
    Connection nullTimeoutConnection = new Connection(hostAndPort, clientConfig);
    nullTimeoutConnection.connect();

    return nullTimeoutConnection;
  }

}
