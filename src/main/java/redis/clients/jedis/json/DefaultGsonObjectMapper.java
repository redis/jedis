package redis.clients.jedis.json;

import com.google.gson.Gson;

/**
 * Use the default {@link Gson} configuration for serialization and deserialization JSON
 * operations.
 * <p>When none is explicitly set, this will be set.</p>
 * @see JsonObjectMapper Create a custom JSON serializer/deserializer
 */
public class DefaultGsonObjectMapper implements JsonObjectMapper {
  /**
   * Instance of Gson object with default gson configuration.
   */
  private final Gson gson = new Gson();

  @Override
  public <T> T fromJson(String value, Class<T> valueType) {
    return gson.fromJson(value, valueType);
  }

  @Override
  public String toJson(Object value) {
    return gson.toJson(value);
  }
}
