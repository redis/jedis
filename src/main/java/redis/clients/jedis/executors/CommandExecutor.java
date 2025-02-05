package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.util.JedisBroadcastReplies;

public interface CommandExecutor extends AutoCloseable {

  <T> T executeCommand(CommandObject<T> commandObject);

  default <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject);
  }

  default JedisBroadcastReplies broadcastCommandDifferingReplies(CommandObject commandObject) {
    return JedisBroadcastReplies.singleton(executeCommand(commandObject));
  }
}
