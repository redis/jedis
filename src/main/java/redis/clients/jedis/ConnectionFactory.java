package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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

    private JedisSocketFactory jedisSocketFactory;
    private JedisClientConfig clientConfig;
    private Cache cache;
    private InitializationTracker<Connection> tracker;
    private HostAndPort hostAndPort;

    public JedisSocketFactory getJedisSocketFactory() {
      return jedisSocketFactory;
    }

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    public Cache getCache() {
      return cache;
    }

    public InitializationTracker<Connection> getTracker() {
      return tracker;
    }

    public Builder setJedisSocketFactory(JedisSocketFactory jedisSocketFactory) {
      this.jedisSocketFactory = jedisSocketFactory;
      return this;
    }

    public Builder setClientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public Builder setCache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public Builder setTracker(InitializationTracker<Connection> tracker) {
      this.tracker = tracker;
      return this;
    }

    public Builder setHostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    public ConnectionFactory build() {
      return new ConnectionFactory(this);
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisSocketFactory jedisSocketFactory;
  private final JedisClientConfig clientConfig;
  private final Cache clientSideCache;
  private Supplier<Connection> objectMaker;

  private AuthXEventListener authXEventListener;

  private InitializationTracker<Connection> tracker;

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this(hostAndPort, DefaultJedisClientConfig.builder().build(), null);
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(hostAndPort, clientConfig, null);
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig,
      Cache csCache) {
    this(new DefaultJedisSocketFactory(hostAndPort, clientConfig), clientConfig, csCache);
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig) {
    this(jedisSocketFactory, clientConfig, null);
  }

  private ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig, Cache csCache) {

    this.jedisSocketFactory = jedisSocketFactory;
    this.clientSideCache = csCache;
    this.clientConfig = clientConfig;

    initAuthXManager();
  }

  public ConnectionFactory(Builder builder) {
    this.clientConfig = builder.getClientConfig() != null ? builder.getClientConfig()
      : DefaultJedisClientConfig.builder().build();
    if (builder.getJedisSocketFactory() == null) {
      this.jedisSocketFactory = new DefaultJedisSocketFactory(builder.hostAndPort, this.clientConfig);
    } else {
      this.jedisSocketFactory = builder.getJedisSocketFactory();
    }
    this.clientSideCache = builder.getCache();
    this.tracker = builder.getTracker();

    initAuthXManager();
  }

  private void initAuthXManager() {
    AuthXManager authXManager = clientConfig.getAuthXManager();
    if (authXManager == null) {
      this.objectMaker = connectionSupplier();
      this.authXEventListener = AuthXEventListener.NOOP_LISTENER;
    } else {
      Supplier<Connection> supplier = connectionSupplier();
      this.objectMaker = () -> (Connection) authXManager.addConnection(supplier.get());
      this.authXEventListener = authXManager.getListener();
      authXManager.start();
    }
  }

  private Supplier<Connection> connectionSupplier() {
    Connection.Builder conBuilder = clientSideCache == null ? Connection.builder()
      : CacheConnection.builder(clientSideCache);
    conBuilder.setSocketFactory(jedisSocketFactory).setClientConfig(clientConfig);
    if (tracker != null) {
      conBuilder.setTracker(tracker);
    }
    return () -> conBuilder.build();
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
