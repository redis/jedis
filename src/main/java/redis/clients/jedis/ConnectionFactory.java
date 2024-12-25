package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisException;
import today.bonfire.oss.sop.PooledObjectFactory;

/**
 * PoolableObjectFactory custom impl.
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisSocketFactory jedisSocketFactory;
  private final JedisClientConfig clientConfig;
  private Cache clientSideCache = null;

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this.clientConfig = DefaultJedisClientConfig.builder().build();
    this.jedisSocketFactory = new DefaultJedisSocketFactory(hostAndPort);
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = new DefaultJedisSocketFactory(hostAndPort, this.clientConfig);
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig,
      Cache csCache) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = new DefaultJedisSocketFactory(hostAndPort, this.clientConfig);
    this.clientSideCache = csCache;
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = jedisSocketFactory;
  }

  @Override
  public Connection createObject() {
    try {
      return clientSideCache == null ? new Connection(jedisSocketFactory, clientConfig)
          : new CacheConnection(jedisSocketFactory, clientConfig, clientSideCache);
    } catch (JedisException je) {
      logger.debug("Error while creating object", je);
      throw je;
    }
  }

  @Override
  public void activateObject(Connection obj) {
    // no-op
  }

  @Override
  public void passivateObject(Connection obj) {
    // no-op
  }

  @Override
  public boolean isObjectValidForBorrow(Connection obj) {
    try {
      // Quick validation before borrowing - same as previous validateObject
      return obj.isConnected();
    } catch (final Exception e) {
      logger.warn("Error while validating connection for borrow.", e);
      return false;
    }
  }

  @Override
  public boolean isObjectValid(Connection obj) {
    try {
      // Full validation - same logic as before
      return obj.isConnected() && obj.ping();
    } catch (final Exception e) {
      logger.warn("Error while validating connection.", e);
      return false;
    }
  }

  @Override
  public void destroyObject(Connection obj) {
    if (obj.isConnected()) {
      try {
        obj.close();
      } catch (RuntimeException e) {
        logger.debug("Error while closing", e);
      }
    }
  }
}
