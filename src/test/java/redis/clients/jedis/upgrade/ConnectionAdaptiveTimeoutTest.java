package redis.clients.jedis.upgrade;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import redis.server.stub.RedisServerStubConfig;
import redis.server.stub.MaintenanceEvent;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
  private final int originalInfiniteTimeoutMs = 0;
  private final Duration relaxedTimeout = Duration.ofSeconds(10);
  private final Duration relaxedBlockingTimeout = Duration.ofSeconds(15);
  private final CommandObjects commandObjects = new CommandObjects();
  private RedisServerStub mockServer;
  private Connection connection;

  @BeforeEach
  public void setUp() throws IOException {
    // Start the mock TCP server
    mockServer = new RedisServerStub(RedisServerStubConfig.builder().build());
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
  @Timeout(value = 1, unit = TimeUnit.SECONDS)
  public void testRelaxedBlockingTimeoutAppliedDuringBlockingCommand()
      throws IOException, InterruptedException {

    CountDownLatch blockingCommandLatch = new CountDownLatch(1);

    mockServer.beforeCommand(PING.name(), (args, ctx) -> {
      // send maintenance event before blocking for outou
      mockServer.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "1"));
      blockingCommandLatch.await();
    });

    // Step 1: Verify initial timeout
    Socket socket = ReflectionTestUtil.getField(connection, "socket");
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
    assertFalse(connection.isRelaxedTimeoutActive(),
      "Relaxed timeout should not be activated initially");

    // Step 2: Verify that relaxed timeout is activated by blocking commands

    // TODO : currently we don't have any blocking command supported by mock server.
    // test uses command interceptor to block command execution for PING command
    // once we have blocking command supported by mock server, we should use that
    CommandObject<String> blockingCommand1 = new CommandObject<>(
        new CommandArguments(PING).blocking(), BuilderFactory.STRING);

    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
      String response = connection.executeCommand(blockingCommand1);
      assertEquals("PONG", response);
    });

    // await command to be executed
    Awaitility.await().atMost(Duration.ofSeconds(100))
        .until(() -> connection.isRelaxedTimeoutActive());
    assertTrue(connection.isRelaxedTimeoutActive(), "Relaxed timeout should be activated");
    assertEquals(relaxedBlockingTimeout.toMillis(), socket.getSoTimeout(),
      "Socket timeout should be set to blocking relaxed timeout for blocking command");

    // Step 3: Verify that relaxed timeout is de-activated if MIGRATED is received
    // during on-going blocking command

    // Send MIGRATED push notification to disable relaxed timeout
    // Command is not yet completed
    mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(2, "1"));
    Awaitility.await().atMost(Duration.ofSeconds(100))
        .until(() -> !connection.isRelaxedTimeoutActive());
    assertFalse(connection.isRelaxedTimeoutActive(),
      "Relaxed timeout should be disabled after MIGRATED");
    assertEquals(originalInfiniteTimeoutMs, socket.getSoTimeout(),
      "Socket timeout should be restored to original blocking timeout");

    // Complete the command
    blockingCommandLatch.countDown();
    future.join();

    // Step 4: Verify that blocking timeout is de-activated after command completion
    // After command completion, non-blocking timeout should be restored
    assertEquals(originalTimeoutMs, socket.getSoTimeout());
    assertFalse(connection.isRelaxedTimeoutActive());
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
