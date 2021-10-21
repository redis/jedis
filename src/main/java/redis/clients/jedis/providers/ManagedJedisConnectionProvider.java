package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public class ManagedJedisConnectionProvider implements JedisConnectionProvider {

  private Connection connection;

  public final void setConnection(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void close() {
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return connection;
  }
}
