package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Expiry (maintenance reconnect deadline) enforcement in the pool lifecycle: expired connections
 * are still handed out on borrow (an expired deadline is a recycle hint, not a health signal) and
 * destroyed on return; {@link ConnectionFactory#validateObject} reports them invalid so the
 * standard {@code testOnBorrow}/{@code testWhileIdle} knobs opt into stricter cleanup.
 */
@Tag("sch")
public class ConnectionPoolExpiryMockTest {

  private TcpMockServer mockServer;
  private HostAndPort mockAddress;
  private JedisClientConfig clientConfig;

  @BeforeEach
  public void setUp() throws Exception {
    mockServer = new TcpMockServer();
    mockServer.start();
    mockAddress = new HostAndPort("127.0.0.1", mockServer.getPort());
    clientConfig = DefaultJedisClientConfig.builder().socketTimeoutMillis(5000)
        .protocol(RedisProtocol.RESP3).build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    mockServer.stop();
  }

  // --- factory hooks as units ---

  @Test
  public void activateObject_passesEvenWhenExpired() {
    ConnectionFactory factory = new ConnectionFactory(mockAddress, clientConfig);
    Connection conn = new Connection(mockAddress);
    conn.expireAt(NanoClock.INSTANCE.getAsLong()); // deadline == now -> expired on next check
    // Availability decision: expired connections are still usable and must reach borrowed work.
    assertDoesNotThrow(() -> factory.activateObject(pooled(conn)));
  }

  @Test
  public void validateObject_failsForExpiredConnection() throws Exception {
    ConnectionFactory factory = new ConnectionFactory(mockAddress, clientConfig);
    PooledObject<Connection> pooled = factory.makeObject();
    try {
      pooled.getObject().expireAt(NanoClock.INSTANCE.getAsLong());
      assertFalse(factory.validateObject(pooled));
    } finally {
      factory.destroyObject(pooled);
    }
  }

  @Test
  public void validateObject_passesForFutureOrNoDeadline() throws Exception {
    ConnectionFactory factory = new ConnectionFactory(mockAddress, clientConfig);
    PooledObject<Connection> pooled = factory.makeObject();
    try {
      assertTrue(factory.validateObject(pooled)); // EXPIRE_NOT_SET
      pooled.getObject().expireAt(NanoClock.INSTANCE.getAsLong() + TimeUnit.HOURS.toNanos(1));
      assertTrue(factory.validateObject(pooled));
    } finally {
      factory.destroyObject(pooled);
    }
  }

  // --- default borrow path: availability first ---

  @Test
  public void expiredIdleIsServedThenDestroyedOnReturn() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);

    try (ConnectionPool pool = new ConnectionPool(mockAddress, clientConfig, poolConfig)) {
      Connection first = pool.getResource();
      first.close(); // healthy return -> idle
      first.expireAt(NanoClock.INSTANCE.getAsLong()); // expires while idle

      Connection second = pool.getResource();
      assertSame(first, second, "expired idle is still handed out");
      assertTrue(second.ping(), "borrowed work runs on the expired connection");

      second.close(); // return: Connection.close routes expired -> returnBrokenResource
      assertEquals(1, pool.getDestroyedCount(), "expired connection destroyed on return");
      // (invalidateObject may pre-create a replacement idle; no assertion on getNumIdle here)

      Connection third = pool.getResource();
      assertNotSame(second, third);
      assertFalse(third.isExpired());
      third.close();
    }
  }

  // --- opt-in strictness via the standard validation knobs ---

  @Test
  public void testOnBorrow_replacesExpiredIdleWithinOneBorrow() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    poolConfig.setTestOnBorrow(true);

    try (ConnectionPool pool = new ConnectionPool(mockAddress, clientConfig, poolConfig)) {
      Connection first = pool.getResource();
      first.close();
      first.expireAt(NanoClock.INSTANCE.getAsLong());

      Connection second = pool.getResource(); // validate fails -> destroy -> fresh, one call
      assertNotSame(first, second, "testOnBorrow never hands out an expired connection");
      assertFalse(second.isExpired());
      assertEquals(1, pool.getDestroyedCount());
      second.close();
    }
  }

  @Test
  public void evictorReapsExpiredIdleViaValidation() throws Exception {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig(); // testWhileIdle=true default
    poolConfig.setMaxTotal(1);

    try (ConnectionPool pool = new ConnectionPool(mockAddress, clientConfig, poolConfig)) {
      Connection conn = pool.getResource();
      conn.close();
      conn.expireAt(NanoClock.INSTANCE.getAsLong()); // deadline == now -> expired on next check
      assertEquals(1, pool.getNumIdle());

      pool.evict(); // one synchronous pass: testWhileIdle -> validateObject false -> destroy
      assertEquals(1, pool.getDestroyedCount(), "expired idle reaped by idle validation");
      assertEquals(0, pool.getNumIdle());
    }
  }

  // --- baseline envelope: remote drop of an idle connection (no maintenance involved) ---

  @Test
  public void remoteDropOfIdleConnectionSurfacesOnFirstUseByDefault() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig(); // testOnBorrow=false default
    poolConfig.setMaxTotal(1);

    try (ConnectionPool pool = new ConnectionPool(mockAddress, clientConfig, poolConfig)) {
      Connection idle = pool.getResource();
      idle.close(); // healthy return -> idle

      // Remote peer drops the pooled idle; a peer close is invisible locally (isConnected() stays
      // true in CLOSE_WAIT), so only I/O can detect it.
      mockServer.disconnectAllClients();

      Connection borrowed = pool.getResource();
      assertSame(idle, borrowed, "dead idle is handed out without borrow-time validation");
      assertThrows(JedisConnectionException.class, borrowed::ping,
        "first use surfaces the dead connection");

      borrowed.close(); // broken -> destroyed on return
      assertEquals(1, pool.getDestroyedCount());

      Connection fresh = pool.getResource(); // listener still up: pool recovers
      assertTrue(fresh.ping());
      fresh.close();
    }
  }

  /**
   * Real server behavior during an active maintenance window: the MOVING push follows the +OK of
   * CLIENT MAINT_NOTIFICATIONS ON, so every new connection is stamped from its own push stream
   * (consumed by the first read after the handshake) rather than by any factory-level marking.
   */
  private void injectMovingOnNewConnection() {
    String target = "127.0.0.1:" + mockServer.getPort(); // remap-neutral: the mock itself
    String okThenMoving = "+OK\r\n" + RespResponse.push(Arrays.asList("MOVING", 1, 30, target));
    mockServer.setCommandHandler((args, clientId) -> {
      if (args.size() < 2) return null;
      String cmd = SafeEncoder.encode(args.getCommand().getRaw()).toUpperCase();
      String sub = SafeEncoder.encode(args.get(1).getRaw()).toUpperCase();
      return "CLIENT".equals(cmd) && "MAINT_NOTIFICATIONS".equals(sub) ? okThenMoving : null;
    });
  }

  @Test
  public void movingOnNewConnectionIsServedThenRecycledByDefault() {
    injectMovingOnNewConnection();

    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build();
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig(); // testOnBorrow=false default
    poolConfig.setMaxTotal(1);

    try (ConnectionPool pool = new ConnectionPool(
        ConnectionFactory.builder().hostAndPort(mockAddress).clientConfig(clientConfig), poolConfig,
        maintConfig)) {
      // Default settings: no borrow-time validation, so the connection is handed out; its first
      // read consumes the MOVING and stamps it for recycle.
      Connection conn = pool.getResource();
      assertTrue(conn.ping(), "borrowed work runs");
      assertTrue(conn.isExpired(), "the ping consumed the MOVING push and stamped the connection");

      conn.close();
      assertEquals(1, pool.getDestroyedCount(), "stamped connection recycled on first return");

      // The replacement is re-notified with the same-seq MOVING but postdates the reconnect
      // deadline: it is this rebind's own reconnect and must not be stamped (churn guard).
      Connection next = pool.getResource();
      assertTrue(next.ping()); // consumes its own re-notification
      assertFalse(next.isExpired(), "replacement is immune to the same-seq re-notification");
      next.close();
      assertEquals(1, pool.getDestroyedCount(), "recycled once, then immune");
    }
  }

  @Test
  @Timeout(10)
  public void movingOnNewConnectionFailsFastUnderTestOnBorrow() {
    injectMovingOnNewConnection();

    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build();
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    poolConfig.setTestOnBorrow(true);

    try (ConnectionPool pool = new ConnectionPool(
        ConnectionFactory.builder().hostAndPort(mockAddress).clientConfig(clientConfig), poolConfig,
        maintConfig)) {
      // Strict validation must not hand out a connection stamped during its own validation ping.
      // Failed validation of a freshly created object surfaces as the pool's own borrow failure
      // (NoSuchElementException wrapped by Pool.getResource) - it must not spin creating forever.
      assertThrows(JedisException.class, pool::getResource);
      assertTrue(pool.getDestroyedCount() >= 1, "the rejected fresh connection is destroyed");
    }
  }

  private static PooledObject<Connection> pooled(Connection conn) {
    return new DefaultPooledObject<>(conn);
  }
}
