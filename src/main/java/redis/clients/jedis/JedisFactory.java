package redis.clients.jedis;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

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
class JedisFactory implements PooledObjectFactory<Jedis> {

  private static final Logger logger = LoggerFactory.getLogger(JedisFactory.class);

  private final AtomicReference<HostAndPort> hostAndPort = new AtomicReference<>();

  private final JedisClientConfig config;

  JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, password, database, clientName, false, null, null, null);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String user, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName, false, null, null, null);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, null, password, database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName,
               final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
               final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, 0, user, password, database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  JedisFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this.hostAndPort.set(hostAndPort);
    this.config = DefaultJedisClientConfig.copyConfig(clientConfig);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String user, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this.hostAndPort.set(new HostAndPort(host, port));
    this.config = DefaultJedisClientConfig.builder().withConnectionTimeoutMillis(connectionTimeout)
        .withSoTimeoutMillis(soTimeout).withInfiniteSoTimeoutMillis(infiniteSoTimeout).withUser(user)
        .withPassword(password).withDatabse(database).withClientName(clientName)
        .withSsl(ssl).withSslSocketFactory(sslSocketFactory)
        .withSslParameters(sslParameters).withHostnameVerifier(hostnameVerifier).build();
  }

  JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName) {
    this(uri, connectionTimeout, soTimeout, clientName, null, null, null);
  }

  JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, connectionTimeout, soTimeout, 0, clientName, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String clientName, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
          "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }
    this.hostAndPort.set(new HostAndPort(uri.getHost(), uri.getPort()));
    this.config = DefaultJedisClientConfig.builder().withConnectionTimeoutMillis(connectionTimeout)
        .withSoTimeoutMillis(soTimeout).withInfiniteSoTimeoutMillis(infiniteSoTimeout)
        .withUser(JedisURIHelper.getUser(uri)).withPassword(JedisURIHelper.getPassword(uri))
        .withDatabse(JedisURIHelper.getDBIndex(uri)).withClientName(clientName)
        .withSsl(JedisURIHelper.isRedisSSLScheme(uri)).withSslSocketFactory(sslSocketFactory)
        .withSslParameters(sslParameters).withHostnameVerifier(hostnameVerifier).build();
  }

  public void setHostAndPort(final HostAndPort hostAndPort) {
    this.hostAndPort.set(hostAndPort);
  }

  @Override
  public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.getDB() != config.getDatabase()) {
      jedis.select(config.getDatabase());
    }
  }

  @Override
  public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.isConnected()) {
      try {
        // need a proper test, probably with mock
        if (!jedis.isBroken()) {
          jedis.quit();
        }
      } catch (Exception e) {
        logger.warn("Error while QUIT", e);
      }
      try {
        jedis.close();
      } catch (Exception e) {
        logger.warn("Error while close", e);
      }
    }
  }

  @Override
  public PooledObject<Jedis> makeObject() throws Exception {
    final HostAndPort hostPort = this.hostAndPort.get();
    Jedis jedis = null;
    try {
      jedis = new Jedis(hostPort, config);
      jedis.connect();
      return new DefaultPooledObject<>(jedis);
    } catch (JedisException je) {
      if (jedis != null) {
        try {
          jedis.quit();
        } catch (Exception e) {
          logger.warn("Error while QUIT", e);
        }
        try {
          jedis.close();
        } catch (Exception e) {
          logger.warn("Error while close", e);
        }
      }
      throw je;
    }
  }

  @Override
  public void passivateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
  }

  @Override
  public boolean validateObject(PooledObject<Jedis> pooledJedis) {
    final BinaryJedis jedis = pooledJedis.getObject();
    try {
      HostAndPort hostAndPort = this.hostAndPort.get();

      String connectionHost = jedis.getClient().getHost();
      int connectionPort = jedis.getClient().getPort();

      return hostAndPort.getHost().equals(connectionHost)
          && hostAndPort.getPort() == connectionPort && jedis.isConnected()
          && jedis.ping().equals("PONG");
    } catch (final Exception e) {
      return false;
    }
  }
}
