package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;
import today.bonfire.oss.sop.PooledObjectFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

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
    this(host, port, connectionTimeout, soTimeout, password, database, clientName, false, null,
        null, null);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String user, final String password, final int database,
      final String clientName) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout, final String user, final String password,
      final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database,
        clientName, false, null, null, null);
  }

  /**
   * {@link #setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be called later.
   */
  JedisFactory(final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName,
        false, null, null, null);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, null, password, database, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String user, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  protected JedisFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = new DefaultJedisSocketFactory(hostAndPort, this.clientConfig);
  }

  protected JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout, final String user, final String password,
      final int database, final String clientName, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this.clientConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).user(user).password(password)
        .database(database).clientName(clientName).ssl(ssl).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build();
    this.jedisSocketFactory = new DefaultJedisSocketFactory(new HostAndPort(host, port),
        this.clientConfig);
  }

  protected JedisFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    this.jedisSocketFactory = jedisSocketFactory;
  }

  /**
   * {@link #setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be called later.
   */
  JedisFactory(final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout).user(user)
        .password(password).database(database).clientName(clientName).ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  /**
   * {@link JedisFactory#setHostAndPort(redis.clients.jedis.HostAndPort) setHostAndPort} must be
   * called later.
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
    this(uri, connectionTimeout, soTimeout, 0, clientName, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  protected JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String clientName,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }
    this.clientConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).user(JedisURIHelper.getUser(uri))
        .password(JedisURIHelper.getPassword(uri)).database(JedisURIHelper.getDBIndex(uri))
        .clientName(clientName).protocol(JedisURIHelper.getRedisProtocol(uri))
        .ssl(JedisURIHelper.isRedisSSLScheme(uri)).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build();
    this.jedisSocketFactory = new DefaultJedisSocketFactory(new HostAndPort(uri.getHost(),
        uri.getPort()), this.clientConfig);
  }

  public void setHostAndPort(final HostAndPort hostAndPort) {
    if (jedisSocketFactory instanceof DefaultJedisSocketFactory jsf) {
      jsf.updateHostAndPort(hostAndPort);
    }else{
      throw new IllegalStateException("setHostAndPort method has limited capability.");
    }
  }

  @Override
  public Jedis createObject() {
    try {
      Jedis jedis = new Jedis(jedisSocketFactory, clientConfig);
      return jedis;
    } catch (JedisException je) {
      logger.debug("Error while creating object", je);
      throw je;
    }
  }

  @Override
  public void activateObject(Jedis obj) {
    if (obj.getDB() != clientConfig.getDatabase()) {
      obj.select(clientConfig.getDatabase());
    }
  }

  @Override
  public void passivateObject(Jedis obj) {
    // no-op
  }

  @Override
  public boolean isObjectValidForBorrow(Jedis obj) {
    try {
      boolean targetHasNotChanged = true;
      if (jedisSocketFactory instanceof DefaultJedisSocketFactory) {
        HostAndPort targetAddress = ((DefaultJedisSocketFactory) jedisSocketFactory)
            .getHostAndPort();
        HostAndPort objectAddress = obj.getConnection().getHostAndPort();

        targetHasNotChanged = targetAddress.getHost().equals(objectAddress.getHost())
            && targetAddress.getPort() == objectAddress.getPort();
      }

      return targetHasNotChanged && obj.getConnection().isConnected() && obj.ping().equals("PONG");
    } catch (final Exception e) {
      logger.warn("Error while validating Jedis object for borrow.", e);
      return false;
    }
  }

  @Override
  public boolean isObjectValid(Jedis obj) {
    try {
      boolean targetHasNotChanged = true;
      if (jedisSocketFactory instanceof DefaultJedisSocketFactory) {
        HostAndPort targetAddress = ((DefaultJedisSocketFactory) jedisSocketFactory)
            .getHostAndPort();
        HostAndPort objectAddress = obj.getConnection().getHostAndPort();

        targetHasNotChanged = targetAddress.getHost().equals(objectAddress.getHost())
            && targetAddress.getPort() == objectAddress.getPort();
      }

      return targetHasNotChanged && obj.getConnection().isConnected() && obj.ping().equals("PONG");
    } catch (final Exception e) {
      logger.warn("Error while validating Jedis object.", e);
      return false;
    }
  }

  @Override
  public void destroyObject(Jedis obj) {
    if (obj.isConnected()) {
      try {
        obj.close();
      } catch (RuntimeException e) {
        logger.debug("Error while closing", e);
      }
    }
  }
}
