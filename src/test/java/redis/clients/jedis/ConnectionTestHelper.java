package redis.clients.jedis;

import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;

import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Test helper for accessing package-private/protected members of Connection.
 * <p>
 * This class is in the same package as Connection to avoid using reflection in tests.
 * </p>
 */
public class ConnectionTestHelper {

  /**
   * Gets the list of push consumers from a Connection.
   * <p>
   * This method provides test access to the protected getPushConsumers() method.
   * </p>
   * @param connection the connection to get consumers from
   * @return the list of push consumers
   */
  public static List<PushConsumer> getPushConsumers(Connection connection) {
    return connection.getPushConsumers();
  }

  /**
   * Sets the active-subscription flag of a Connection, simulating a pub/sub read loop driving the
   * connection.
   * @param connection the connection to modify
   * @param active whether a pub/sub read loop is active
   */
  public static void setActiveSubscription(Connection connection, boolean active) {
    connection.setActiveSubscription(active);
  }

  /**
   * Wires the production maintenance handshake onto a builder — same wiring as
   * {@link ConnectionFactory}: maintenance config plus a {@link MaintenanceAwareVisitor} backed by
   * a fresh {@link MaintenanceEventController}.
   */
  public static Connection.Builder withMaintenanceHandshake(Connection.Builder builder,
      MaintenanceNotificationsConfig maintConfig) {
    return builder.maintenanceConfig(maintConfig).addVisitor(
      new MaintenanceAwareVisitor(builder, MaintenanceEventController.from(maintConfig)));
  }

  /**
   * Returns {@code true} if the consumer is a {@link MaintenanceEventConsumer}.
   * <p>
   * The maintenance consumer captures its owning connection, so it cannot be a shared singleton and
   * must be matched by type rather than identity.
   * </p>
   */
  public static boolean isMaintenanceEventConsumer(PushConsumer consumer) {
    return consumer instanceof MaintenanceEventConsumer;
  }

  /**
   * Returns {@code true} if the consumer is a {@link PubSubPushConsumer}.
   * <p>
   * The pub/sub consumer is gated on its owning connection's subscription state, so it cannot be a
   * shared singleton and must be matched by type rather than identity.
   * </p>
   */
  public static boolean isPubSubPushConsumer(PushConsumer consumer) {
    return consumer instanceof PubSubPushConsumer;
  }

  /**
   * Maintenance relaxed-timeout state on {@link Connection}. Relaxation is wired only when the
   * maintenance feature is active (a {@link MaintenanceEventController} attached via the pool), and
   * is layered as overrides on the connection's timeout source chain — the pool-wide rebind window
   * ({@code RebindTimeoutSource}) and the per-connection MOVING window
   * ({@code ExpiringTimeoutSource}). The timeout is relaxed whenever any override currently has an
   * opinion, so this observes the chain rather than a single source.
   */
  public static boolean isRelaxedTimeoutActive(Connection connection) {
    return connection.getTimeoutSource().getOverrideInfo() != null;
  }

  public static void relaxTimeouts(Connection connection, Duration period) {
    ChainedTimeoutSource dts = connection.getTimeoutSource();
    ExpiringTimeoutSource ets = ((ExpiringTimeoutSource) dts.seekBy(ExpiringTimeoutSource.class));
    ets.setExpirationTime(NanoClock.INSTANCE.getAsLong() + period.toNanos());
  }

  public static void resetRelaxedTimeouts(Connection connection) {
    ChainedTimeoutSource dts = connection.getTimeoutSource();
    ExpiringTimeoutSource ets = ((ExpiringTimeoutSource) dts.seekBy(ExpiringTimeoutSource.class));
    ets.setExpirationTime(0);
  }

  public static int getRelaxedSoTimeout(Connection connection) {
    return relaxedInfo(connection).timeout;
  }

  public static int getRelaxedBlockingSoTimeout(Connection connection) {
    return relaxedInfo(connection).blockingTimeout;
  }

