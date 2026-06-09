package redis.clients.jedis;

import java.net.Socket;
import java.util.List;

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
   * Returns {@code true} if the consumer is a {@link Connection.MaintenanceEventConsumer}.
   * <p>
   * The maintenance consumer captures its owning connection, so it cannot be a shared singleton and
   * must be matched by type rather than identity.
   * </p>
   */
  public static boolean isMaintenanceEventConsumer(PushConsumer consumer) {
    return consumer instanceof Connection.MaintenanceEventConsumer;
  }

  /**
   * Returns the underlying {@link Socket} of a Connection so tests can assert OS-level state (e.g.
   * the applied {@code SO_TIMEOUT}). The field is private, so reflection is centralized here rather
   * than repeated in test bodies.
   */
  public static Socket getSocket(Connection connection) {
    return ReflectionTestUtil.getField(connection, "socket");
  }

  private ConnectionTestHelper() {
    // Utility class - prevent instantiation
  }
}
