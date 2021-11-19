package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.util.Pool;

public class JedisConnectionPool extends Pool<Connection> {

  public JedisConnectionPool(GenericObjectPoolConfig<Connection> poolConfig,
      HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(poolConfig, new JedisConnectionFactory(hostAndPort, clientConfig));
  }

  public JedisConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new JedisConnectionFactory(hostAndPort, clientConfig));
  }

  public JedisConnectionPool(PooledObjectFactory<Connection> factory) {
    super(new GenericObjectPoolConfig<Connection>(), factory);
  }

  public JedisConnectionPool(GenericObjectPoolConfig<Connection> poolConfig, PooledObjectFactory<Connection> factory) {
    super(poolConfig, factory);
  }
}
