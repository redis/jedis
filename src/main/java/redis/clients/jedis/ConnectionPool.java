package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionPool extends Pool<Connection> {

  private AuthXManager authXManager;
  private RebindHandler rebindHandler;

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
    attachAuthenticationListener(clientConfig.getAuthXManager());
    attachRebindHandler(clientConfig, (ConnectionFactory) this.getFactory());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache));
    attachAuthenticationListener(clientConfig.getAuthXManager());
    attachRebindHandler(clientConfig, (ConnectionFactory) this.getFactory());
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
    attachRebindHandler(clientConfig, (ConnectionFactory) this.getFactory());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
    attachRebindHandler(clientConfig, (ConnectionFactory) this.getFactory());
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
    try {
      if (authXManager != null) {
        authXManager.stop();
      }
    } finally {
      super.close();
    }
  }

  private void attachAuthenticationListener(AuthXManager authXManager) {
    this.authXManager = authXManager;
    if (authXManager != null) {
      authXManager.addPostAuthenticationHook(token -> {
        try {
          // this is to trigger validations on each connection via ConnectionFactory
          evict();
        } catch (Exception e) {
          throw new JedisException("Failed to evict connections from pool", e);
        }
      });
    }
  }

  private void attachRebindHandler(JedisClientConfig clientConfig, ConnectionFactory factory) {
    if (clientConfig.isProactiveRebindEnabled()) {
      rebindHandler = new RebindHandler(this, factory);
      clientConfig.getMaintenanceEventHandler().addListener(rebindHandler);
    }
  }

  private static class RebindHandler implements MaintenanceEventListener {
    private final ConnectionPool pool;
    private final ConnectionFactory factory;
    private final AtomicReference<HostAndPort> rebindTarget = new AtomicReference<>();

    public RebindHandler(ConnectionPool pool, ConnectionFactory factory) {
      this.pool = pool;
      this.factory = factory;
     }

    @Override
    public void onRebind(HostAndPort target, Duration rebindTimeout) {
      HostAndPort previous = rebindTarget.getAndSet(target);
      if (previous != target) {
        this.factory.rebind(target, rebindTimeout);
        this.pool.clear();
      }
    }
  }
}
