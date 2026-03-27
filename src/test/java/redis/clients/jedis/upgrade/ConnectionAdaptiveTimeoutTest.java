package redis.clients.jedis.upgrade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.TimeoutOptions;
import redis.clients.jedis.util.ReflectionTestUtil;
import redis.server.stub.RedisServerStub;
import redis.server.stub.MaintenanceEvent;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.Protocol.Command.PING;

/**
 * Test that connection adaptive timeout works as expected. Uses a mock TCP server to send
 * Maintenance push messages to the client in controllable manner.
 */
@Tag("sch")
public class ConnectionAdaptiveTimeoutTest {

  private final int originalTimeoutMs = 2000;
  private final Duration relaxedTimeout = Duration.ofSeconds(10);
  private final Duration relaxedBlockingTimeout = Duration.ofSeconds(15);
  private final CommandObjects commandObjects = new CommandObjects();
  private RedisServerStub mockServer;
  private Connection connection;

  @BeforeEach
  public void setUp() throws IOException {
    // Start the mock TCP server
    mockServer = new RedisServerStub();
    mockServer.start();

    // Create client configuration with relaxed timeout and maintenance event handler
    TimeoutOptions timeoutOptions = TimeoutOptions.builder()
        .proactiveTimeoutsRelaxing(relaxedTimeout)
        .proactiveBlockingTimeoutsRelaxing(relaxedBlockingTimeout).build();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs).timeoutOptions(timeoutOptions)
        .protocol(RedisProtocol.RESP3).build();

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
    Socket socket = ReflectionTestUtil.getField(connection, "socket");

    assertTrue(connection.isConnected());
    assertEquals(originalTimeoutMs, connection.getSoTimeout());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    // First send MIGRATING to activate relaxed timeout
    mockServer.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "1"));
    assertTrue(connection.ping());
    assertTrue(connection.isRelaxedTimeoutActive());
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

    mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(2, "1"));
    assertTrue(connection.ping());
    assertFalse(connection.isRelaxedTimeoutActive());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
  }

  @Test
  public void testFailoverPushMessage() throws SocketException {
    Socket socket = ReflectionTestUtil.getField(connection, "socket");

    assertTrue(connection.isConnected());
    assertEquals(originalTimeoutMs, connection.getSoTimeout());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    mockServer.sendPushMessageToAll(MaintenanceEvent.failingOver(1, 30, "1"));
    assertTrue(connection.ping());
    assertTrue(connection.isRelaxedTimeoutActive());
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

    mockServer.sendPushMessageToAll(MaintenanceEvent.failedOver(2, "1"));
    assertTrue(connection.ping());
    assertFalse(connection.isRelaxedTimeoutActive());
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
  }

  @Test
  public void testDisabledTimeoutRelaxationDoesNotApplyRelaxedTimeout() throws Exception {
    // Create a connection with disabled timeout relaxation
    Connection disabledConnection = createConnectionWithDisabledTimeoutRelaxation();
    Socket disabledSocket = ReflectionTestUtil.getField(disabledConnection, "socket");

    try {
      assertTrue(disabledConnection.isConnected());
      assertEquals(originalTimeoutMs, disabledConnection.getSoTimeout());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Verify that relaxed timeout is disabled
      assertFalse(disabledConnection.isRelaxedTimeoutActive());

      // Send MIGRATING push message - should NOT activate relaxed timeout
      mockServer.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "1"));

      assertTrue(disabledConnection.ping());

      // Verify that relaxed timeout was NOT activated
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Send FAILING_OVER push message - should also NOT activate relaxed timeout
      mockServer.sendPushMessageToAll(MaintenanceEvent.failingOver(1, 30, "1"));

      assertTrue(disabledConnection.ping());

      // Verify that relaxed timeout is still NOT activated
      assertFalse(disabledConnection.isRelaxedTimeoutActive());
      assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

      // Send MIGRATED and FAILED_OVER messages - timeout should remain unchanged
      mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(2, "1"));
      mockServer.sendPushMessageToAll(MaintenanceEvent.failedOver(2, "1"));

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
    Socket disabledSocket = ReflectionTestUtil.getField(disabledConnection, "socket");

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
    Socket nullTimeoutSocket = ReflectionTestUtil.getField(defaultTimeoutConnection, "socket");

    try {
      assertTrue(defaultTimeoutConnection.isConnected());
      assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

      // Verify that relaxed timeout is disabled
      assertFalse(defaultTimeoutConnection.isRelaxedTimeoutActive());

      // Send maintenance push messages - should NOT activate relaxed timeout
      mockServer.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "1"));

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
  public void testRelaxedBlockingTimeoutAppliedDuringBlockingCommand() throws IOException {

    // Step 1: Verify initial timeout
    Socket socket = ReflectionTestUtil.getField(connection, "socket");
    assertEquals(originalTimeoutMs, socket.getSoTimeout());

    // Send MIGRATING push notification which should trigger relaxTimeouts()
    mockServer.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "1"));

    // Step 2: Verify that relaxed timeout is activated by blocking commands

    // TODO : currently we don't have any blocking command supported by mock server.
    // test uses command interceptor to block command execution for PING command
    // once we have blocking command supported by mock server, we should use that
    CommandObject<String> blockingCommand1 = new CommandObject<>(
        new CommandArguments(PING).blocking(), BuilderFactory.STRING);
    connection.executeCommand(blockingCommand1);

    assertTrue(connection.isRelaxedTimeoutActive(), "Relaxed timeout should be activated");
    assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout(),
      "Socket timeout should be restored to relaxed timeout for non blocking command");

    // Send MIGRATED push notification to disable relaxed timeout
    mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(2, "1"));
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

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs).timeoutOptions(disabledTimeoutOptions)
        .protocol(RedisProtocol.RESP3).build();

    // Create connection to the mock server (connection is established in constructor)
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
    return new Connection(hostAndPort, clientConfig);
  }

  /**
   * Helper method to create a connection with null timeout options.
   */
  private Connection createConnectionWithDefaultTimeoutOptions() {
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(originalTimeoutMs)
        // Note: not setting timeoutOptions, so it will be null
        .protocol(RedisProtocol.RESP3).build();

    // Create connection to the mock server (connection is established in constructor)
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
    return new Connection(hostAndPort, clientConfig);
  }

}
