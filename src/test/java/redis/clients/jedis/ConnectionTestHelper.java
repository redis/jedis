package redis.clients.jedis;

import java.net.Socket;
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
   * Registers a handoff hook on the pool's maintenance controller (fires once a MOVING handoff has
   * been processed — affected connections marked and the pool's evict pass run). Lets tests in
   * other packages await the marking pass deterministically instead of polling.
   */
  public static void addHandoffHook(ConnectionPool pool, Runnable hook) {
    pool.getMaintenanceController().addHandoffHook(hook);
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

  private ConnectionTestHelper() {
    // Utility class - prevent instantiation
  }
}
