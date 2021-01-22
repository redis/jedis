package redis.clients.jedis;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * PoolableObjectFactory custom impl.
 */
class JedisFactory implements PooledObjectFactory<Jedis> {

  private final AtomicReference<HostAndPort> hostAndPort = new AtomicReference<>();

  private final JedisClientConfig clientConfig;

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
    this.clientConfig = clientConfig;
  }

  JedisFactory(final String host, final int port, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final String user, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(new HostAndPort(host, port),
        DefaultJedisClientConfig.builder().withConnectionTimeout(connectionTimeout)
            .withSoTimeout(soTimeout).withInfiniteSoTimeout(infiniteSoTimeout)
            .withUser(user).withPassword(password).withDatabse(database).withClinetName(clientName)
            .withSsl(ssl).withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
            .withHostnameVerifier(hostnameVerifier).build()
    );
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
    this.clientConfig = DefaultJedisClientConfig.builder().withConnectionTimeout(connectionTimeout)
        .withSoTimeout(soTimeout).withInfiniteSoTimeout(infiniteSoTimeout)
        .withUser(JedisURIHelper.getUser(uri)).withPassword(JedisURIHelper.getPassword(uri))
        .withDatabse(JedisURIHelper.getDBIndex(uri)).withClinetName(clientName)
        .withSsl(JedisURIHelper.isRedisSSLScheme(uri)).withSslSocketFactory(sslSocketFactory)
        .withSslParameters(sslParameters).withHostnameVerifier(hostnameVerifier).build();
  }

  public void setHostAndPort(final HostAndPort hostAndPort) {
    this.hostAndPort.set(hostAndPort);
  }

  @Override
  public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.getDB() != clientConfig.getDatabase()) {
      jedis.select(clientConfig.getDatabase());
    }
  }

  @Override
  public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.isConnected()) {
      try {
        try {
          jedis.quit();
        } catch (Exception e) {
        }
        jedis.disconnect();
      } catch (Exception e) {
      }
    }
  }

  @Override
  public PooledObject<Jedis> makeObject() throws Exception {
    final HostAndPort hostPort = this.hostAndPort.get();
    final Jedis jedis = new Jedis(hostPort, (JedisSocketConfig) clientConfig, clientConfig.getInfiniteSoTimeout());

    try {
      jedis.connect();
      if (clientConfig.getUser() != null) {
        jedis.auth(clientConfig.getUser(), clientConfig.getPassword());
      } else if (clientConfig.getPassword() != null) {
        jedis.auth(clientConfig.getPassword());
      }
      if (clientConfig.getDatabase() != 0) {
        jedis.select(clientConfig.getDatabase());
      }
      if (clientConfig.getClientName() != null) {
        jedis.clientSetname(clientConfig.getClientName());
      }
    } catch (JedisException je) {
      jedis.close();
      throw je;
    }

    return new DefaultPooledObject<>(jedis);
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