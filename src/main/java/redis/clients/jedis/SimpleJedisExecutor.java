package redis.clients.jedis;

import redis.clients.jedis.providers.JedisConnectionProvider;

public class SimpleJedisExecutor implements JedisCommandExecutor, AutoCloseable {

  private final JedisConnectionProvider provider;

  public SimpleJedisExecutor(JedisConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void close() throws Exception {
    this.provider.close();
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
