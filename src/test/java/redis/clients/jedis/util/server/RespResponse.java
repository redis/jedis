package redis.clients.jedis.util.server;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for building RESP (Redis Serialization Protocol) responses. This makes it easier to
 * construct proper Redis protocol responses for testing.
 */
public class RespResponse {

  /**
   * Create a simple string response (+OK, +PONG, etc.).
   * @param value The string value
   * @return A RESP simple string response
   */
  public static String simpleString(String value) {
    return "+" + value + "\r\n";
  }

  /**
   * Create an error response (-ERR message).
   * @param message The error message
   * @return A RESP error response
   */
  public static String error(String message) {
    return "-" + message + "\r\n";
  }

  /**
   * Create an integer response (:123).
   * @param value The integer value
   * @return A RESP integer response
   */
  public static String integer(long value) {
    return ":" + value + "\r\n";
  }

  /**
   * Create a bulk string response ($5\r\nhello\r\n).
   * @param value The string value (null for null bulk string)
   * @return A RESP bulk string response
   */
  public static String bulkString(String value) {
    if (value == null) {
      return "$-1\r\n";
    }
    byte[] bytes = value.getBytes();
    return "$" + bytes.length + "\r\n" + value + "\r\n";
  }

  /**
   * Create a bulk string response from byte array.
   * @param bytes The byte array (null for null bulk string)
   * @return A RESP bulk string response
   */
  public static String bulkString(byte[] bytes) {
    if (bytes == null) {
      return "$-1\r\n";
    }
    return "$" + bytes.length + "\r\n" + new String(bytes) + "\r\n";
  }

  /**
   * Create an array response. Supports mixed data types. Not all RESP data types are supported.
   * Only the following are supported: Each element is automatically encoded based on its type:
   * <ul>
   * <li>Integer → RESP integer (:123\r\n)</li>
   * <li>Long → RESP integer (:123\r\n)</li>
   * <li>String → RESP bulk string ($3\r\nabc\r\n)</li>
   * <li>Other → RESP bulk string via toString()</li>
   * </ul>
   * Example: array(Arrays.asList("MIGRATING", 6, 2, "[\"2\", \"4\"]"))
   * @param elements List of array elements (can be Integer, Long, String, etc.)
   * @return A RESP array response (*4\r\n...)
   */
  public static String array(List<Object> elements) {
    if (elements == null) {
      return nullArray();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("*").append(elements.size()).append("\r\n");

    for (Object element : elements) {
      sb.append(encodeElement(element));
    }

    return sb.toString();
  }

  /**
   * Create a RESP3 push message. Push messages are identical to arrays but use '>' instead of '*'
   * as the first byte. Example: push(Arrays.asList("MIGRATING", 6, 2, "[\"2\", \"4\"]"))
   * @see #array(List) for element encoding details
   * @param elements List of push message elements (can be Integer, Long, String, etc.)
   * @return A RESP3 push message (>4\r\n...)
   */
  public static String push(List<Object> elements) {

    if (elements == null || elements.isEmpty()) {
      throw new IllegalArgumentException("Push message elements cannot be null or empty");
    }

    StringBuilder sb = new StringBuilder();
    sb.append(">").append(elements.size()).append("\r\n");

    for (Object element : elements) {
      sb.append(encodeElement(element));
    }

    return sb.toString();
  }

  /**
   * Encode a single element based on its Java type.
   * @param element The element to encode
   * @return RESP-encoded string
   */
  private static String encodeElement(Object element) {
    if (element instanceof Integer) {
      return integer((Integer) element);
    } else if (element instanceof Long) {
      return integer((Long) element);
    } else {
      return bulkString(element.toString());
    }
  }

  /**
   * Create an array response from a list of strings as bulk strings.
   * @param strings The list of strings
   * @return A RESP array response with bulk string elements
   */
  public static String arrayOfBulkStrings(List<String> strings) {
    if (strings == null) {
      return "*-1\r\n";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("*").append(strings.size()).append("\r\n");

    for (String str : strings) {
      sb.append(bulkString(str));
    }

    return sb.toString();
  }

  /**
   * Create an array response from string values (automatically converts to bulk strings).
   * @param values The string values
   * @return A RESP array response with bulk string elements
   */
  public static String arrayOfBulkStrings(String... values) {
    if (values == null) {
      return "*-1\r\n";
    }
    return arrayOfBulkStrings(Arrays.asList(values));
  }

  /**
   * Create an empty array response (*0\r\n).
   * @return A RESP empty array response
   */
  public static String emptyArray() {
    return "*0\r\n";
  }

  /**
   * Create a null array response (*-1\r\n).
   * @return A RESP null array response
   */
  public static String nullArray() {
    return "*-1\r\n";
  }

  /**
   * Create a null bulk string response ($-1\r\n).
   * @return A RESP null bulk string response
   */
  public static String nullBulkString() {
    return "$-1\r\n";
  }

  /**
   * Create an OK response (+OK\r\n).
   * @return A RESP OK response
   */
  public static String ok() {
    return simpleString("OK");
  }

  /**
   * Create a PONG response (+PONG\r\n).
   * @return A RESP PONG response
   */
  public static String pong() {
    return simpleString("PONG");
  }

}