  /** The configured relaxed timeouts, or {@code null} when relaxation was never wired. */
  private static TimeoutSource.TimeoutInfo relaxedInfo(Connection connection) {
    ExpiringTimeoutSource source = (ExpiringTimeoutSource) connection.getTimeoutSource()
        .seekBy(ExpiringTimeoutSource.class);
    return source == null ? null : source.get();
  }

  /**
   * Returns the underlying {@link Socket} of a Connection so tests can assert OS-level state (e.g.
   * the applied {@code SO_TIMEOUT}). The field is private, so reflection is centralized here rather
   * than repeated in test bodies.
   */
  public static Socket getSocket(Connection connection) {
    return ReflectionTestUtil.getField(connection, "socket");
  }

  /**
   * Chains a test hook after the pool's own handoff reaction, so tests can await the marking pass
   * instead of polling.
   */
  public static void addHandoffHook(ConnectionPool pool, Runnable hook) {
    MaintenanceEventController controller = (MaintenanceEventController) pool
        .getMaintenanceController();
    Runnable poolReaction = controller.getHandoffHook();
    controller.setHandoffHook(() -> {
      poolReaction.run(); // evict first: the pass is fully processed before the test observes it
      hook.run();
    });
  }

  /**
   * The pool controller's post-DNS remap for {@code resolved}: the active MOVING target for that
   * peer, or null (no active event, or a 'none' event). New pool connections only ever resolve the
   * configured endpoint, so tests assert other peers' mapping windows through this seam.
   */
  public static SocketAddress getMappedAddress(ConnectionPool pool, SocketAddress resolved) {
    return ((MaintenanceEventController) pool.getMaintenanceController())
        .getSocketAddress(resolved);
  }

  public static void setClockNanos(LongSupplier clock) {
    NanoClock.INSTANCE = clock;
  }

  /** Restores the process-wide monotonic clock to {@link System#nanoTime()}. */
  public static void resetClockNanos() {
    NanoClock.INSTANCE = System::nanoTime;
  }

  public static int getBlockingSoTimeout(Connection connection) {
    return connection.getBlockingSoTimeout();
  }

  // ---- Pool-level maintenance wiring (standalone and cluster families) ----------------------

  /** Whether the pool runs with a maintenance controller at all (any family). */
  public static boolean hasMaintenanceController(ConnectionPool pool) {
    return pool.getMaintenanceController() != null;
  }

  /**
   * Whether the pool's controller is the cluster family's ({@link ClusterMaintenanceController}).
   */
  public static boolean hasClusterMaintenanceController(ConnectionPool pool) {
    return pool.getMaintenanceController() instanceof ClusterMaintenanceController;
  }

  /** Whether the pool's controller is registered as a maintenance listener on the connection. */
  public static boolean isListenerOfPoolController(ConnectionPool pool, Connection connection) {
    MaintenanceController controller = pool.getMaintenanceController();
    return controller != null && connection.getMaintenanceEventListeners().contains(controller);
  }

  /**
   * Whether the pool controller's relax overlay (the pool-wide / client-wide gate, keyed by the
   * controller that installed it) is on the connection's timeout chain.
   */
  public static boolean hasPoolRelaxOverlay(ConnectionPool pool, Connection connection) {
    MaintenanceController controller = pool.getMaintenanceController();
    return controller != null && connection.getTimeoutSource().seekBy(controller) != null;
  }

  /** Whether the per-connection expiring relax overlay (standalone family only) is installed. */
  public static boolean hasExpiringRelaxOverlay(Connection connection) {
    return connection.getTimeoutSource().seekBy(ExpiringTimeoutSource.class) != null;
  }

  /** Whether a {@link MaintenanceEventConsumer} is on the connection's push consumer chain. */
  public static boolean hasMaintenanceEventConsumer(Connection connection) {
    for (PushConsumer consumer : connection.getPushConsumers()) {
      if (isMaintenanceEventConsumer(consumer)) {
        return true;
      }
    }
    return false;
  }

  /** Whether the connection's retirement deadline has passed (see {@code Connection#retireAt}). */
  public static boolean isRetired(Connection connection) {
    return connection.isRetired();
  }

  private ConnectionTestHelper() {
    // Utility class - prevent instantiation
  }
}
