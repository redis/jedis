package redis.clients.jedis.util;

/**
 * Assertion utility class that assists in validating arguments. This class is part of the internal API and may change without
 * further notice.
 *
 * @author ivo.gaydazhiev
 */
public class JedisAsserts {

  /**
   * Assert that an object is not {@code null} .
   *
   * @param object the object to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object is {@code null}
   */
  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert that {@code value} is {@code true}.
   *
   * @param value the value to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the value is {@code false}
   */
  public static void isTrue(boolean value, String message) {
    if (!value) {
      throw new IllegalArgumentException(message);
    }
  }
}
