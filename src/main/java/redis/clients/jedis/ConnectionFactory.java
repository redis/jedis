package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.JedisAuthXManager;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisException;

/**
 * PoolableObjectFactory custom impl.
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisSocketFactory jedisSocketFactory;
  private final JedisClientConfig clientConfig;
  private final Cache clientSideCache;
  private final Supplier<Connection> objectMaker;

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this(hostAndPort, DefaultJedisClientConfig.builder().build(), null, null);
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(hostAndPort, clientConfig, null, null);
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig,
      Cache csCache, JedisAuthXManager authXManager) {
    this(new DefaultJedisSocketFactory(hostAndPort, clientConfig), clientConfig, csCache,
        authXManager);
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig) {
    this(jedisSocketFactory, clientConfig, null, null);
  }

  private ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig, Cache csCache, JedisAuthXManager authXManager) {

    this.jedisSocketFactory = jedisSocketFactory;
    this.clientSideCache = csCache;

    if (authXManager == null) {
      this.clientConfig = clientConfig;
      this.objectMaker = connectionSupplier();
    } else {
      this.clientConfig = replaceCredentialsProvider(clientConfig,
        authXManager);
      Supplier<Connection> supplier = connectionSupplier();
      this.objectMaker = () -> (Connection) authXManager.addConnection(supplier.get());

      try {
        authXManager.start(true);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new JedisException("AuthXManager failed to start!", e);
      }
    }
  }

  private JedisClientConfig replaceCredentialsProvider(JedisClientConfig origin,
      Supplier<RedisCredentials> newCredentialsProvider) {
    return DefaultJedisClientConfig.builder().from(origin)
        .credentialsProvider(newCredentialsProvider).build();
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
    jedis.reAuth();
  }

  @Override
  public boolean validateObject(PooledObject<Connection> pooledConnection) {
    final Connection jedis = pooledConnection.getObject();
    try {
      // check HostAndPort ??
      jedis.reAuth();
      return jedis.isConnected() && jedis.ping();
    } catch (final Exception e) {
      logger.warn("Error while validating pooled Connection object.", e);
      return false;
    }
  }
}
