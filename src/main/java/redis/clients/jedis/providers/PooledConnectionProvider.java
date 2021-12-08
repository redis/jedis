package redis.clients.jedis.providers;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.util.Pool;

public class PooledConnectionProvider implements ConnectionProvider {

  private final Pool<Connection> pool;

  public PooledConnectionProvider(HostAndPort hostAndPort) {
    this(new ConnectionFactory(hostAndPort));
  }

  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
  }

  public PooledConnectionProvider(PooledObjectFactory<Connection> factory) {
    this(factory, new GenericObjectPoolConfig<>());
  }

  public PooledConnectionProvider(PooledObjectFactory<Connection> factory, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionPool(factory, poolConfig));
  }

  private PooledConnectionProvider(Pool<Connection> pool) {
    this.pool = pool;
  }

  @Override
  public void close() {
    pool.close();
  }

  public final Pool<Connection> getPool() {
    return pool;
  }

  @Override
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }
}
