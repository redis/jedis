package redis.clients.jedis;

public interface JedisCommandExecutor extends AutoCloseable {

  <T> T executeCommand(CommandObject<T> commandObject);
}
