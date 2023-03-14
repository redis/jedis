package redis.clients.jedis.json.parser;

import com.google.gson.Gson;

/**
 * Use the default {@link Gson} configuration for serialization and deserialization JSON
 * operations.
 * <p>Needs to explicit the <strong>gson</strong> maven dependency in the <em>pom.xml</em> file to
 * make the use.</p>
 * @see JsonParser Create a custom JSON parser
 */
public class DefaultGsonParser implements JsonParser {
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
