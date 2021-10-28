package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.util.Pool;

public class PooledJedisConnectionProvider implements JedisConnectionProvider {

  private final Pool<Connection> pool;

  public PooledJedisConnectionProvider(Pool<Connection> pool) {
    this.pool = pool;
  }

  @Override
  public void close() {
    pool.close();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }
}
