package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public final class DefaultJedisClientConfig implements JedisClientConfig {

  private final int connectionTimeoutMillis;
  private final int socketTimeoutMillis;
  private final int blockingSocketTimeoutMillis;

  private volatile Supplier<RedisCredentials> credentials;
  private final int database;
  private final String clientName;

  private final boolean ssl;
  private final SSLSocketFactory sslSocketFactory;
  private final SSLParameters sslParameters;
  private final HostnameVerifier hostnameVerifier;

  private final HostAndPortMapper hostAndPortMapper;

  private DefaultJedisClientConfig(int connectionTimeoutMillis, int soTimeoutMillis,
      int blockingSocketTimeoutMillis, Supplier<RedisCredentials> credentials, int database, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
    this.socketTimeoutMillis = soTimeoutMillis;
    this.blockingSocketTimeoutMillis = blockingSocketTimeoutMillis;
    this.credentials = credentials;
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
  public int getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  @Override
  public int getBlockingSocketTimeoutMillis() {
    return blockingSocketTimeoutMillis;
  }

  @Override
  public String getUser() {
    return credentials.get().getUser();
  }

  @Override
  public String getPassword() {
    return new String(credentials.get().getPassword());
  }

  @Override
  public Supplier<RedisCredentials> getCredentialsProvider() {
    return credentials;
  }

  @Override
  @Deprecated
  public synchronized void updatePassword(String password) {
    String user = getUser();
    ((DefaultRedisCredentialsProvider) this.credentials)
        .setCredentials(new DefaultRedisCredentials(user, password));
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
    private int socketTimeoutMillis = Protocol.DEFAULT_TIMEOUT;
    private int blockingSocketTimeoutMillis = 0;

    private String user = null;
    private String password = null;
    private Supplier<RedisCredentials> credentials;
    private int database = Protocol.DEFAULT_DATABASE;
    private String clientName = null;

    private boolean ssl = false;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLParameters sslParameters = null;
    private HostnameVerifier hostnameVerifier = null;

    private HostAndPortMapper hostAndPortMapper = null;

    private Builder() {
    }

    public DefaultJedisClientConfig build() {
      return new DefaultJedisClientConfig(connectionTimeoutMillis, socketTimeoutMillis,
          blockingSocketTimeoutMillis, credentials, database, clientName, ssl, sslSocketFactory,
          sslParameters, hostnameVerifier, hostAndPortMapper);
    }

    public Builder timeoutMillis(int timeoutMillis) {
      this.connectionTimeoutMillis = timeoutMillis;
      this.socketTimeoutMillis = timeoutMillis;
      return this;
    }

    public Builder connectionTimeoutMillis(int connectionTimeoutMillis) {
      this.connectionTimeoutMillis = connectionTimeoutMillis;
      return this;
    }

    public Builder socketTimeoutMillis(int socketTimeoutMillis) {
      this.socketTimeoutMillis = socketTimeoutMillis;
      return this;
    }

    public Builder blockingSocketTimeoutMillis(int blockingSocketTimeoutMillis) {
      this.blockingSocketTimeoutMillis = blockingSocketTimeoutMillis;
      return this;
    }

    /**
     * @deprecated Use {@link Builder#credentials(redis.clients.jedis.RedisCredentials)}.
     */
    @Deprecated
    public Builder user(String user) {
      this.user = user;
      return this;
    }

    /**
     * @deprecated Use {@link Builder#credentials(redis.clients.jedis.RedisCredentials)}.
     */
    @Deprecated
    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder credentials(RedisCredentials credentials) {
      this.credentials = new DefaultRedisCredentialsProvider(credentials);
      return this;
    }

    public Builder credentialsProvider(Supplier<RedisCredentials> credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder database(int database) {
      this.database = database;
      return this;
    }

    public Builder clientName(String clientName) {
      this.clientName = clientName;
      return this;
    }

    public Builder ssl(boolean ssl) {
      this.ssl = ssl;
      return this;
    }

    public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
      this.sslSocketFactory = sslSocketFactory;
      return this;
    }

    public Builder sslParameters(SSLParameters sslParameters) {
      this.sslParameters = sslParameters;
      return this;
    }

    public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
      this.hostnameVerifier = hostnameVerifier;
      return this;
    }

    public Builder hostAndPortMapper(HostAndPortMapper hostAndPortMapper) {
      this.hostAndPortMapper = hostAndPortMapper;
      return this;
    }
  }

  public static DefaultJedisClientConfig create(int connectionTimeoutMillis, int soTimeoutMillis,
      int blockingSocketTimeoutMillis, String user, String password, int database, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    return new DefaultJedisClientConfig(connectionTimeoutMillis, soTimeoutMillis, blockingSocketTimeoutMillis,
        new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(user, password)),
        database, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMapper);
  }

  public static DefaultJedisClientConfig copyConfig(JedisClientConfig copy) {
    return new DefaultJedisClientConfig(copy.getConnectionTimeoutMillis(),
        copy.getSocketTimeoutMillis(), copy.getBlockingSocketTimeoutMillis(),
        new DefaultRedisCredentialsProvider(
            new DefaultRedisCredentials(copy.getUser(), copy.getPassword())),
        copy.getDatabase(), copy.getClientName(), copy.isSsl(),
        copy.getSslSocketFactory(), copy.getSslParameters(),
        copy.getHostnameVerifier(), copy.getHostAndPortMapper());
  }
}
