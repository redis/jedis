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
    installMaintenanceHooks(factory);
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    super(factory, poolConfig);
    installMaintenanceHooks(factory);
  }

  /**
   * Install pool-side maintenance hooks when the factory was constructed with a controller: wrap
   * the existing eviction policy in a rebind-aware one and route MOVING handoffs to an immediate
   * selective {@link #evict()}. Controller creation/injection happens upstream in the convenience
   * constructors; this method only wires the pool-level pieces.
   */
  private void installMaintenanceHooks(PooledObjectFactory<Connection> factory) {
    if (!(factory instanceof ConnectionFactory)) {
      return;
    }
    MaintenanceEventController controller = ((ConnectionFactory) factory).getMaintenanceController();
    if (controller == null) {
      return;
    }
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
    this(buildFactoryWithMaintenance(hostAndPort, clientConfig, null));
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(buildFactoryWithMaintenance(hostAndPort, clientConfig, null), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache) {
    this(buildFactoryWithMaintenance(hostAndPort, clientConfig, clientSideCache));
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig) {
    this(buildFactoryWithMaintenance(hostAndPort, clientConfig, clientSideCache), poolConfig);
    attachAuthenticationListener(clientConfig.getAuthXManager());
  }

  /**
   * Build a {@link ConnectionFactory} for the convenience constructors, wiring a maintenance
   * controller into it at construction when {@link MaintenanceNotificationsConfig#isEnabledOrAuto()}
   * holds. The controller, when present, is injected into the default {@code Connection.Builder}
   * (for push delivery) and into the default {@link DefaultJedisSocketFactory} (as the post-DNS
   * address mapper for MOVING redirects).
   */
  private static ConnectionFactory buildFactoryWithMaintenance(HostAndPort hostAndPort,
      JedisClientConfig clientConfig, Cache cache) {
    ConnectionFactory.Builder b = ConnectionFactory.builder().hostAndPort(hostAndPort)
        .clientConfig(clientConfig);
    if (cache != null) {
      b.cache(cache);
    }
    MaintenanceNotificationsConfig maint = clientConfig.maintNotificationsConfig();
    if (maint != null && maint.isEnabledOrAuto()) {
      b.maintenanceController(MaintenanceEventController.from(maint));
    }
    return b.build();
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
