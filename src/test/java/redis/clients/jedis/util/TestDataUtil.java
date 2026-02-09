package redis.clients.jedis.util;

/**
 * Utility class for generating test data.
 */
public class TestDataUtil {

  private TestDataUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Generates a string of a specific size filled with a repeated character.
   * @param size The desired size in characters
   * @param fillChar The character to fill the string with
   * @return A string of the specified size
   */
  public static String generateString(int size, char fillChar) {
    StringBuilder value = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
      value.append(fillChar);
    }
    return value.toString();
  }

  /**
   * Generates a string of a specific size filled with 'x' characters.
   * @param size The desired size in characters
   * @return A string of the specified size filled with 'x'
   */
  public static String generateString(int size) {
    return generateString(size, 'x');
  }
}
