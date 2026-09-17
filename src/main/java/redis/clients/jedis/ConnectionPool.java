package redis.clients.jedis;

import java.util.function.Consumer;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.authentication.core.Token;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class ConnectionPool extends Pool<Connection> {

  private AuthXManager authXManager;
  private PoolMaintenance maintenance = PoolMaintenance.OFF;
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

  /**
   * Creates the pool with maintenance notifications configured for its connections; {@code null}
   * disables them.
   * @since 8.1
   */
  @Experimental
  public ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      Cache clientSideCache, GenericObjectPoolConfig<Connection> poolConfig,
      MaintenanceNotificationsConfig maintConfig) {
    this(ConnectionFactory.builder().hostAndPort(hostAndPort).clientConfig(clientConfig)
        .cache(clientSideCache), poolConfig, maintConfig);
  }

  /**
   * Creates the pool from a connection-factory builder with maintenance notifications configured
   * for its connections; {@code null} disables them.
   * @since 8.1
   */
  @Experimental
  public ConnectionPool(ConnectionFactory.Builder factoryBuilder,
      GenericObjectPoolConfig<Connection> poolConfig, MaintenanceNotificationsConfig maintConfig) {
    this(factoryBuilder, poolConfig, PoolMaintenance.standalone(maintConfig));
  }

  ConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig, Cache clientSideCache,
      GenericObjectPoolConfig<Connection> poolConfig, PoolMaintenance maintenance) {
    this(ConnectionFactory.builder().hostAndPort(hostAndPort).clientConfig(clientConfig)
        .cache(clientSideCache), poolConfig, maintenance);
  }

  private ConnectionPool(ConnectionFactory.Builder factoryBuilder,
      GenericObjectPoolConfig<Connection> poolConfig, PoolMaintenance maintenance) {
    super(maintenance.configure(factoryBuilder).build(), poolConfig);
    this.maintenance = maintenance;
    attachAuthenticationListener(factoryBuilder.getClientConfig().getAuthXManager());
    if (maintenance == PoolMaintenance.OFF) {
      this.returnHook = super::returnResource;
    } else {
      // a retired connection (Connection.isRetired) is destroyed on return instead of reused
      this.returnHook = c -> {
        if (c.isRetired()) {
          super.returnBrokenResource(c);
        } else {
          super.returnResource(c);
        }
      };
    }
    maintenance.attach(this);
  }

  /** Exposes the pool's maintenance controller ({@code null} when off) for test clock injection. */
  @VisibleForTesting
  MaintenanceController getMaintenanceController() {
    return maintenance.controller();
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
      maintenance.close();
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
