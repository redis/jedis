package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public class DefaultJedisSocketConfig implements JedisSocketConfig {

  private final int connectionTimeout;
  private final int soTimeout;

  private final boolean ssl;
  private final SSLSocketFactory sslSocketFactory;
  private final SSLParameters sslParameters;
  private final HostnameVerifier hostnameVerifier;

  private final HostAndPortMapper hostAndPortMapper;

  private DefaultJedisSocketConfig(int connectionTimeout, int soTimeout, boolean ssl,
      SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
    this.hostAndPortMapper = hostAndPortMapper;
  }

  @Override
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public int getSoTimeout() {
    return soTimeout;
  }

  @Override
  public boolean isSSL() {
    return ssl;
  }

  @Override
  public SSLSocketFactory getSSLSocketFactory() {
    return sslSocketFactory;
  }

  @Override
  public SSLParameters getSSLParameters() {
    return sslParameters;
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }

  @Override
  public HostAndPortMapper getHostAndPortMapper() {
    return hostAndPortMapper;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;

    private boolean ssl = false;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLParameters sslParameters = null;
    private HostnameVerifier hostnameVerifier = null;

    private HostAndPortMapper hostAndPortMapper = null;

    private Builder() {
    }

    public DefaultJedisSocketConfig build() {
      return new DefaultJedisSocketConfig(connectionTimeout, soTimeout, ssl,
          sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMapper);
    }

    public Builder withTimeout(int timeout) {
      return withConnectionTimeout(timeout).withSoTimeout(timeout);
    }

    public Builder withConnectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder withSoTimeout(int soTimeout) {
      this.soTimeout = soTimeout;
      return this;
    }

    public Builder withSsl(boolean ssl) {
      this.ssl = ssl;
      return this;
    }

    public Builder withSslSocketFactory(SSLSocketFactory sslSocketFactory) {
      this.sslSocketFactory = sslSocketFactory;
      return this;
    }

    public Builder withSslParameters(SSLParameters sslParameters) {
      this.sslParameters = sslParameters;
      return this;
    }

    public Builder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
      this.hostnameVerifier = hostnameVerifier;
      return this;
    }

    public Builder withHostAndPortMapper(HostAndPortMapper hostAndPortMapper) {
      this.hostAndPortMapper = hostAndPortMapper;
      return this;
    }

    public int getConnectionTimeout() {
      return connectionTimeout;
    }

    public int getSoTimeout() {
      return soTimeout;
    }

    public boolean isSsl() {
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

    public HostAndPortMapper getHostAndPortMapper() {
      return hostAndPortMapper;
    }
  }

  static DefaultJedisSocketConfig withSsl(boolean ssl, JedisSocketConfig copy) {
    return new DefaultJedisSocketConfig(copy.getConnectionTimeout(), copy.getSoTimeout(),
        ssl, copy.getSSLSocketFactory(), copy.getSSLParameters(), copy.getHostnameVerifier(),
        copy.getHostAndPortMapper());
  }
}
