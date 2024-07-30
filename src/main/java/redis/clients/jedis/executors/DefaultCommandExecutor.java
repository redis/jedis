package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.providers.ConnectionProvider;

public class DefaultCommandExecutor implements CommandExecutor {

  protected final ConnectionProvider provider;

  private final ClientSideCache cache;

  public DefaultCommandExecutor(ConnectionProvider provider) {
    this(provider, (ClientSideCache) null);
  }

  public DefaultCommandExecutor(ConnectionProvider provider, ClientSideCache cache) {
    this.provider = provider;
    this.cache = cache;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    try (Connection connection = provider.getConnection(commandObject.getArguments())) {
      if (cache != null) {
        return cache.get(connection, commandObject);
      } else {
        return connection.executeCommand(commandObject);
      }
    }
  }
}
