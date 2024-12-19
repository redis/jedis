package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.util.Pool;
import today.bonfire.oss.sop.PooledObjectFactory;

public class ConnectionPool extends Pool<Connection> {

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig, Cache clientSideCache) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache));
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      JedisPoolConfig poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig), poolConfig);
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig, Cache clientSideCache,
      JedisPoolConfig poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache), poolConfig);
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory,
      JedisPoolConfig poolConfig) {
    super(factory, poolConfig);
  }

  @Override
  public Connection getResource() {
    Connection conn = super.getResource();
    conn.setHandlingPool(this);
    return conn;
  }

  public PooledObjectFactory<Connection> getFactory() {
    return super.getFactory();
  }
}
