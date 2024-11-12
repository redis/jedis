package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.JedisAuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  private JedisAuthXManager authXManager;

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(hostAndPort, clientConfig, createAuthXManager(clientConfig));
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      JedisAuthXManager authXManager) {
    this(new ConnectionFactory(hostAndPort, clientConfig, null, authXManager));
    attachAuthenticationListener(authXManager);
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache) {
    this(hostAndPort, clientConfig, clientSideCache, createAuthXManager(clientConfig));
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, JedisAuthXManager authXManager) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache, authXManager));
    attachAuthenticationListener(authXManager);
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(hostAndPort, clientConfig, null, createAuthXManager(clientConfig), poolConfig);
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig) {
    this(hostAndPort, clientConfig, clientSideCache, createAuthXManager(clientConfig), poolConfig);
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, JedisAuthXManager authXManager,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache, authXManager),
        poolConfig);
        attachAuthenticationListener(authXManager);
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

  @Override
  public void close() {
    if (authXManager != null) {
      authXManager.stop();
    }
    super.close();
  }

  private static JedisAuthXManager createAuthXManager(JedisClientConfig config) {
    if (config.getTokenAuthConfig() != null) {
      return new JedisAuthXManager(config.getTokenAuthConfig());
    }
    return null;
  }

  private void attachAuthenticationListener(JedisAuthXManager authXManager) {
    this.authXManager = authXManager;
    if (authXManager != null) {
      authXManager.setListener(token -> {
        try {
          // this is to trigger validations on each connection via ConnectionFactory
          evict();
        } catch (Exception e) {
          throw new JedisException("Failed to evict connections from pool", e);
        }
      });
    }
  }
}
