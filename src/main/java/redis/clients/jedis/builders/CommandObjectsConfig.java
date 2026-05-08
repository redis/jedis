package redis.clients.jedis.builders;

import redis.clients.jedis.CommandKeyArgumentPreProcessor;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.json.JsonObjectMapper;

/**
 * Immutable bag of configuration that the client passes to a {@link CommandObjects} constructor.
 * Holds the knobs that are independent of the negotiated Redis protocol (key preprocessing, JSON
 * object mapping, default search dialect).
 * <p>
 * Each field is nullable: {@code null} means "the user didn't configure this knob, keep the library
 * default". Builders snapshot their fluent configuration into a {@code CommandObjectsConfig} and
 * hand it to the client constructor. The client then constructs its own {@link CommandObjects}
 * (using a subtype-aware factory hook) which reads this configuration during construction.
 */
public final class CommandObjectsConfig {

  private final CommandKeyArgumentPreProcessor keyPreProcessor;
  private final JsonObjectMapper jsonObjectMapper;
  private final Integer searchDialect;

  public CommandObjectsConfig(CommandKeyArgumentPreProcessor keyPreProcessor,
      JsonObjectMapper jsonObjectMapper, Integer searchDialect) {
    this.keyPreProcessor = keyPreProcessor;
    this.jsonObjectMapper = jsonObjectMapper;
    this.searchDialect = searchDialect;
  }

  public CommandKeyArgumentPreProcessor getKeyPreProcessor() {
    return keyPreProcessor;
  }

  public JsonObjectMapper getJsonObjectMapper() {
    return jsonObjectMapper;
  }

  public Integer getSearchDialect() {
    return searchDialect;
  }

  /**
   * @return a config that holds no overrides — equivalent to library defaults.
   */
  public static CommandObjectsConfig empty() {
    return new CommandObjectsConfig(null, null, null);
  }
}
