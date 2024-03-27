package redis.clients.jedis.providers;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.util.Pool;

public class PooledConnectionProvider implements ConnectionProvider {

  private final Pool<Connection> pool;
  private Object connectionMapKey = "";

  public PooledConnectionProvider(HostAndPort hostAndPort) {
    this(new ConnectionFactory(hostAndPort));
    this.connectionMapKey = hostAndPort;
  }

  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionPool(hostAndPort, clientConfig));
    this.connectionMapKey = hostAndPort;
  }

  @Experimental
  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig, ClientSideCache clientSideCache) {
    this(new ConnectionPool(hostAndPort, clientConfig, clientSideCache));
    this.connectionMapKey = hostAndPort;
  }

  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionPool(hostAndPort, clientConfig, poolConfig));
    this.connectionMapKey = hostAndPort;
  }

  @Experimental
  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionPool(hostAndPort, clientConfig, clientSideCache, poolConfig));
    this.connectionMapKey = hostAndPort;
  }

  public PooledConnectionProvider(PooledObjectFactory<Connection> factory) {
    this(new ConnectionPool(factory));
    this.connectionMapKey = factory;
  }

  public PooledConnectionProvider(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionPool(factory, poolConfig));
    this.connectionMapKey = factory;
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

  @Override
  public Map<?, Pool<Connection>> getConnectionMap() {
    return Collections.singletonMap(connectionMapKey, pool);
  }
}
