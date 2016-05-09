package redis.clients.jedis;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.util.JedisURIHelper;

public class JedisConnectionConfigBuilder {
  private String clientName;
  private URI uri;
  private String host;
  private int port = Protocol.DEFAULT_PORT;
  private String password;
  private int dbIndex = JedisURIHelper.DEFAULT_DB;
  private boolean ssl = false;
  private HostnameVerifier hostnameVerifier = null;
  private int connectTimeout = Protocol.DEFAULT_TIMEOUT;
  private int soTimeout = Protocol.DEFAULT_TIMEOUT;
  private int subscribeSoTimeout = Protocol.DEFAULT_SUBSCRIBE_TIMEOUT;
  private SSLParameters sslParameters = null;
  private SSLSocketFactory sslSocketFactory = null;

  public JedisConnectionConfigBuilder withCheckedUri(final URI uri) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(uri.toString() + " is not a valid redis URI");
    }
    return withUri(uri);
  }

  public JedisConnectionConfigBuilder withUri(URI uri) {
    this.uri = uri;
    return this;
  }

  public JedisConnectionConfigBuilder withConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public JedisConnectionConfigBuilder withSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
    return this;
  }

  public JedisConnectionConfigBuilder withSubscribeSoTimeout(int subscribeSoTimeout) {
    this.subscribeSoTimeout = subscribeSoTimeout;
    return this;
  }

  public JedisConnectionConfigBuilder withSslSocketFactory(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
    return this;
  }

  public JedisConnectionConfigBuilder withSslParameters(SSLParameters sslParameters) {
    this.sslParameters = sslParameters;
    return this;
  }

  public JedisConnectionConfigBuilder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
    return this;
  }

  public JedisConnectionConfigBuilder withClientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

  public JedisConnectionConfigBuilder withHost(final String host) {
    this.host = host;
    return this;
  }

  public JedisConnectionConfigBuilder withPort(final int port) {
    this.port = port;
    return this;
  }

  public JedisConnectionConfigBuilder withPassword(final String password) {
    this.password = password;
    return this;
  }

  public JedisConnectionConfigBuilder withDbIndex(final int dbIndex) {
    this.dbIndex = dbIndex;
    return this;
  }

  public JedisConnectionConfigBuilder withSsl(final boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  public JedisConnectionConfig build() {
    if (uri != null) {
      return new JedisConnectionConfig(
              uri.getHost(),
              uri.getPort(),
              JedisURIHelper.getPassword(uri),
              JedisURIHelper.getDBIndex(uri),
              "rediss".equals(uri.getScheme()),
              connectTimeout,
              soTimeout,
              subscribeSoTimeout,
              sslSocketFactory,
              sslParameters,
              clientName,
              hostnameVerifier);
    } else {
      return new JedisConnectionConfig(
              host,
              port,
              password,
              dbIndex,
              ssl,
              connectTimeout,
              soTimeout,
              subscribeSoTimeout,
              sslSocketFactory,
              sslParameters,
              clientName,
              hostnameVerifier);
    }
  }
}