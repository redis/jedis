package redis.clients.jedis;

import java.util.function.Function;

import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.search.SearchProtocol;

/**
 * Fluent builder for {@link CommandObjects} and its subclasses. The builder is parameterised on the
 * concrete instance type and uses a {@link Function} factory so subclasses such as
 * {@link ClusterCommandObjects} can be constructed through the same fluent API without forcing each
 * subclass to declare its own static {@code builder()} (which would clash with static-method hiding
 * rules for generic return types).
 * <p>
 * The setters and {@link #build()} method are package-private — the builder is an internal seam
 * between the client constructor and {@link CommandObjects}, and external callers configure these
 * knobs through {@link JedisClientConfig} on the client builder. The class itself is public only so
 * that subclasses of {@link UnifiedJedis} in this package can be referenced through the generic
 * return type of {@code applyClientConfig(...)}.
 */
public class CommandObjectsBuilder<T extends CommandObjects> {

  private final Function<RedisProtocol, T> factory;
  private RedisProtocol protocol;
  private CommandKeyArgumentPreProcessor commandKeyArgumentPreProcessor;
  private JsonObjectMapper jsonObjectMapper;
  private Integer searchDialect;

  CommandObjectsBuilder(Function<RedisProtocol, T> factory) {
    this.factory = factory;
  }

  CommandObjectsBuilder<T> protocol(RedisProtocol protocol) {
    this.protocol = protocol;
    return this;
  }

  CommandObjectsBuilder<T> commandKeyArgumentPreProcessor(
      CommandKeyArgumentPreProcessor commandKeyArgumentPreProcessor) {
    this.commandKeyArgumentPreProcessor = commandKeyArgumentPreProcessor;
    return this;
  }

  CommandObjectsBuilder<T> jsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
    this.jsonObjectMapper = jsonObjectMapper;
    return this;
  }

  CommandObjectsBuilder<T> searchDialect(int searchDialect) {
    if (searchDialect == 0) {
      throw new IllegalArgumentException("DIALECT=0 cannot be set.");
    }
    this.searchDialect = searchDialect;
    return this;
  }

  T build() {
    T target = factory.apply(protocol);
    if (commandKeyArgumentPreProcessor != null) {
      target.setKeyArgumentPreProcessor(commandKeyArgumentPreProcessor);
    }
    if (jsonObjectMapper != null) {
      target.setJsonObjectMapper(jsonObjectMapper);
    }
    if (searchDialect != null && searchDialect != SearchProtocol.DEFAULT_DIALECT) {
      target.setDefaultSearchDialect(searchDialect);
    }
    return target;
  }
}
