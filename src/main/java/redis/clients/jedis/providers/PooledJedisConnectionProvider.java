package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.JedisConnection;
import redis.clients.jedis.util.Pool;
import java.io.IOException;

public class PooledJedisConnectionProvider<C extends JedisConnection> implements JedisConnectionProvider, AutoCloseable {

  private final Pool<C> pool;

  public PooledJedisConnectionProvider(Pool<C> pool) {
    this.pool = pool;
  }

  @Override
  public void close() throws IOException {
    pool.close();
  }

  @Override
  public JedisConnection getConnection(CommandArguments args) {
    return pool.getResource();
  }
}
