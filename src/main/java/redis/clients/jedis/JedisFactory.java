package redis.clients.jedis;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * PoolableObjectFactory custom impl.
 */
// Legacy
public class JedisFactory implements PooledObjectFactory<Jedis> {

  private static final Logger logger = LoggerFactory.getLogger(JedisFactory.class);

  private final JedisSocketFactory jedisSocketFactory;

  private final JedisClientConfig clientConfig;

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, password, database, clientName, false, null, null, null);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String user, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName, false, null, null, null);
  }

  /**
   * {@link #setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be called later.
   */
  JedisFactory(final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName, false, null, null, null);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, null, password, database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName,
               final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
               final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  protected JedisFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = new DefaultJedisSocketFactory(hostAndPort, this.clientConfig);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String user, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this.clientConfig = DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout).user(user)
        .password(password).database(database).clientName(clientName)
        .ssl(ssl).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build();
    this.jedisSocketFactory = new DefaultJedisSocketFactory(new HostAndPort(host, port), this.clientConfig);
  }

  protected JedisFactory(final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = jedisSocketFactory;
  }

  /**
   * {@link #setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be called later.
   */
  JedisFactory(final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout).user(user)
        .password(password).database(database).clientName(clientName)
        .ssl(ssl).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build());
  }

  /**
   * {@link JedisFactory#setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be called later.
   */
  JedisFactory(final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = new DefaultJedisSocketFactory(clientConfig);
  }

  protected JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName) {
    this(uri, connectionTimeout, soTimeout, clientName, null, null, null);
  }

  protected JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, connectionTimeout, soTimeout, 0, clientName, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  protected JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String clientName, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
          "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }
    this.clientConfig = DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
        .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
        .database(JedisURIHelper.getDBIndex(uri)).clientName(clientName)
        .protocol(JedisURIHelper.getRedisProtocol(uri))
        .ssl(JedisURIHelper.isRedisSSLScheme(uri)).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build();
    this.jedisSocketFactory = new DefaultJedisSocketFactory(new HostAndPort(uri.getHost(), uri.getPort()), this.clientConfig);
  }

  void setHostAndPort(final HostAndPort hostAndPort) {
    if (!(jedisSocketFactory instanceof DefaultJedisSocketFactory)) {
      throw new IllegalStateException("setHostAndPort method has limited capability.");
    }
    ((DefaultJedisSocketFactory) jedisSocketFactory).updateHostAndPort(hostAndPort);
  }

  @Override
  public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final Jedis jedis = pooledJedis.getObject();
    if (jedis.getDB() != clientConfig.getDatabase()) {
      jedis.select(clientConfig.getDatabase());
    }
  }

  @Override
  public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final Jedis jedis = pooledJedis.getObject();
    if (jedis.isConnected()) {
      try {
        jedis.close();
      } catch (RuntimeException e) {
        logger.debug("Error while close", e);
      }
    }
  }

  @Override
  public PooledObject<Jedis> makeObject() throws Exception {
    Jedis jedis = null;
    try {
      jedis = new Jedis(jedisSocketFactory, clientConfig);
      return new DefaultPooledObject<>(jedis);
    } catch (JedisException je) {
      logger.debug("Error while makeObject", je);
      throw je;
    }
  }

  @Override
  public void passivateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
  }

  @Override
  public boolean validateObject(PooledObject<Jedis> pooledJedis) {
    final Jedis jedis = pooledJedis.getObject();
    try {
      boolean targetHasNotChanged = true;
      if (jedisSocketFactory instanceof DefaultJedisSocketFactory) {
        HostAndPort targetAddress = ((DefaultJedisSocketFactory) jedisSocketFactory).getHostAndPort();
        HostAndPort objectAddress = jedis.getConnection().getHostAndPort();

        targetHasNotChanged = targetAddress.getHost().equals(objectAddress.getHost())
            && targetAddress.getPort() == objectAddress.getPort();
      }

      return targetHasNotChanged
          && jedis.getConnection().isConnected()
          && jedis.ping().equals("PONG");
    } catch (final Exception e) {
      logger.warn("Error while validating pooled Jedis object.", e);
      return false;
    }
  }
}
