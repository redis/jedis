package redis.clients.jedis.json;

/**
 * Represents the ability of serialize an object to JSON format string and deserialize it to the
 * typed object.
 * @see DefaultGsonObjectMapper Default implementation for <em>JSON serializer/deserializer</em>
 *     engine with com.google.gson.Gson
 */
public interface JsonObjectMapper {
  /**
   * Perform deserialization from JSON format string to the given type object as argument.
   * @param value     the JSON format
   * @param valueType the object type to convert
   * @param <T>       the type object to convert
   * @return the instance of an object to the type given argument
   */
  <T> T fromJson(String value, Class<T> valueType);

  /**
   * Perform serialization from object to JSON format string.
   * @param value the object to convert
   * @return the JSON format string
   */
  String toJson(Object value);
}
