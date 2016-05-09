package redis.clients.jedis;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.JedisURIHelper;

public class JedisPool extends JedisPoolAbstract {

  public JedisPool() {
    this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host) {
    this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, null);
  }

  public JedisPool(String host, int port) {
    this(new GenericObjectPoolConfig(), host, port, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, null);
  }

  public JedisPool(final JedisPoolConfig poolConfig, final JedisConnectionConfig connectionConfig) {
    final JedisFactory jedisFactory = new JedisFactory(connectionConfig.getHost(),
        connectionConfig.getPort(), connectionConfig.getConnectTimeout(),
        connectionConfig.getSoTimeout(), connectionConfig.getSubscribeSoTimeout(),
        connectionConfig.getPassword(), connectionConfig.getDbIndex(),
        connectionConfig.getClientName(), connectionConfig.isSsl(),
        connectionConfig.getSslSocketFactory(), connectionConfig.getSslParameters(),
        connectionConfig.getHostnameVerifier());
    this.internalPool = new GenericObjectPool<Jedis>(jedisFactory, poolConfig);
  }

  public JedisPool(final String host) {
    URI uri = URI.create(host);
    final JedisConnectionConfig connectionConfig;
    if (JedisURIHelper.isValid(uri)) {
      connectionConfig = new JedisConnectionConfigBuilder().withUri(uri).build();
    } else {
      connectionConfig = new JedisConnectionConfigBuilder().withHost(host).build();
    }
    this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(connectionConfig),
        new GenericObjectPoolConfig());
  }

  public JedisPool(final String host, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    URI uri = URI.create(host);
    final JedisConnectionConfig connectionConfig;
    if (JedisURIHelper.isValid(uri)) {
      connectionConfig = new JedisConnectionConfigBuilder().withUri(uri)
          .withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
          .withHostnameVerifier(hostnameVerifier).build();
    } else {
      connectionConfig = new JedisConnectionConfigBuilder().withHost(host)
          .withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
          .withHostnameVerifier(hostnameVerifier).build();
    }
    this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(connectionConfig),
        new GenericObjectPoolConfig());
  }

  public JedisPool(final URI uri) {
    this(new GenericObjectPoolConfig(), uri, Protocol.DEFAULT_TIMEOUT);
  }

  public JedisPool(final URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(new GenericObjectPoolConfig(), uri, Protocol.DEFAULT_TIMEOUT, sslSocketFactory,
        sslParameters, hostnameVerifier);
  }

  public JedisPool(final URI uri, final int timeout) {
    this(new GenericObjectPoolConfig(), uri, timeout);
  }

  public JedisPool(final URI uri, final int timeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(new GenericObjectPoolConfig(), uri, timeout, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final boolean ssl) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, null, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
      final boolean ssl) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null,
        ssl);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
      final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null,
        ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
      final int timeout) {
    this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE, null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
      final int timeout, final boolean ssl) {
    this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
      final int timeout, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE, null, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database) {
    this(poolConfig, host, port, timeout, password, database, null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database, final boolean ssl) {
    this(poolConfig, host, port, timeout, password, database, null, ssl);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, password, database, null, ssl, sslSocketFactory,
        sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName, false, null,
        null, null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName,
      final boolean ssl) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName, ssl, null, null,
        null);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      int timeout, final String password, final int database, final String clientName,
      final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(poolConfig, host, port, timeout, timeout, password, database, clientName, ssl,
        sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
      final int connectionTimeout, final int soTimeout, final String password, final int database,
      final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    super(poolConfig, new JedisFactory(new JedisConnectionConfigBuilder().withHost(host)
        .withPort(port).withConnectTimeout(connectionTimeout).withSoTimeout(soTimeout)
        .withPassword(password).withDbIndex(database).withClientName(clientName).withSsl(ssl)
        .withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
        .withHostnameVerifier(hostnameVerifier).build()));
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri) {
    this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri, final int timeout) {
    this(poolConfig, uri, timeout, timeout);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri, final int timeout,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(poolConfig, uri, timeout, timeout, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri,
      final int connectionTimeout, final int soTimeout) {
    super(poolConfig, new JedisFactory(new JedisConnectionConfigBuilder().withCheckedUri(uri)
        .withConnectTimeout(connectionTimeout).withSoTimeout(soTimeout).build()));
  }

  public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri,
      final int connectionTimeout, final int soTimeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    super(poolConfig, new JedisFactory(new JedisConnectionConfigBuilder().withCheckedUri(uri)
        .withConnectTimeout(connectionTimeout).withSoTimeout(soTimeout)
        .withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
        .withHostnameVerifier(hostnameVerifier).build()));
  }

  @Override
  public Jedis getResource() {
    Jedis jedis = super.getResource();
    jedis.setDataSource(this);
    return jedis;
  }

  @Override
  protected void returnBrokenResource(final Jedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  @Override
  protected void returnResource(final Jedis resource) {
    if (resource != null) {
      try {
        resource.resetState();
        returnResourceObject(resource);
      } catch (Exception e) {
        returnBrokenResource(resource);
        throw new JedisException("Could not return the resource to the pool", e);
      }
    }
  }
}
