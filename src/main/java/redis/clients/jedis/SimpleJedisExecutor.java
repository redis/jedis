package redis.clients.jedis;

import redis.clients.jedis.providers.JedisConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class SimpleJedisExecutor implements JedisCommandExecutor {

  private final JedisConnectionProvider provider;

  public SimpleJedisExecutor(JedisConnectionProvider provider) {
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
