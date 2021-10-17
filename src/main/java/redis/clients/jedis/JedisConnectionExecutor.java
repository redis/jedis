package redis.clients.jedis;

import redis.clients.jedis.util.IOUtils;

public class JedisConnectionExecutor implements JedisCommandExecutor {

  private final Connection connection;

  public JedisConnectionExecutor(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(connection);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    return connection.executeCommand(commandObject);
  }
}
