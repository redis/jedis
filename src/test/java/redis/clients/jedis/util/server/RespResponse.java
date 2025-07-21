package redis.clients.jedis.util.server;

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
   * Create an array response (*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n).
   * @param elements The array elements as RESP strings
   * @return A RESP array response
   */
  public static String array(String... elements) {
    if (elements == null) {
      return "*-1\r\n";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("*").append(elements.length).append("\r\n");

    for (String element : elements) {
      sb.append(element);
    }

    return sb.toString();
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

    String[] elements = new String[strings.size()];
    for (int i = 0; i < strings.size(); i++) {
      elements[i] = bulkString(strings.get(i));
    }

    return array(elements);
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

    String[] elements = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      elements[i] = bulkString(values[i]);
    }

    return array(elements);
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
