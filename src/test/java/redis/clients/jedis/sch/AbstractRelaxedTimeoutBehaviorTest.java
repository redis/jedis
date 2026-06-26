package redis.clients.jedis.sch;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static redis.clients.jedis.ConnectionTestHelper.getRelaxedBlockingSoTimeout;
import static redis.clients.jedis.ConnectionTestHelper.getRelaxedSoTimeout;
import static redis.clients.jedis.ConnectionTestHelper.isRelaxedTimeoutActive;
import static redis.clients.jedis.ConnectionTestHelper.relaxTimeouts;
import static redis.clients.jedis.JedisClientConfig.UNSET_TIMEOUT_MS;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.CommandHandler;
import redis.clients.jedis.util.server.MaintenanceEventMessages;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Shared behavior tests for the relaxed-timeout window: per-receiver MIGRATING / FAILING_OVER /
 * MOVING events apply the relaxed {@code SO_TIMEOUT}; the matching MIGRATED / FAILED_OVER (or the
 * max-duration backstop) reverts it. Concrete subclasses provide the pool and direct-connection
 * construction hooks for the variant under test.
 */
public abstract class AbstractRelaxedTimeoutBehaviorTest {

  protected static final int SO_TIMEOUT_MS = 2000;
  protected static final int RELAXED_TIMEOUT_MS = 10000;
  protected static final int RELAXED_BLOCKING_TIMEOUT_MS = 15000;

