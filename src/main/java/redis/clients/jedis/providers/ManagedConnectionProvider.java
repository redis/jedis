package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public class ManagedConnectionProvider implements ConnectionProvider {

  private Connection connection;

  public final void setConnection(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void close() {
  }

  @Override
  public final Connection getConnection() {
    return connection;
  }

  @Override
  public final Connection getConnection(CommandArguments args) {
    return connection;
  }
}
