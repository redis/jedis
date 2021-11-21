package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  public ConnectionPool(GenericObjectPoolConfig<Connection> poolConfig,
      HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(poolConfig, new ConnectionFactory(hostAndPort, clientConfig));
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(new GenericObjectPoolConfig<Connection>(), factory);
  }

  public ConnectionPool(GenericObjectPoolConfig<Connection> poolConfig, PooledObjectFactory<Connection> factory) {
    super(poolConfig, factory);
  }

  @Override
  public Connection getResource() {
    Connection resource = super.getResource();
    resource.setHandlingPool(this);
    return resource;
  }
}
