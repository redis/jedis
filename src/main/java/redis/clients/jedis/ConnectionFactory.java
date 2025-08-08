package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

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

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    public Connection.Builder getConnectionBuilder() {
      return connectionBuilder;
    }

    public JedisSocketFactory getJedisSocketFactory() {
      return jedisSocketFactory;
    }

    public Cache getCache() {
      return cache;
    }

    public Builder setClientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public Builder setConnectionBuilder(Connection.Builder connectionBuilder) {
      this.connectionBuilder = connectionBuilder;
      return this;
    }

    public Builder setJedisSocketFactory(JedisSocketFactory jedisSocketFactory) {
      this.jedisSocketFactory = jedisSocketFactory;
      return this;
    }

    public Builder setCache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public Builder setHostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    public ConnectionFactory build() {
      if (jedisSocketFactory == null) {
        this.jedisSocketFactory = defaultJedisSocketFactory();
      }
      if (connectionBuilder == null) {
        this.connectionBuilder = defaultConnectionBuilder();
      }
      return new ConnectionFactory(this);
    }

    private JedisSocketFactory defaultJedisSocketFactory() {
      if (clientConfig == null) {
        clientConfig = DefaultJedisClientConfig.builder().build();
      }
      return new DefaultJedisSocketFactory(hostAndPort, clientConfig);
    }

    private Connection.Builder defaultConnectionBuilder() {
      Connection.Builder connBuilder = cache == null ? Connection.builder() : CacheConnection.builder(cache);
      connBuilder.setSocketFactory(jedisSocketFactory).setClientConfig(clientConfig);
      return connBuilder;
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
    this(hostAndPort, DefaultJedisClientConfig.builder().build(), null);
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(clientConfig, Connection.builder().setSocketFactory(new DefaultJedisSocketFactory(hostAndPort, clientConfig)));
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig, Cache csCache) {
    this(clientConfig,
    // TODO: not sure if we need to check cache is null or not.
      CacheConnection.builder(csCache).setSocketFactory(new DefaultJedisSocketFactory(hostAndPort, clientConfig)));
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    this(clientConfig, Connection.builder().setClientConfig(clientConfig).setSocketFactory(jedisSocketFactory));
  }

  private ConnectionFactory(JedisClientConfig clientConfig, Connection.Builder connectionBuilder) {
    this(builder().setClientConfig(clientConfig).setConnectionBuilder(connectionBuilder));
  }

  public ConnectionFactory(Builder builder) {
    this.clientConfig = builder.getClientConfig();
    this.connectionBuilder = builder.getConnectionBuilder();

    initAuthXManager();
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
    return connectionBuilder.build();
  }

  // private Connection.Builder defaultConnectionBuilder() {
  // Connection.Builder connBuilder = clientSideCache == null ? Connection.builder()
  // : CacheConnection.builder(clientSideCache);
  // connBuilder.setSocketFactory(jedisSocketFactory).setClientConfig(clientConfig);
  // return connBuilder;
  // }

  // public Connection.Builder getConnectionBuilder() {
  // return connectionBuilder;
  // }

  // public void setConnectionBuilder(Connection.Builder connectionBuilder) {
  // this.connectionBuilder = connectionBuilder;
  // }

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
