package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.util.Pool;

public class JedisConnectionPool extends Pool<JedisConnection> {

  public JedisConnectionPool(GenericObjectPoolConfig<JedisConnection> poolConfig,
      HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(poolConfig, new JedisConnectionFactory(hostAndPort, clientConfig));
  }

  public JedisConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new JedisConnectionFactory(hostAndPort, clientConfig));
  }

  public JedisConnectionPool(PooledObjectFactory<JedisConnection> factory) {
    super(new GenericObjectPoolConfig<JedisConnection>(), factory);
  }

  public JedisConnectionPool(GenericObjectPoolConfig<JedisConnection> poolConfig, PooledObjectFactory<JedisConnection> factory) {
    super(poolConfig, factory);
  }
}
