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
  
  private final JedisSocketFactory jedisSocketFactory;
  
  private final String user;
  private final String password;
  private final int database;
  private final String clientName;

  JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, password, database, clientName,
        false, null, null, null);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName) {
    this(host, port, connectionTimeout, soTimeout, user, password, database, clientName,
            false, null, null, null);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, null, password, database, clientName,
            ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  JedisFactory(final String host, final int port, final int connectionTimeout,
               final int soTimeout, final String user, final String password, final int database, final String clientName,
               final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
               final HostnameVerifier hostnameVerifier) {
    this.jedisSocketFactory = new DefaultJedisSocketFactory(host, port, connectionTimeout,
        soTimeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);

    this.user = user;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
  }

  JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName) {
    this(uri, connectionTimeout, soTimeout, clientName, null, null, null);
  }

  JedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
      final String clientName, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }
    
    this.jedisSocketFactory = new DefaultJedisSocketFactory(uri.getHost(), uri.getPort(),
        connectionTimeout, soTimeout, JedisURIHelper.isRedisSSLScheme(uri), sslSocketFactory, sslParameters, hostnameVerifier);
    this.user = JedisURIHelper.getUser(uri);
    this.password = JedisURIHelper.getPassword(uri);
    this.database = JedisURIHelper.getDBIndex(uri);
    this.clientName = clientName;
  }

  JedisFactory(final JedisSocketFactory jedisSocketFactory, final String user,
      final String password, final int database, final String clientName) {
    this.jedisSocketFactory = jedisSocketFactory;
    this.user = user;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
  }
  
  public void setHostAndPort(final HostAndPort hostAndPort) {
    this.jedisSocketFactory.setHost(hostAndPort.getHost());
    this.jedisSocketFactory.setPort(hostAndPort.getPort());
  }

  @Override
  public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.getDB() != database) {
      jedis.select(database);
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
    final Jedis jedis = new Jedis(this.jedisSocketFactory);
    try {
      jedis.connect();
      if (user != null) {
        jedis.auth(user, password);
      } else if (password != null) {
        jedis.auth(password);
      }
      if (database != 0) {
        jedis.select(database);
      }
      if (clientName != null) {
        jedis.clientSetname(clientName);
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
      String host = this.jedisSocketFactory.getHost();
      int port = this.jedisSocketFactory.getPort();

      String connectionHost = jedis.getClient().getHost();
      int connectionPort = jedis.getClient().getPort();

      return host.equals(connectionHost)
          && port == connectionPort && jedis.isConnected()
          && jedis.ping().equals("PONG");
    } catch (final Exception e) {
      return false;
    }
  }
}