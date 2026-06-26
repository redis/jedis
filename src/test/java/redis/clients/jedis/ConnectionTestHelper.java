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
   * Maintenance relaxed-timeout state machine on {@link Connection}. These methods are package-
   * private (driven internally by the SCH layer); tests in other packages reach them here.
   */
  public static boolean isRelaxedTimeoutActive(Connection connection) {
    return connection.isRelaxedTimeoutActive();
  }

  public static void relaxTimeouts(Connection connection, Duration period) {
    connection.relaxTimeouts(period);
  }

  public static void resetRelaxedTimeouts(Connection connection) {
    connection.resetRelaxedTimeouts();
  }

  public static int getRelaxedSoTimeout(Connection connection) {
    return connection.getRelaxedSoTimeout();
  }

  public static int getRelaxedBlockingSoTimeout(Connection connection) {
    return connection.getRelaxedBlockingSoTimeout();
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
   * Overrides the monotonic clock on the connection and (if attached) its pool's
   * {@link MaintenanceEventController}, so tests can fast-forward time deterministically across
   * both the per-connection deadlines and any pool-wide state that consults the clock.
   */
  public static void setClockNanos(ConnectionPool pool, Connection conn, LongSupplier clock) {
    conn.setClockNanos(clock);
    MaintenanceEventController ctrl = pool.getMaintenanceController();
    if (ctrl != null) ctrl.setClockNanos(clock);
  }

  private ConnectionTestHelper() {
    // Utility class - prevent instantiation
  }
}
