package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.util.Pool;

public class PooledJedisConnectionProvider<C extends Connection> implements JedisConnectionProvider {

  private final Pool<C> pool;

  public PooledJedisConnectionProvider(Pool<C> pool) {
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
