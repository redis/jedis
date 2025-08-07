package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.authentication.JedisAuthenticationException;
import redis.clients.jedis.authentication.AuthXEventListener;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Expirable;

/**
 * PoolableObjectFactory custom impl.
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> , RebindAware {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisSocketFactory jedisSocketFactory;
  private final JedisClientConfig clientConfig;
  private final Cache clientSideCache;
  private final Supplier<Connection> objectMaker;

  private final AuthXEventListener authXEventListener;
  private final RebindAwareHostPortSupplier rebindAwareHostPortSupplier;

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

    if (clientConfig.isProactiveRebindEnabled()) {
      if (!(jedisSocketFactory instanceof DefaultJedisSocketFactory)) {
        throw new IllegalStateException("Rebind not supported for custom JedisSocketFactory implementations");
      }
      DefaultJedisSocketFactory factory = (DefaultJedisSocketFactory) jedisSocketFactory;
      this.rebindAwareHostPortSupplier = wrapHostAndPortSupplier(factory);
    } else {
      this.rebindAwareHostPortSupplier = null;
    }
  }

  private RebindAwareHostPortSupplier wrapHostAndPortSupplier(DefaultJedisSocketFactory factory) {
    RebindAwareHostPortSupplier hostPortSupplier =  new RebindAwareHostPortSupplier(factory.getHostAndPort(), factory.getHostAndPortSupplier());
    factory.setHostAndPortSupplier(hostPortSupplier);
    return  hostPortSupplier;
  }

  private Supplier<Connection> connectionSupplier() {
    return clientSideCache == null ? () -> new Connection(jedisSocketFactory, clientConfig)
        : () -> new CacheConnection(jedisSocketFactory, clientConfig, clientSideCache);
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

  @Override
  public void rebind(HostAndPort newHostAndPort, Duration rebindTimeout) {
    if (rebindAwareHostPortSupplier != null) {
      rebindAwareHostPortSupplier.rebind(newHostAndPort, rebindTimeout);
    }
  }

  private static class RebindAwareHostPortSupplier implements Supplier<HostAndPort>, RebindAware {
    private final Supplier<HostAndPort> delegatedSupplier;
    private final HostAndPort initialHostAndPort;
    private volatile Expirable<HostAndPort> rebindTarget;

    public RebindAwareHostPortSupplier(HostAndPort initialHostAndPort,
        Supplier<HostAndPort> hostAndPortSupplier) {
      this.initialHostAndPort = initialHostAndPort;
      this.delegatedSupplier = hostAndPortSupplier;
    }

    public void rebind(HostAndPort rebindTarget, Duration rebindTimeout) {
      this.rebindTarget = new Expirable<>(rebindTarget, rebindTimeout);
    }

    public HostAndPort get() {
      if (rebindTarget != null && rebindTarget.isValid()) {
        return rebindTarget.getValue();
      }

      if (delegatedSupplier != null) {
        return delegatedSupplier.get();
      }

      return initialHostAndPort;
    }

  }
}
