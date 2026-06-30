package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import redis.clients.jedis.TimeoutSupplier.DefaultTimeoutCard;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.authentication.JedisAuthenticationException;
import redis.clients.jedis.authentication.AuthXEventListener;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisException;

/**
 * PoolableObjectFactory custom impl.
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> {

  public static class Builder {
    private JedisClientConfig clientConfig;
    private Connection.Builder connectionBuilder;
    private JedisSocketFactory jedisSocketFactory;
    private Cache cache;
    private HostAndPort hostAndPort;
    private MaintenanceEventController maintenanceController;

    // Fluent API methods (preferred)
    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public Builder connectionBuilder(Connection.Builder connectionBuilder) {
      this.connectionBuilder = connectionBuilder;
      return this;
    }

    public Builder socketFactory(JedisSocketFactory jedisSocketFactory) {
      this.jedisSocketFactory = jedisSocketFactory;
      return this;
    }

    public Builder cache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public Builder hostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    /**
     * Maintenance controller propagated to the default socket factory (as the post-DNS
     * address mapper for MOVING redirects) and to the default {@link Connection.Builder} (so each
     * connection forwards push frames to it). {@code null} disables maintenance for this factory.
     */
    Builder maintenanceController(MaintenanceEventController maintenanceController) {
      this.maintenanceController = maintenanceController;
      return this;
    }

    public Connection.Builder getConnectionBuilder() {
      return connectionBuilder;
    }

    public JedisSocketFactory getJedisSocketFactory() {
      return jedisSocketFactory;
    }

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    public Cache getCache() {
      return cache;
    }

    public ConnectionFactory build() {
      withDefaults();
      return create();
    }

    protected ConnectionFactory create() {
      return new ConnectionFactory(this);
    }

    private Builder withDefaults() {
      if (jedisSocketFactory == null) {
        this.jedisSocketFactory = createDefaultSocketFactory();
      }
      if (connectionBuilder == null) {
        this.connectionBuilder = createDefaultConnectionBuilder();
      }
      return this;
    }

    private JedisSocketFactory createDefaultSocketFactory() {
      if (clientConfig == null) {
        clientConfig = DefaultJedisClientConfig.builder().build();
      }
      if (hostAndPort == null) {
        throw new IllegalStateException("HostAndPort is required when no socketFactory is provided");
      }
      return new DefaultJedisSocketFactory(hostAndPort, clientConfig, maintenanceController);
    }

    private Connection.Builder createDefaultConnectionBuilder() {
      Connection.Builder connBuilder = cache == null ? Connection.builder() : CacheConnection.builder(cache);
      connBuilder.socketFactory(jedisSocketFactory).clientConfig(clientConfig);
      if (maintenanceController != null) {
        connBuilder.maintenanceConfig(maintenanceController.getConfig())
            .addMaintenanceEventListener(maintenanceController)
            .timeoutSupplier(rebindSoTimeoutSupplier(maintenanceController, clientConfig));
      }
      return connBuilder;
    }

    /**
     * SO_TIMEOUT override that relaxes a connection's timeout while a MOVING rebind window is active
     * in the pool, and defers ({@link JedisClientConfig#UNSET_TIMEOUT_MS}) otherwise so the
     * connection falls back to its own per-receiver / configured calculation. Relaxed values are
     * captured from the (immutable) client config at wiring time; an unset relaxed value is itself
     * {@code UNSET_TIMEOUT_MS}, so it naturally defers.
     */
    private static TimeoutSupplier rebindSoTimeoutSupplier(MaintenanceEventController controller,
        JedisClientConfig clientConfig) {

      return new AdvancedTimeoutSupplier(
          new MaintenanceAwareTimeoutCard(clientConfig.getSocketTimeoutMillis(),
              clientConfig.getBlockingSocketTimeoutMillis(), controller));
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisClientConfig clientConfig;
  private Supplier<Connection> objectMaker;
  private Connection.Builder connectionBuilder;

  private AuthXEventListener authXEventListener;

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this(builder().hostAndPort(hostAndPort).withDefaults());
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(builder().hostAndPort(hostAndPort).clientConfig(clientConfig).withDefaults());
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig, Cache csCache) {
    this(builder().hostAndPort(hostAndPort).clientConfig(clientConfig).cache(csCache).withDefaults());
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    this(builder().socketFactory(jedisSocketFactory).clientConfig(clientConfig).withDefaults());
  }

  public ConnectionFactory(Builder builder) {
    this.clientConfig = builder.getClientConfig();
    this.connectionBuilder = builder.getConnectionBuilder();

    initAuthXManager();
  }

  JedisClientConfig getClientConfig() {
    return clientConfig;
  }

  /** Visible for testing: the underlying connection builder (and via it, the socket factory). */
  Connection.Builder getConnectionBuilder() {
    return connectionBuilder;
  }

  private void initAuthXManager() {
    AuthXManager authXManager = clientConfig.getAuthXManager();
    if (authXManager == null) {
      this.objectMaker = () -> build();
      this.authXEventListener = AuthXEventListener.NOOP_LISTENER;
    } else {
      this.objectMaker = () -> (Connection) authXManager.addConnection(build());
      this.authXEventListener = authXManager.getListener();
      authXManager.start();
    }
  }

  private Connection build() {
    Connection conn = connectionBuilder.buildUninitialized();
    initialize(conn);
    return conn;
  }

  /**
   * Initialize a freshly built {@link Connection}.
   * <p>
   * Subclasses may override to wrap initialization (for example, with cancellation tracking),
   * but must ensure {@link Connection#initializeFromClientConfig()} is invoked exactly once.
   */
  protected void initialize(Connection conn) {
      conn.initializeFromClientConfig();
  }

  @Override
  public void activateObject(PooledObject<Connection> pooledConnection) throws Exception {
    // what to do ??
  }

  @Override
  public void destroyObject(PooledObject<Connection> pooledConnection) throws Exception {
    final Connection jedis = pooledConnection.getObject();
    if (jedis.isConnected()) {
      try {
        jedis.close();
      } catch (RuntimeException e) {
        logger.debug("Error while close", e);
      }
    }
  }

  @Override
  public PooledObject<Connection> makeObject() throws Exception {
    try {
      Connection jedis = objectMaker.get();
      return new DefaultPooledObject<>(jedis);
    } catch (JedisException je) {
      logger.debug("Error while makeObject", je);
      throw je;
    }
  }

  @Override
  public void passivateObject(PooledObject<Connection> pooledConnection) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
    Connection jedis = pooledConnection.getObject();
    reAuthenticate(jedis);
  }

  @Override
  public boolean validateObject(PooledObject<Connection> pooledConnection) {
    final Connection jedis = pooledConnection.getObject();
    try {
      // check HostAndPort ??
      if (!jedis.isConnected()) {
        return false;
      }
      reAuthenticate(jedis);
      return jedis.ping();
    } catch (final Exception e) {
      logger.warn("Error while validating pooled Connection object.", e);
      return false;
    }
  }

  private void reAuthenticate(Connection jedis) throws Exception {
    try {
      String result = jedis.reAuthenticate();
      if (result != null && !result.equals("OK")) {
        String msg = "Re-authentication failed with server response: " + result;
        Exception failedAuth = new JedisAuthenticationException(msg);
        logger.error(failedAuth.getMessage(), failedAuth);
        authXEventListener.onConnectionAuthenticationError(failedAuth);
        return;
      }
    } catch (Exception e) {
      logger.error("Error while re-authenticating connection", e);
      authXEventListener.onConnectionAuthenticationError(e);
      throw e;
    }
  }
}
