package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public final class DefaultJedisClientConfig implements JedisClientConfig {

  private final RedisProtocol redisProtocol;

  private final int connectionTimeoutMillis;
  private final int socketTimeoutMillis;
  private final int blockingSocketTimeoutMillis;

  private volatile Supplier<RedisCredentials> credentialsProvider;
  private final int database;
  private final String clientName;

  private final boolean ssl;
  private final SSLSocketFactory sslSocketFactory;
  private final SSLParameters sslParameters;
  private final SslOptions sslOptions;
  private final HostnameVerifier hostnameVerifier;

  private final HostAndPortMapper hostAndPortMapper;

  private final ClientSetInfoConfig clientSetInfoConfig;

  private final boolean readOnlyForRedisClusterReplicas;

  // TODO: how many constructors should we have? 1) all params, 2) builder 3) another config
  private DefaultJedisClientConfig(RedisProtocol protocol, int connectionTimeoutMillis, int soTimeoutMillis,
      int blockingSocketTimeoutMillis, Supplier<RedisCredentials> credentialsProvider, int database,
      String clientName, boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper,
      ClientSetInfoConfig clientSetInfoConfig, boolean readOnlyForRedisClusterReplicas) {
    this.redisProtocol = protocol;
    this.connectionTimeoutMillis = connectionTimeoutMillis;
    this.socketTimeoutMillis = soTimeoutMillis;
    this.blockingSocketTimeoutMillis = blockingSocketTimeoutMillis;
    this.credentialsProvider = credentialsProvider;
    this.database = database;
    this.clientName = clientName;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.sslOptions = null; // TODO:
    this.hostnameVerifier = hostnameVerifier;
    this.hostAndPortMapper = hostAndPortMapper;
    this.clientSetInfoConfig = clientSetInfoConfig;
    this.readOnlyForRedisClusterReplicas = readOnlyForRedisClusterReplicas;
  }

  private DefaultJedisClientConfig(DefaultJedisClientConfig.Builder builder) {
    this.redisProtocol = builder.redisProtocol;
    this.connectionTimeoutMillis = builder.connectionTimeoutMillis;
    this.socketTimeoutMillis = builder.socketTimeoutMillis;
    this.blockingSocketTimeoutMillis = builder.blockingSocketTimeoutMillis;
    this.credentialsProvider = builder.credentialsProvider;
    this.database = builder.database;
    this.clientName = builder.clientName;
    this.ssl = builder.ssl;
    this.sslSocketFactory = builder.sslSocketFactory;
    this.sslParameters = builder.sslParameters;
    this.sslOptions = builder.sslOptions;
    this.hostnameVerifier = builder.hostnameVerifier;
    this.hostAndPortMapper = builder.hostAndPortMapper;
    this.clientSetInfoConfig = builder.clientSetInfoConfig;
    this.readOnlyForRedisClusterReplicas = builder.readOnlyForRedisClusterReplicas;
  }

  @Override
  public RedisProtocol getRedisProtocol() {
    return redisProtocol;
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
    return credentialsProvider.get().getUser();
  }

  @Override
  public String getPassword() {
    char[] password = credentialsProvider.get().getPassword();
    return password == null ? null : new String(password);
  }

  @Override
  public Supplier<RedisCredentials> getCredentialsProvider() {
    return credentialsProvider;
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
  public SslOptions getSslOptions() {
    return sslOptions;
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }

  @Override
  public HostAndPortMapper getHostAndPortMapper() {
    return hostAndPortMapper;
  }

  @Override
  public ClientSetInfoConfig getClientSetInfoConfig() {
    return clientSetInfoConfig;
  }

  @Override
  public boolean isReadOnlyForRedisClusterReplicas() {
    return readOnlyForRedisClusterReplicas;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private RedisProtocol redisProtocol = null;

    private int connectionTimeoutMillis = Protocol.DEFAULT_TIMEOUT;
    private int socketTimeoutMillis = Protocol.DEFAULT_TIMEOUT;
    private int blockingSocketTimeoutMillis = 0;

    private String user = null;
    private String password = null;
    private Supplier<RedisCredentials> credentialsProvider;
    private int database = Protocol.DEFAULT_DATABASE;
    private String clientName = null;

    private boolean ssl = false;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLParameters sslParameters = null;
    private SslOptions sslOptions = null;
    private HostnameVerifier hostnameVerifier = null;

    private HostAndPortMapper hostAndPortMapper = null;

    private ClientSetInfoConfig clientSetInfoConfig = ClientSetInfoConfig.DEFAULT;

    private boolean readOnlyForRedisClusterReplicas = false;

    private Builder() {
    }

    public DefaultJedisClientConfig build() {
      if (credentialsProvider == null) {
        credentialsProvider = new DefaultRedisCredentialsProvider(
            new DefaultRedisCredentials(user, password));
      }

      return new DefaultJedisClientConfig(this);
    }

    /**
     * Shortcut to {@link redis.clients.jedis.DefaultJedisClientConfig.Builder#protocol(RedisProtocol)} with
     * {@link RedisProtocol#RESP3}.
     * @return this
     */
    public Builder resp3() {
      return protocol(RedisProtocol.RESP3);
    }

    public Builder protocol(RedisProtocol protocol) {
      this.redisProtocol = protocol;
      return this;
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

    public Builder user(String user) {
      this.user = user;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder credentials(RedisCredentials credentials) {
      this.credentialsProvider = new DefaultRedisCredentialsProvider(credentials);
      return this;
    }

    public Builder credentialsProvider(Supplier<RedisCredentials> credentials) {
      this.credentialsProvider = credentials;
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

    public Builder sslOptions(SslOptions sslOptions) {
      this.sslOptions = sslOptions;
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

    public Builder clientSetInfoConfig(ClientSetInfoConfig setInfoConfig) {
      this.clientSetInfoConfig = setInfoConfig;
      return this;
    }

    public Builder readOnlyForRedisClusterReplicas() {
      this.readOnlyForRedisClusterReplicas = true;
      return this;
    }
  }

  // TOOD: depends on which constructors we're going to have/keep
  public static DefaultJedisClientConfig create(int connectionTimeoutMillis, int soTimeoutMillis,
      int blockingSocketTimeoutMillis, String user, String password, int database, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    return new DefaultJedisClientConfig(null,
        connectionTimeoutMillis, soTimeoutMillis, blockingSocketTimeoutMillis,
        new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(user, password)), database,
        clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMapper, null,
        false);
  }

  // TOOD: depends on which constructors we're going to have/keep
  public static DefaultJedisClientConfig copyConfig(JedisClientConfig copy) {
    return new DefaultJedisClientConfig(copy.getRedisProtocol(),
        copy.getConnectionTimeoutMillis(), copy.getSocketTimeoutMillis(),
        copy.getBlockingSocketTimeoutMillis(), copy.getCredentialsProvider(),
        copy.getDatabase(), copy.getClientName(), copy.isSsl(), copy.getSslSocketFactory(),
        copy.getSslParameters(), copy.getHostnameVerifier(), copy.getHostAndPortMapper(),
        copy.getClientSetInfoConfig(), copy.isReadOnlyForRedisClusterReplicas());
  }
}
