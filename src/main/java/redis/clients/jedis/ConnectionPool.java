package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.csc.ClientSideCache;

public class ConnectionPool extends Pool<Connection> {

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig, ClientSideCache csCache) {
    this(new ConnectionFactory(hostAndPort, clientConfig, csCache));
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig), poolConfig);
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig, ClientSideCache csCache,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig, csCache), poolConfig);
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    super(factory, poolConfig);
  }

  @Override
  public Connection getResource() {
    Connection conn = super.getResource();
    conn.setHandlingPool(this);
    return conn;
  }
}
