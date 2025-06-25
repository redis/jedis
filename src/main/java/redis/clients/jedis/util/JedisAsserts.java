package redis.clients.jedis.util;

import java.time.Duration;

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
}
