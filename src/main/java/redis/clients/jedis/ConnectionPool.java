package redis.clients.jedis;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.authentication.core.AuthXManagerFactory;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenListener;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.JedisAuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(hostAndPort, clientConfig, createAuthXManager(clientConfig));
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      JedisAuthXManager authXManager) {
    this(new ConnectionFactory(hostAndPort, clientConfig, null, authXManager));
    attachAuthXManager(authXManager);
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
    attachAuthXManager(authXManager);
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
    attachAuthXManager(authXManager);
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

  private static JedisAuthXManager createAuthXManager(JedisClientConfig clientConfig) {
    return (clientConfig.getTokenAuthConfig() != null)
        ? (JedisAuthXManager) AuthXManagerFactory.create(JedisAuthXManager.class,
          clientConfig.getTokenAuthConfig())
        : null;
  }

  private void attachAuthXManager(JedisAuthXManager authXManager) {
    if (authXManager != null) {
      authXManager.setListener(new TokenListener() {
        @Override
        public void onTokenRenewed(Token token) {
          try {
            ConnectionPool.this.evict();
            System.out.println("pool evict: " + ConnectionPool.this.hashCode());

          } catch (Exception e) {
            throw new JedisException("Failed to evict connections from pool", e);
          }
        }

        @Override
        public void onError(Exception reason) {
        }
      });
    }
  }
}
