package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;

public interface CommandExecutor extends AutoCloseable {

  <T> T executeCommand(CommandObject<T> commandObject);
}
