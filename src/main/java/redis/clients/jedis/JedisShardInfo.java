package redis.clients.jedis;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.ShardInfo;
import redis.clients.jedis.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {

  private int connectionTimeout;
  private int soTimeout;
  private String host;
  private int port;
  private String user = null;
  private String password = null;
  private String name = null;
  // Default Redis DB
  private int db = 0;
  private boolean ssl;
  private SSLSocketFactory sslSocketFactory;
  private SSLParameters sslParameters;
  private HostnameVerifier hostnameVerifier;
  
  public JedisShardInfo(String host) {
    super(Sharded.DEFAULT_WEIGHT);
    URI uri = URI.create(host);
    if (JedisURIHelper.isValid(uri)) {
      this.host = uri.getHost();
      this.port = uri.getPort();
      this.user = JedisURIHelper.getUser(uri);
      this.password = JedisURIHelper.getPassword(uri);
      this.db = JedisURIHelper.getDBIndex(uri);
      this.ssl = JedisURIHelper.isRedisSSLScheme(uri);
    } else {
      this.host = host;
      this.port = Protocol.DEFAULT_PORT;
    }
  }

  public JedisShardInfo(String host, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
    this(host);
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  public JedisShardInfo(String host, String name) {
    this(host, Protocol.DEFAULT_PORT, name);
  }

  public JedisShardInfo(HostAndPort hp) {
    this(hp.getHost(), hp.getPort());
  }

  public JedisShardInfo(String host, int port) {
    this(host, port, Protocol.DEFAULT_TIMEOUT);
  }

  public JedisShardInfo(String host, int port, boolean ssl) {
    this(host, port, Protocol.DEFAULT_TIMEOUT, ssl);
  }

  public JedisShardInfo(String host, int port, boolean ssl, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
    this(host, port, Protocol.DEFAULT_TIMEOUT, ssl, sslSocketFactory, sslParameters,
        hostnameVerifier);
  }

  public JedisShardInfo(String host, int port, String name) {
    this(host, port, Protocol.DEFAULT_TIMEOUT, name);
  }

  public JedisShardInfo(String host, int port, String name, boolean ssl) {
    this(host, port, Protocol.DEFAULT_TIMEOUT, name, ssl);
  }

  public JedisShardInfo(String host, int port, String name, boolean ssl, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
    this(host, port, Protocol.DEFAULT_TIMEOUT, name, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public JedisShardInfo(String host, int port, int timeout) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT);
  }

  public JedisShardInfo(String host, int port, int timeout, boolean ssl) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT, ssl);
  }

  public JedisShardInfo(String host, int port, int timeout, boolean ssl,
      SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT, ssl, sslSocketFactory,
        sslParameters, hostnameVerifier);
  }

  public JedisShardInfo(String host, int port, int timeout, String name) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT);
    this.name = name;
  }

  public JedisShardInfo(String host, int port, int timeout, String name, boolean ssl) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT, ssl);
    this.name = name;
  }

  public JedisShardInfo(String host, int port, int timeout, String name, boolean ssl,
      SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    this.name = name;
  }

  public JedisShardInfo(String host, int port, int connectionTimeout, int soTimeout, int weight) {
    super(weight);
    this.host = host;
    this.port = port;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
  }

  public JedisShardInfo(String host, int port, int connectionTimeout, int soTimeout, int weight,
      boolean ssl) {
    super(weight);
    this.host = host;
    this.port = port;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.ssl = ssl;
  }

  public JedisShardInfo(String host, int port, int connectionTimeout, int soTimeout, int weight,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(host, port, connectionTimeout, soTimeout, weight, ssl);
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  public JedisShardInfo(String host, String name, int port, int timeout, int weight) {
    this(host, port, timeout, timeout, weight);
    this.name = name;
  }

  public JedisShardInfo(String host, String name, int port, int timeout, int weight, boolean ssl) {
    this(host, port, timeout, timeout, weight, ssl);
    this.name = name;
  }

  public JedisShardInfo(String host, String name, int port, int timeout, int weight,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, timeout, weight, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    this.name = name;
  }

  public JedisShardInfo(URI uri) {
    super(Sharded.DEFAULT_WEIGHT);
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }

    this.host = uri.getHost();
    this.port = uri.getPort();
    this.user = JedisURIHelper.getUser(uri);
    this.password = JedisURIHelper.getPassword(uri);
    this.db = JedisURIHelper.getDBIndex(uri);
    this.ssl = JedisURIHelper.isRedisSSLScheme(uri);
  }

  public JedisShardInfo(URI uri, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(uri);
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  @Override
  public String toString() {
    return host + ":" + port + "*" + getWeight();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String auth) {
    this.password = auth;
  }

  public String getUser() { return user; }

  public void setUser(String user) { this.user = user; }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getSoTimeout() {
    return soTimeout;
  }

  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }

  @Override
  public String getName() {
    return name;
  }

  public int getDb() {
    return db;
  }

  public boolean getSsl() {
      return ssl;
  }

  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }

  public SSLParameters getSslParameters() {
    return sslParameters;
  }

  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }

  @Override
  public Jedis createResource() {
    return new Jedis(this);
  }
  
}
