package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.JedisConnection;

public class ManagedJedisConnectionProvider implements JedisConnectionProvider {

  private JedisConnection connection;

  public final void setConnection(JedisConnection connection) {
    this.connection = connection;
  }

  @Override
  public void close() throws Exception {
  }

  @Override
  public JedisConnection getConnection(CommandArguments args) {
    return connection;
  }
}
