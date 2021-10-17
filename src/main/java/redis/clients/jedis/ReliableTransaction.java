package redis.clients.jedis;

import redis.clients.jedis.commands.PipelineCommands;

public class ReliableTransaction extends ReliableTransactionBase implements PipelineCommands {

  private final RedisCommandObjects commandObjects;

  public ReliableTransaction(Connection connection) {
    super(connection);
    this.commandObjects = new RedisCommandObjects();
  }

  @Override
  public Response<Long> del(String key) {
    return appendCommand(commandObjects.del(key));
  }

  @Override
  public Response<String> get(String key) {
    return appendCommand(commandObjects.get(key));
  }

  @Override
  public Response<String> set(String key, String value) {
    return appendCommand(commandObjects.set(key, value));
  }

}
