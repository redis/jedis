package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;

public interface CommandExecutor extends AutoCloseable {

  <T> T executeCommand(CommandObject<T> commandObject);

  default <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject);
  }

  default <T> T executeKeylessCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject);
  }
}
