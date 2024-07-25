package redis.clients.jedis.executors;

import java.util.function.Supplier;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.ClientSideCacheCommandExecutorHelper;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.providers.ConnectionProvider;

public class DefaultCommandExecutor implements CommandExecutor {

  protected final ConnectionProvider provider;

  private final ClientSideCacheCommandExecutorHelper cache;

  public DefaultCommandExecutor(ConnectionProvider provider) {
    this(provider, (ClientSideCache) null);
  }

  public DefaultCommandExecutor(ConnectionProvider provider, ClientSideCache cache) {
    this.provider = provider;
    this.cache = new ClientSideCacheCommandExecutorHelper(cache);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject, Supplier<Object[]> keys) {
    try (Connection connection = provider.getConnection(commandObject.getArguments())) {
      if (cache != null && keys != null) {
        return cache.get(connection, commandObject, (Object[]) keys.get());
      } else {
        return connection.executeCommand(commandObject);
      }
    }
  }
}
