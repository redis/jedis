package redis.clients.jedis;

import java.util.function.Consumer;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.Token;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

  private AuthXManager authXManager;
  private MaintenanceEventController maintenanceController; // null = maintenance off
  private final Consumer<Connection> returnHook;

  // Primary constructors using factory
  public ConnectionPool(PooledObjectFactory<Connection> factory) {
    super(factory);
    this.returnHook = super::returnResource;
  }

  public ConnectionPool(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    super(factory, poolConfig);
    this.returnHook = super::returnResource;
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

  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig,
      MaintenanceNotificationsConfig maintConfig) {
    this(ConnectionFactory.builder().hostAndPort(hostAndPort).clientConfig(clientConfig)
        .cache(clientSideCache), poolConfig, maintConfig);
  }

  private static MaintenanceEventController controllerFor(MaintenanceNotificationsConfig config) {
    return config != null && config.isEnabledOrAuto() ? MaintenanceEventController.from(config)
        : null;
  }

  @Experimental
  public ConnectionPool(ConnectionFactory.Builder factoryBuilder,
      GenericObjectPoolConfig<Connection> poolConfig, MaintenanceNotificationsConfig maintConfig) {
    this(factoryBuilder, poolConfig, controllerFor(maintConfig));
  }

  private ConnectionPool(ConnectionFactory.Builder factoryBuilder,
      GenericObjectPoolConfig<Connection> poolConfig, MaintenanceEventController controller) {
    super(factoryBuilder.maintenanceController(controller).build(), poolConfig);
    this.maintenanceController = controller;
    attachAuthenticationListener(factoryBuilder.getClientConfig().getAuthXManager());
    if (controller != null) {
      setEvictionPolicy(new RebindAwareEvictionPolicy(getEvictionPolicy()));
      controller.addHandoffHook(this::evictQuietly); // handoff processed: evict the marked idles
      // Marked connections are routed to returnBrokenResource by Connection.close(); the hook
      // covers direct returnResource callers.
      returnHook = c -> {
        if (c.isMarkedForReconnect()) {
          super.returnBrokenResource(c);
        } else {
          super.returnResource(c);
        }
      };
    } else {
      returnHook = super::returnResource;
    }
  }

  /**
   * Handoff-hook reaction: evict marked idles. Runs on the maintenance scheduler thread or inline
   * on a notifying thread; must never propagate (a failed pass degrades to lazy recycling on
   * return).
   */
  private void evictQuietly() {
    if (isClosed()) {
      return;
    }
    try {
      evict();
    } catch (Exception e) {
      log.warn("Maintenance eviction pass failed; marked connections recycle on return", e);
    }
  }

  /** Exposes the pool's maintenance controller ({@code null} when off) for test clock injection. */
  @VisibleForTesting
  MaintenanceEventController getMaintenanceController() {
    return maintenanceController;
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

  @Override
  public void destroy() {
    try {
      super.destroy();
    } finally {
      if (maintenanceController != null) {
        maintenanceController.close();
      }
    }
  }

  @Override
  public void returnResource(final Connection resource) {
    returnHook.accept(resource);
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
