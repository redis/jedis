package redis.server.stub;

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
   * Create a bulk string response ($5\r\nhello\r\n). For null values, returns RESP3 null.
   * @param value The string value (null for RESP3 null)
   * @return A RESP bulk string response or RESP3 null
   */
  public static String bulkString(String value) {
    if (value == null) {
      return nullValue();
    }
    byte[] bytes = value.getBytes();
    return "$" + bytes.length + "\r\n" + value + "\r\n";
  }

  /**
   * Create a bulk string response from byte array. For null values, returns RESP3 null.
   * @param bytes The byte array (null for RESP3 null)
   * @return A RESP bulk string response or RESP3 null
   */
  public static String bulkString(byte[] bytes) {
    if (bytes == null) {
      return nullValue();
    }
    return "$" + bytes.length + "\r\n" + new String(bytes) + "\r\n";
  }

  /**
   * Create an array response (*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n). For null, returns RESP3 null.
   * @param elements The array elements as RESP strings (null for RESP3 null)
   * @return A RESP array response or RESP3 null
   */
  public static String array(String... elements) {
    if (elements == null) {
      return nullValue();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("*").append(elements.length).append("\r\n");

    for (String element : elements) {
      sb.append(element);
    }

    return sb.toString();
  }

  /**
   * Create an array response from a list of strings as bulk strings. For null, returns RESP3 null.
   * @param strings The list of strings (null for RESP3 null)
   * @return A RESP array response with bulk string elements or RESP3 null
   */
  public static String arrayOfBulkStrings(List<String> strings) {
    if (strings == null) {
      return nullValue();
    }

    String[] elements = new String[strings.size()];
    for (int i = 0; i < strings.size(); i++) {
      elements[i] = bulkString(strings.get(i));
    }

    return array(elements);
  }

  /**
   * Create an array response from string values (automatically converts to bulk strings). For null,
   * returns RESP3 null.
   * @param values The string values (null for RESP3 null)
   * @return A RESP array response with bulk string elements or RESP3 null
   */
  public static String arrayOfBulkStrings(String... values) {
    if (values == null) {
      return nullValue();
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
   * Create a RESP3 null value (_\r\n). This is the proper RESP3 representation of null.
   * <p>
   * RESP3 introduced the null type to fix the RESP2 duality of null bulk strings ($-1\r\n) and null
   * arrays (*-1\r\n). This is the unified null representation.
   * @return A RESP3 null response
   */
  public static String nullValue() {
    return "_\r\n";
  }

  /**
   * Create a null array response (*-1\r\n).
   * <p>
   * <b>RESP2 Compatibility</b>: This method is preserved for backward compatibility. In RESP3, use
   * {@link #nullValue()} instead. This method now delegates to nullValue() to return the proper
   * RESP3 null.
   * @return A RESP3 null response (delegates to nullValue())
   * @deprecated Use {@link #nullValue()} for RESP3. This RESP2 null array format is deprecated.
   */
  @Deprecated
  public static String nullArray() {
    return nullValue();
  }

  /**
   * Create a null bulk string response ($-1\r\n).
   * <p>
   * <b>RESP2 Compatibility</b>: This method is preserved for backward compatibility. In RESP3, use
   * {@link #nullValue()} instead. This method now delegates to nullValue() to return the proper
   * RESP3 null.
   * @return A RESP3 null response (delegates to nullValue())
   * @deprecated Use {@link #nullValue()} for RESP3. This RESP2 null bulk string format is
   *             deprecated.
   */
  public static String nullBulkString() {
    return nullValue();
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

  /**
   * Create a RESP3 push message (>3\r\n$7\r\nMOVING\r\n$1\r\n1\r\n$2\r\n15\r\n...). Push messages
   * are server-initiated notifications sent to clients.
   * <p>
   * Format: {@code >}<count>{@code \r\n}<element1><element2>...
   * <p>
   * All arguments are automatically converted to bulk strings.
   * @param args Push message elements (first is typically the message type)
   * @return A RESP3 push message
   */
  public static String push(String... args) {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("Push message must have at least one element");
    }

    StringBuilder sb = new StringBuilder();
    sb.append(">").append(args.length).append("\r\n");

    for (String arg : args) {
      sb.append(bulkString(arg));
    }

    return sb.toString();
  }

  /**
   * Create a RESP3 map response (%2\r\n+key1\r\n+value1\r\n+key2\r\n+value2\r\n). For null, returns
   * RESP3 null.
   * @param entries Key-value pairs (alternating keys and values, null for RESP3 null)
   * @return A RESP3 map response or RESP3 null
   */
  public static String map(String... entries) {
    if (entries == null) {
      return nullValue();
    }

    if (entries.length % 2 != 0) {
      throw new IllegalArgumentException("Map entries must be key-value pairs (even number)");
    }

    StringBuilder sb = new StringBuilder();
    int pairs = entries.length / 2;
    sb.append("%").append(pairs).append("\r\n");

    for (String entry : entries) {
      sb.append(entry);
    }

    return sb.toString();
  }

  /**
   * Create a RESP3 map response from alternating string keys and values (automatically converted to
   * bulk strings). For null, returns RESP3 null.
   * @param keyValuePairs Alternating keys and values (null for RESP3 null)
   * @return A RESP3 map response with bulk string keys and values or RESP3 null
   */
  public static String mapOfBulkStrings(String... keyValuePairs) {
    if (keyValuePairs == null) {
      return nullValue();
    }

    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Must provide key-value pairs (even number of arguments)");
    }

    String[] entries = new String[keyValuePairs.length];
    for (int i = 0; i < keyValuePairs.length; i++) {
      entries[i] = bulkString(keyValuePairs[i]);
    }

    return map(entries);
  }

}
