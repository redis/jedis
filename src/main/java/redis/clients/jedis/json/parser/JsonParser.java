package redis.clients.jedis.json.parser;

/**
 * Represents the ability of serialize an object to JSON format string
 * and deserialize it to the typed object.
 *
 * @see DefaultGsonParser Default implementation for JSON parser engine with com.google.gson.Gson
 */
public interface JsonParser {
  /**
   * Perform deserialization from JSON format string to the given type object as argument.
   * @param value the JSON format
   * @param valueType the object type to convert
   * @return the instance of an object to the type given argument
   * @param <T> the type object to convert
   */
  <T> T fromJson(String value, Class<T> valueType);

  /**
   * Perform serialization from object to JSON format string.
   *
   * @param value the object to convert
   * @return the JSON format string
   */
  String toJson(Object value);
}
