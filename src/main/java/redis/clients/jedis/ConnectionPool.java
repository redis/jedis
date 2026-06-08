package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.authentication.core.Token;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  private AuthXManager authXManager;

  // Primary constructors using factory
  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
    wireMaintenance(factory);
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    super(factory, poolConfig);
    wireMaintenance(factory);
  }

  /**
   * Wire maintenance event controller. Entry point for all pool constructors (convenience, factory,
   * and customer-supplied pools): when maintenance is enabled, create the pool-owned controller,
   * hand it to the factory, install the rebind-aware eviction policy (wrapping the user's
   * existing policy), and route handoffs to an immediate selective {@link #evict()}.
   */
  private void wireMaintenance(PooledObjectFactory<Connection> factory) {
    if (!(factory instanceof ConnectionFactory)) {
      return;
    }
    ConnectionFactory cf = (ConnectionFactory) factory;
    MaintenanceNotificationsConfig maint = cf.getClientConfig().maintNotificationsConfig();
    if (!maint.isEnabled()) {
      return;
    }
    MaintenanceEventController controller = MaintenanceEventController.from(maint);
    cf.attachMaintenanceController(controller);
    setEvictionPolicy(new RebindAwareEvictionPolicy(controller, getEvictionPolicy()));
    controller.addHandoffHook(handoff -> evictQuietly());
  }

  /** Wraps the checked-Exception {@link #evict()}. */
  private void evictQuietly() {
    try {
      evict();
    } catch (Exception e) {
      throw new JedisException("Failed to evict pool on maintenance handoff", e);
    }
  }

  // Convenience constructors
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig));
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache));
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig, clientSideCache), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
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

  protected void attachAuthenticationListener(AuthXManager authXManager) {
    this.authXManager = authXManager;
    if (authXManager != null) {
      authXManager.addPostAuthenticationHook(this::postAuthentication);
    }
  }

  protected void detachAuthenticationListener() {
    if (authXManager != null) {
      authXManager.removePostAuthenticationHook(this::postAuthentication);
    }
  }

  private void postAuthentication(Token token) {
    try {
      // this is to trigger validations on each connection via ConnectionFactory
      evict();
    } catch (Exception e) {
      throw new JedisException("Failed to evict connections from pool", e);
    }
  }
}
