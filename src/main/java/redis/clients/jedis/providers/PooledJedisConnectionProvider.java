package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.JedisConnection;
import redis.clients.jedis.util.Pool;

public class PooledJedisConnectionProvider<C extends JedisConnection> implements JedisConnectionProvider {

  private final Pool<C> pool;

  public PooledJedisConnectionProvider(Pool<C> pool) {
    this.pool = pool;
  }

  @Override
  public void close() {
    pool.close();
  }

  @Override
  public JedisConnection getConnection(CommandArguments args) {
    return pool.getResource();
  }
}
