package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.providers.ConnectionProvider;

public class DefaultCommandExecutor implements CommandExecutor {

  protected final ConnectionProvider provider;

  public DefaultCommandExecutor(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    try (Connection connection = provider.getConnection(commandObject.getArguments())) {
      return connection.executeCommand(commandObject);
    }
  }
}
