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
    final CommandArguments args = commandObject.getArguments();
    try (JedisConnection connection = provider.getConnection(args)) {
      connection.sendCommand(args);
      if (!args.isBlocking()) {
        return commandObject.getBuilder().build(connection.getOne());
      } else {
        try {
          connection.setTimeoutInfinite();
          return commandObject.getBuilder().build(connection.getOne());
        } finally {
          connection.rollbackTimeout();
        }
      }
    }
  }
}
