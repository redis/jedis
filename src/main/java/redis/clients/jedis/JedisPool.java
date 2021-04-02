package redis.clients.jedis;

import java.net.URI;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.util.JedisURIHelper;

public class JedisPool extends JedisPoolAbstract {

  private static final Logger log = LoggerFactory.getLogger(JedisPool.class);

  public JedisPool() {
    this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host) {
    this(poolConfig, host, Protocol.DEFAULT_PORT);
  }

  public JedisPool(String host, int port) {
    this(new GenericObjectPoolConfig<Jedis>(), host, port);
  }

  /**
   * @param url
   * @deprecated This constructor will not accept a host string in future. It will accept only a uri
   *             string. You can use {@link JedisURIHelper#isValid(java.net.URI)} before this.
   */
  @Deprecated
  public JedisPool(final String url) {
    URI uri = URI.create(url);
    if (JedisURIHelper.isValid(uri)) {
      initPool(new GenericObjectPoolConfig<Jedis>(),
        new JedisFactory(uri, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null));
    } else {
      initPool(new GenericObjectPoolConfig<Jedis>(),
        new JedisFactory(url, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
            Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null));
    }
  }

  /**
   * @param url
   * @param sslSocketFactory
   * @param sslParameters
   * @param hostnameVerifier
   * @deprecated This constructor will not accept a host string in future. It will accept only a uri
   *             string. You can use {@link JedisURIHelper#isValid(java.net.URI)} before this.
   */
  @Deprecated
  public JedisPool(final String url, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    URI uri = URI.create(url);
    if (JedisURIHelper.isValid(uri)) {
      initPool(new GenericObjectPoolConfig<Jedis>(), new JedisFactory(uri, Protocol.DEFAULT_TIMEOUT,
          Protocol.DEFAULT_TIMEOUT, null, sslSocketFactory, sslParameters, hostnameVerifier));
    } else {
      initPool(new GenericObjectPoolConfig<Jedis>(),
        new JedisFactory(url, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
            Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null, false, null, null,
            null));
    }
  }

  public JedisPool(final URI uri) {
    this(new GenericObjectPoolConfig<Jedis>(), uri);
  }

  public JedisPool(final URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(new GenericObjectPoolConfig<Jedis>(), uri, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final URI uri, final int timeout) {
    this(new GenericObjectPoolConfig<Jedis>(), uri, timeout);
  }

  public JedisPool(final URI uri, final int timeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(new GenericObjectPoolConfig<Jedis>(), uri, timeout, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisPool(final String host, int port, String user, final String password) {
    this(new GenericObjectPoolConfig<Jedis>(), host, port, user, password);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      String user, final String password) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, user, password,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password) {
    this(poolConfig, host, port, timeout, user, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final boolean ssl) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password, final boolean ssl) {
    this(poolConfig, host, port, timeout, user, password, Protocol.DEFAULT_DATABASE, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final boolean ssl) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, ssl, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final int timeout) {
    this(poolConfig, host, port, timeout, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final int timeout, final boolean ssl) {
    this(poolConfig, host, port, timeout, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final int timeout, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, null, ssl, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database) {
    this(poolConfig, host, port, timeout, password, database, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password, final int database) {
    this(poolConfig, host, port, timeout, user, password, database, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database, final boolean ssl) {
    this(poolConfig, host, port, timeout, password, database, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password, final int database,
      final boolean ssl) {
    this(poolConfig, host, port, timeout, user, password, database, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, password, database, null, ssl, sslSocketFactory,
        sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password, final int database,
      final String clientName) {
    this(poolConfig, host, port, timeout, timeout, user, password, database, clientName);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName,
      final boolean ssl) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String user, final String password, final int database,
      final String clientName, final boolean ssl) {
    this(poolConfig, host, port, timeout, timeout, user, password, database, clientName, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    super(poolConfig, new JedisFactory(host, port, connectionTimeout, soTimeout, password, database,
        clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String password, final int database, final String clientName, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, connectionTimeout, soTimeout, infiniteSoTimeout, null, password,
        database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final String user, final String password,
      final int database, final String clientName, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, connectionTimeout, soTimeout, 0, user, password, database,
        clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    super(poolConfig,
        new JedisFactory(host, port, connectionTimeout, soTimeout, infiniteSoTimeout, user,
            password, database, clientName, ssl, sslSocketFactory, sslParameters,
            hostnameVerifier));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final HostAndPort hostAndPort,
      final JedisClientConfig clientConfig) {
    super(poolConfig, new JedisFactory(hostAndPort, clientConfig));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig,
      final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    super(poolConfig, new JedisFactory(jedisSocketFactory, clientConfig));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig) {
    this(poolConfig, Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  public JedisPool(final String host, final int port, final boolean ssl) {
    this(new GenericObjectPoolConfig<Jedis>(), host, port, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final String password, final int database,
      final String clientName) {
    super(poolConfig,
        new JedisFactory(host, port, connectionTimeout, soTimeout, password, database, clientName));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final String user, final String password,
      final int database, final String clientName) {
    super(poolConfig, new JedisFactory(host, port, connectionTimeout, soTimeout, user, password,
        database, clientName));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName) {
    super(poolConfig, new JedisFactory(host, port, connectionTimeout, soTimeout, infiniteSoTimeout,
        user, password, database, clientName));
  }

  public JedisPool(final String host, final int port, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(new GenericObjectPoolConfig<Jedis>(), host, port, ssl, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final int connectionTimeout, final int soTimeout, final String password,
      final int database, final String clientName, final boolean ssl) {
    this(poolConfig, host, port, connectionTimeout, soTimeout, password, database, clientName, ssl,
        null, null, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final String host,
      final int port, final int connectionTimeout, final int soTimeout, final String user,
      final String password, final int database, final String clientName, final boolean ssl) {
    this(poolConfig, host, port, connectionTimeout, soTimeout, user, password, database, clientName,
        ssl, null, null, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri) {
    this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final int timeout) {
    this(poolConfig, uri, timeout, timeout);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final int timeout, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, uri, timeout, timeout, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final int connectionTimeout, final int soTimeout) {
    this(poolConfig, uri, connectionTimeout, soTimeout, null, null, null);
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final int connectionTimeout, final int soTimeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    super(poolConfig, new JedisFactory(uri, connectionTimeout, soTimeout, null, sslSocketFactory,
        sslParameters, hostnameVerifier));
  }

  public JedisPool(final GenericObjectPoolConfig<Jedis> poolConfig, final URI uri,
      final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    super(poolConfig, new JedisFactory(uri, connectionTimeout, soTimeout, infiniteSoTimeout, null,
        sslSocketFactory, sslParameters, hostnameVerifier));
  }

  public JedisPool(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }

  @Override
  public Jedis getResource() {
    Jedis jedis = super.getResource();
    jedis.setDataSource(this);
    return jedis;
  }

  @Override
  public void returnResource(final Jedis resource) {
    if (resource != null) {
      try {
        resource.resetState();
        returnResourceObject(resource);
      } catch (Exception e) {
        returnBrokenResource(resource);
        log.warn("Resource is returned to the pool as broken", e);
      }
    }
  }
}