  protected final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP3);

  protected TcpMockServer mockServer;
  protected CommandHandler mockHandler;
  protected ConnectionPool pool;
  protected Connection connection;

  /** Build the pool used to borrow the connection under test (variant-specific). */
  protected abstract ConnectionPool createPool(HostAndPort hostAndPort, JedisClientConfig config,
      MaintenanceNotificationsConfig maintConfig);

  /** Build a connection via a public constructor (no pool) — for tests that exercise raw config. */
  protected abstract Connection buildDirect(HostAndPort hostAndPort, JedisClientConfig config,
      MaintenanceNotificationsConfig maintConfig);

  @BeforeEach
  public void schSetUp() throws IOException {
    mockServer = new TcpMockServer();
    mockHandler = Mockito.mock(CommandHandler.class);
    mockServer.setCommandHandler(mockHandler);
    mockServer.start();

    pool = createPool(new HostAndPort("localhost", mockServer.getPort()), defaultClientConfig(),
      defaultMaintConfig());
    connection = pool.getResource();
  }

  @AfterEach
  public void schTearDown() throws IOException {
    if (connection != null && connection.isConnected()) {
      connection.close();
    }
    if (pool != null) {
      pool.close();
    }
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  /** Default client config: RESP3, mock server's port. */
  protected JedisClientConfig defaultClientConfig() {
    return DefaultJedisClientConfig.builder().socketTimeoutMillis(SO_TIMEOUT_MS)
        .protocol(RedisProtocol.RESP3).build();
  }

  /**
   * Default maintenance config: AUTO mode, default backstop window, relaxed timeouts configured.
   */
  protected MaintenanceNotificationsConfig defaultMaintConfig() {
    return MaintenanceNotificationsConfig.builder().relaxedSocketTimeoutMillis(RELAXED_TIMEOUT_MS)
        .relaxedBlockingSocketTimeoutMillis(RELAXED_BLOCKING_TIMEOUT_MS).build();
  }

  // ---- Tests ---------------------------------------------------------------

  @Test
  public void testMigratingPushMessage() throws SocketException {
    Socket socket = ConnectionTestHelper.getSocket(connection);

    assertEquals(SO_TIMEOUT_MS, connection.getSoTimeout());
    assertEquals(RELAXED_TIMEOUT_MS, getRelaxedSoTimeout(connection));

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.migrating(1, 10, Collections.singletonList("1")));
    assertTrue(connection.ping());
    assertTrue(isRelaxedTimeoutActive(connection));
    assertEquals(RELAXED_TIMEOUT_MS, socket.getSoTimeout());

    mockServer
        .sendPushMessageToAll(MaintenanceEventMessages.migrated(1, Collections.singletonList("1")));
    assertTrue(connection.ping());
    assertFalse(isRelaxedTimeoutActive(connection));
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
  }

  @Test
  public void testFailoverPushMessage() throws SocketException {
    Socket socket = ConnectionTestHelper.getSocket(connection);

    assertTrue(connection.isConnected());
    assertEquals(SO_TIMEOUT_MS, connection.getSoTimeout());
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.failingOver(1, 10, Collections.singletonList("1")));
    assertTrue(connection.ping());
    assertTrue(isRelaxedTimeoutActive(connection));
    assertEquals(RELAXED_TIMEOUT_MS, socket.getSoTimeout());

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.failedOver(1, Collections.singletonList("1")));
    assertTrue(connection.ping());
    assertFalse(isRelaxedTimeoutActive(connection));
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
  }

  /** Relaxed timeout unset (UNSET_TIMEOUT) falls back to the baseline socket timeout. */
  @Test
  public void testRelaxTimeoutsDisabledFallbackToSoTimeout() throws SocketException {
    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .relaxedSocketTimeoutMillis(UNSET_TIMEOUT_MS).build();
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(SO_TIMEOUT_MS).protocol(RedisProtocol.RESP3).build();

    try (Connection conn = buildDirect(new HostAndPort("localhost", mockServer.getPort()),
      clientConfig, maintConfig)) {
      Socket socket = ConnectionTestHelper.getSocket(conn);
      assertTrue(conn.isConnected());
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
      assertEquals(UNSET_TIMEOUT_MS, getRelaxedSoTimeout(conn));

      relaxTimeouts(conn, Duration.ofSeconds(10));

      assertTrue(isRelaxedTimeoutActive(conn));
      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
    }
  }

  /** Default TimeoutOptions: blocking timeout 0, relaxed timeout DEFAULT_RELAXED_TIMEOUT. */
  @Test
  public void testDefaultTimeoutOptions() throws SocketException {
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(SO_TIMEOUT_MS).protocol(RedisProtocol.RESP3).build();

    try (Connection conn = buildDirect(new HostAndPort("localhost", mockServer.getPort()),
      clientConfig, MaintenanceNotificationsConfig.builder()
          .mode(MaintenanceNotificationsConfig.Mode.AUTO).build())) {
      Socket socket = ConnectionTestHelper.getSocket(conn);
      assertTrue(conn.isConnected());

      assertEquals(SO_TIMEOUT_MS, conn.getSoTimeout());
      assertEquals(0, conn.getBlockingSoTimeout());
      assertEquals(RELAXED_TIMEOUT_MS, getRelaxedSoTimeout(conn));
      assertEquals(UNSET_TIMEOUT_MS, getRelaxedBlockingSoTimeout(conn));
      assertFalse(isRelaxedTimeoutActive(conn));

      assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
    }
  }

  /**
   * MIGRATING during a blocking command applies the relaxed-blocking timeout; once the blocking
   * read returns, the timeout reverts to the relaxed non-blocking value until MIGRATED arrives.
   */
  @Test
  public void testRelaxedBlockingTimeoutAppliedDuringBlockingCommand()
      throws InterruptedException, SocketException {
    Socket socket = ConnectionTestHelper.getSocket(connection);
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());

    // Three latches:
    // - blpopSent: counts down when mockHandler sees BLPOP (so we know BLPOP is in flight).
    // - blpopRelease: held by main thread; the mock answer waits on it before returning the
    // reply, so the worker stays parked in the blocking read while we assert.
    // - blpopFinished: counts down when executeCommand returns on the worker thread.
    CountDownLatch blpopSent = new CountDownLatch(1);
    CountDownLatch blpopRelease = new CountDownLatch(1);
    CountDownLatch blpopFinished = new CountDownLatch(1);
    doAnswer(invocation -> {
      blpopSent.countDown();
      blpopRelease.await();
      return RespResponse.arrayOfBulkStrings("popped-item");
    }).when(mockHandler).handleCommand(argThat(args -> {
      String cmd = SafeEncoder.encode(args.getCommand().getRaw());
      return "BLPOP".equalsIgnoreCase(cmd);
    }), anyString());

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.migrating(1, 10, Collections.singletonList("1")));

    Thread t1 = new Thread(() -> {
      connection.executeCommand(commandObjects.blpop(5, "test:blpop:key"));
      blpopFinished.countDown();
    });
    t1.start();

    blpopSent.await();
    // BLPOP is now parked on the mock side; worker is reading. The MIGRATING push frame is
    // ahead of the (not-yet-sent) reply in the client buffer, so the worker processes it.
    await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(5))
        .until(() -> isRelaxedTimeoutActive(connection));
    assertEquals(RELAXED_BLOCKING_TIMEOUT_MS, socket.getSoTimeout(),
      "Socket timeout should be relaxed blocking timeout during blocking command");

    // Let the mock return the BLPOP reply; worker can now complete.
    blpopRelease.countDown();
    blpopFinished.await();
    assertTrue(isRelaxedTimeoutActive(connection),
      "Relaxed timeout should still be active after blocking command");
    assertEquals(RELAXED_TIMEOUT_MS, socket.getSoTimeout(),
      "Socket timeout should be restored to relaxed (non-blocking) value");

    mockServer
        .sendPushMessageToAll(MaintenanceEventMessages.migrated(1, Collections.singletonList("1")));
    connection.executeCommand(commandObjects.ping());

    assertFalse(isRelaxedTimeoutActive(connection),
      "Relaxed timeout should be disabled after MIGRATED");
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout(),
      "Socket timeout should be restored to baseline");
  }

  /**
   * Without a matching MIGRATED, the per-connection window reverts at the max-duration backstop.
   */
  @Test
  public void testMigratingWithoutMigratedRevertsAtMaxDuration() throws SocketException {
    Socket socket = ConnectionTestHelper.getSocket(connection);
    AtomicLong clock = new AtomicLong(0);
    ConnectionTestHelper.setClockNanos(pool, connection, clock::get);

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.migrating(1, 10, Collections.singletonList("1")));
    assertTrue(connection.ping());
    assertTrue(isRelaxedTimeoutActive(connection));
    assertEquals(RELAXED_TIMEOUT_MS, socket.getSoTimeout());

    // No MIGRATED arrives; advance past the 60s safety cap.
    clock.set(Duration.ofSeconds(61).toNanos());
    assertTrue(connection.ping());
    assertFalse(isRelaxedTimeoutActive(connection),
      "Relaxed timeout should revert at maxRelaxedDuration without a terminator");
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
  }

  /**
   * MOVING relaxes the receiving connection for the server-supplied grace window; the next read
   * after the window expires reverts to the baseline timeout.
   */
  @Test
  public void testMovingRelaxesReceiverForGraceWindowThenReverts() throws SocketException {
    Socket socket = ConnectionTestHelper.getSocket(connection);
    AtomicLong clock = new AtomicLong(0);
    // MOVING also drives the pool-wide rebind state via the controller; sync its clock too so
    // executeCommand's read-time apply sees the same time progression.
    ConnectionTestHelper.setClockNanos(pool, connection, clock::get);

    mockServer.sendPushMessageToAll(
      MaintenanceEventMessages.moving(1, 15, "localhost:" + mockServer.getPort()));
    assertTrue(connection.ping());
    assertTrue(isRelaxedTimeoutActive(connection), "MOVING relaxes the receiving connection");
    assertEquals(RELAXED_TIMEOUT_MS, socket.getSoTimeout());

    clock.set(Duration.ofSeconds(16).toNanos());
    assertTrue(connection.ping());
    assertFalse(isRelaxedTimeoutActive(connection));
    assertEquals(SO_TIMEOUT_MS, socket.getSoTimeout());
  }
}
