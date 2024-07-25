package redis.clients.jedis.executors;

import java.util.function.Supplier;
import redis.clients.jedis.CommandObject;

public interface CommandExecutor extends AutoCloseable {

  <T> T executeCommand(CommandObject<T> commandObject);

  /**
   * To support client-side caching.
   *
   * This must be implemented to get support of client-side caching.
   */
  default <T> T executeCommand(CommandObject<T> commandObject, Supplier<Object[]> keys) {
    return executeCommand(commandObject); // without client-side caching
  }

  default <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject);
  }
}
