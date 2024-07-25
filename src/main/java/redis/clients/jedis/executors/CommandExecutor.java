package redis.clients.jedis.executors;

import java.util.function.Supplier;
import redis.clients.jedis.CommandObject;

public interface CommandExecutor extends AutoCloseable {

  default <T> T executeCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject, null);
  }

  <T> T executeCommand(CommandObject<T> commandObject, Supplier<Object[]> keys);

  default <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject, null);
  }
}
