package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static redis.clients.jedis.TimeoutOptions.DISABLED_TIMEOUT_MS;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.util.ReflectionTestUtil;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.CommandHandler;
import redis.clients.jedis.util.server.MaintenanceEvent;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for Connection that don't require a real Redis server. Uses TcpMockServer to simulate
 * Redis protocol.
 * <p>
 * These tests verify Connection base class behavior (default push consumer registration and push
 * notification handling).
 * </p>
 */
public class ConnectionMockTest {

  private TcpMockServer mockServer;
  private final CommandHandler mockHandler = Mockito.mock(CommandHandler.class);

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler(mockHandler);
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Nested
  class PushNotificationHandling {

    @Test
    public void pubSubConsumerRegisteredWithConfigConstructor() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = new Connection(socketFactory, config);

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify only PUBSUB_CONSUMER is registered by default
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }

    @Test
    public void pubSubConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
          .build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify only PUBSUB_CONSUMER is registered by default
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }

    @Test
    public void arbitraryPushNotificationDoesNotBreakConnection() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      try (Connection conn = new Connection(socketFactory, config)) {

        // Send arbitrary push notification (not pub/sub related)
        mockServer.sendPushMessageToAll("ARBITRARY_PUSH", "arg1", "arg2");

        // Execute command after receiving arbitrary push notification
        // If push notification handling is broken, this will throw an exception
        assertDoesNotThrow(() -> conn.ping(),
          "PING after arbitrary push notification should not throw exception");

        assertTrue(conn.ping(), "PING should succeed");

        // Verify connection is still healthy
        assertFalse(conn.isBroken(), "Connection should not be broken");
        assertTrue(conn.isConnected(), "Connection should still be connected");
      }
    }

  }

  @Nested
  class MaintenanceEventHandling {

    /**
     * Tests that the MaintenanceEventConsumer is registered when using the constructors not
     * providing a JedisClientConfig.
     */
    @Test
    public void maintenanceConsumerRegisteredConstructorWithConfig() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(timeoutOpts).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
          .maintNotificationsConfig(maintConfig).build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
        instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerRegisteredWithConnectionBuilder() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(timeoutOpts).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
          .maintNotificationsConfig(maintConfig).build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
          .build();

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
        instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerNotRegisteredRelaxedTimeoutDisabled() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
      assertThat(conn.getPushConsumers(),
        not(hasItem(instanceOf(Connection.MaintenanceEventConsumer.class))));
    }
  }

  @Nested
  public class RelaxedTimeoutTest {

    private final int SO_TIMEOUT_MS = 2000;

    private final int RELAXED_TIMEOUT_MS = 10000;

    private final int RELAXED_BLOCKING_TIMEOUT_MS = 15000;

    private final Duration relaxedTimeout = Duration.ofMillis(RELAXED_TIMEOUT_MS);

    private final Duration relaxedBlockingTimeout = Duration.ofMillis(RELAXED_BLOCKING_TIMEOUT_MS);

    private final CommandObjects commandObjects = new CommandObjects();

    private Connection connection;

    @BeforeEach
    public void setUp() throws IOException {
      // Create client configuration with relaxed timeout and maintenance event handler
      TimeoutOptions timeoutOptions = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(relaxedTimeout)
          .proactiveBlockingTimeoutsRelaxing(relaxedBlockingTimeout).build();

      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(timeoutOptions).build();

      DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
          .socketTimeoutMillis(SO_TIMEOUT_MS).maintNotificationsConfig(maintConfig)
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

      assertEquals(SO_TIMEOUT_MS, connection.getSoTimeout());
      assertEquals(RELAXED_TIMEOUT_MS, connection.getRelaxedSoTimeout());

      // First send MIGRATING to activate relaxed timeout
      mockServer
          .sendPushMessageToAll(MaintenanceEvent.migrating(1, 10, Collections.singletonList("1")));
      assertTrue(connection.ping());
      assertTrue(connection.isRelaxedTimeoutActive());
      assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

      mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(1, Collections.singletonList("1")));
      assertTrue(connection.ping());
      assertFalse(connection.isRelaxedTimeoutActive());
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
    }

    @Test
    public void testFailoverPushMessage() throws SocketException {
      Socket socket = ReflectionTestUtil.getField(connection, "socket");

      assertTrue(connection.isConnected());
      assertEquals(SO_TIMEOUT_MS, connection.getSoTimeout());
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());

      // First send MIGRATING to activate relaxed timeout
      mockServer.sendPushMessageToAll(
        MaintenanceEvent.failingOver(1, 10, Collections.singletonList("1")));
      assertTrue(connection.ping());
      assertTrue(connection.isRelaxedTimeoutActive());
      assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

      mockServer
          .sendPushMessageToAll(MaintenanceEvent.failedOver(1, Collections.singletonList("1")));
      assertTrue(connection.ping());
      assertFalse(connection.isRelaxedTimeoutActive());
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
    }

    @Test
    public void testRelaxTimeoutsDisabledFallbackToSoTimeout() throws Exception {
      TimeoutOptions disabledTimeoutOptions = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(TimeoutOptions.DISABLED_TIMEOUT).build();

      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(disabledTimeoutOptions).build();

      DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
          .socketTimeoutMillis(SO_TIMEOUT_MS).maintNotificationsConfig(maintConfig)
          .protocol(RedisProtocol.RESP3).build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      try (Connection conn = new Connection(hostAndPort, clientConfig)) {
        Socket socket = ReflectionTestUtil.getField(conn, "socket");
        assertTrue(conn.isConnected());
        assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
        assertEquals(DISABLED_TIMEOUT_MS, conn.getRelaxedSoTimeout());

        conn.activateRelaxedTimeout();

        // Relaxed timeout should fallback to original timeout, if relaxed timeout is disabled
        assertTrue(conn.isRelaxedTimeoutActive());
        assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
      }
    }

    @Test
    public void testDefaultTimeoutOptions() throws Exception {

      DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
          .socketTimeoutMillis(SO_TIMEOUT_MS)
          .maintNotificationsConfig(MaintenanceNotificationsConfig.DEFAULT)
          .protocol(RedisProtocol.RESP3).build();

      // Create connection to the mock server (connection is established in constructor)
      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());

      Connection conn = new Connection(hostAndPort, clientConfig);
      Socket socket = ReflectionTestUtil.getField(conn, "socket");

      try {
        assertTrue(conn.isConnected());

        assertEquals(SO_TIMEOUT_MS, conn.getSoTimeout());
        assertEquals(0, conn.getBlockingSoTimeout());
        assertEquals(10000, conn.getRelaxedSoTimeout());
        assertEquals(DISABLED_TIMEOUT_MS, conn.getRelaxedBlockingSoTimeout());
        assertFalse(conn.isRelaxedTimeoutActive());

        // verify actual socket timeout
        assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
      } finally {
        if (conn.isConnected()) {
          conn.close();
        }
      }
    }

    @Test
    public void testRelaxedBlockingTimeoutAppliedDuringBlockingCommand()
        throws IOException, InterruptedException {

      // Verify initial timeout
      Socket socket = ReflectionTestUtil.getField(connection, "socket");
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());

      CountDownLatch blpopLatch = new CountDownLatch(1);
      CountDownLatch blpopLatchAfter = new CountDownLatch(1);
      doAnswer(invocation -> {
        blpopLatch.countDown();
        return RespResponse.arrayOfBulkStrings("popped-item");
      }).when(mockHandler).handleCommand(argThat(args -> {
        String cmd = SafeEncoder.encode(args.getCommand().getRaw());
        return "BLPOP".equalsIgnoreCase(cmd);
      }), anyString());

      // Send MIGRATING push notification which should trigger relaxTimeouts()
      mockServer
          .sendPushMessageToAll(MaintenanceEvent.migrating(1, 10, Collections.singletonList("1")));

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
      mockServer.sendPushMessageToAll(MaintenanceEvent.migrated(1, Collections.singletonList("1")));
      connection.executeCommand(commandObjects.ping());

      assertFalse(connection.isRelaxedTimeoutActive(),
        "Relaxed timeout should be disabled after MIGRATED");
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout(),
        "Socket timeout should be restored to original timeout");
    }

  }
}
