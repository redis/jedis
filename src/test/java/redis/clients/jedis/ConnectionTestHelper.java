package redis.clients.jedis;

import java.util.List;

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

  private ConnectionTestHelper() {
    // Utility class - prevent instantiation
  }
}
