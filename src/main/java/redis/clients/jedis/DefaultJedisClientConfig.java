package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public final class DefaultJedisClientConfig implements JedisClientConfig {

  private final int connectionTimeoutMillis;
  private final int soTimeoutMillis;
  private final int infiniteSoTimeoutMillis;

  private final String user;
  private final String password;
  private final int database;
  private final String clientName;

  private final boolean ssl;
  private final SSLSocketFactory sslSocketFactory;
  private final SSLParameters sslParameters;
  private final HostnameVerifier hostnameVerifier;

  private final HostAndPortMapper hostAndPortMapper;

  private DefaultJedisClientConfig(int connectionTimeoutMillis, int soTimeoutMillis,
      int infiniteSoTimeoutMillis, String user, String password, int database, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
    this.soTimeoutMillis = soTimeoutMillis;
    this.infiniteSoTimeoutMillis = infiniteSoTimeoutMillis;
    this.user = user;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
    this.hostAndPortMapper = hostAndPortMapper;
  }

  @Override
  public int getConnectionTimeoutMillis() {
    return connectionTimeoutMillis;
  }

  @Override
  public int getSoTimeoutMillis() {
    return soTimeoutMillis;
  }

  @Override
  public int getInfiniteSoTimeoutMillis() {
    return infiniteSoTimeoutMillis;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public int getDatabase() {
    return database;
  }

  @Override
  public String getClientName() {
    return clientName;
  }

  @Override
  public boolean isSsl() {
    return ssl;
  }

  @Override
  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }

  @Override
  public SSLParameters getSslParameters() {
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

    private int connectionTimeoutMillis = Protocol.DEFAULT_TIMEOUT;
    private int soTimeoutMillis = Protocol.DEFAULT_TIMEOUT;
    private int infiniteSoTimeoutMillis = 0;

    private String user = null;
    private String password = null;
    private int databse = Protocol.DEFAULT_DATABASE;
    private String clientName = null;

    private boolean ssl = false;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLParameters sslParameters = null;
    private HostnameVerifier hostnameVerifier = null;

    private HostAndPortMapper hostAndPortMapper = null;

    private Builder() {
    }

    public DefaultJedisClientConfig build() {
      return new DefaultJedisClientConfig(connectionTimeoutMillis, soTimeoutMillis,
          infiniteSoTimeoutMillis, user, password, databse, clientName,
          ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMapper);
    }

    public Builder withConnectionTimeoutMillis(int connectionTimeoutMillis) {
      this.connectionTimeoutMillis = connectionTimeoutMillis;
      return this;
    }

    public Builder withSoTimeoutMillis(int soTimeoutMillis) {
      this.soTimeoutMillis = soTimeoutMillis;
      return this;
    }

    public Builder withInfiniteSoTimeoutMillis(int infiniteSoTimeoutMillis) {
      this.infiniteSoTimeoutMillis = infiniteSoTimeoutMillis;
      return this;
    }

    public Builder withUser(String user) {
      this.user = user;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withDatabse(int databse) {
      this.databse = databse;
      return this;
    }

    public Builder withClientName(String clientName) {
      this.clientName = clientName;
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
  }
  
  public static DefaultJedisClientConfig copyConfig(JedisClientConfig copy) {
    return new DefaultJedisClientConfig(copy.getConnectionTimeoutMillis(), copy.getSoTimeoutMillis(),
        copy.getInfiniteSoTimeoutMillis(), copy.getUser(), copy.getPassword(), copy.getDatabase(),
        copy.getClientName(), copy.isSsl(), copy.getSslSocketFactory(), copy.getSslParameters(),
        copy.getHostnameVerifier(), copy.getHostAndPortMapper());
  }
}
