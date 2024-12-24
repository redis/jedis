package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.authentication.AuthXManager;

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

  private final AuthXManager authXManager;

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
    this.authXManager = builder.authXManager;
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
  public AuthXManager getAuthXManager() {
    return authXManager;
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

    private AuthXManager authXManager = null;

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

    public Builder authXManager(AuthXManager authXManager) {
      this.authXManager = authXManager;
      return this;
    }

    public Builder from(JedisClientConfig instance) {
      this.redisProtocol = instance.getRedisProtocol();
      this.connectionTimeoutMillis = instance.getConnectionTimeoutMillis();
      this.socketTimeoutMillis = instance.getSocketTimeoutMillis();
      this.blockingSocketTimeoutMillis = instance.getBlockingSocketTimeoutMillis();
      this.credentialsProvider = instance.getCredentialsProvider();
      this.database = instance.getDatabase();
      this.clientName = instance.getClientName();
      this.ssl = instance.isSsl();
      this.sslSocketFactory = instance.getSslSocketFactory();
      this.sslParameters = instance.getSslParameters();
      this.sslOptions = instance.getSslOptions();
      this.hostnameVerifier = instance.getHostnameVerifier();
      this.hostAndPortMapper = instance.getHostAndPortMapper();
      this.clientSetInfoConfig = instance.getClientSetInfoConfig();
      this.readOnlyForRedisClusterReplicas = instance.isReadOnlyForRedisClusterReplicas();
      this.authXManager = instance.getAuthXManager();
      return this;
    }
  }

  /**
   * @deprecated Use {@link redis.clients.jedis.DefaultJedisClientConfig.Builder}.
   */
  @Deprecated
  public static DefaultJedisClientConfig create(int connectionTimeoutMillis, int soTimeoutMillis,
      int blockingSocketTimeoutMillis, String user, String password, int database, String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, HostAndPortMapper hostAndPortMapper) {
    Builder builder = builder();
    builder.connectionTimeoutMillis(connectionTimeoutMillis).socketTimeoutMillis(soTimeoutMillis)
        .blockingSocketTimeoutMillis(blockingSocketTimeoutMillis);
    if (user != null || password != null) {
      // deliberately not handling 'user != null && password == null' here
      builder.credentials(new DefaultRedisCredentials(user, password));
    }
    builder.database(database).clientName(clientName);
    builder.ssl(ssl).sslSocketFactory(sslSocketFactory).sslParameters(sslParameters).hostnameVerifier(hostnameVerifier);
    builder.hostAndPortMapper(hostAndPortMapper);
    return builder.build();
  }

  /**
   * @deprecated Use
   * {@link redis.clients.jedis.DefaultJedisClientConfig.Builder#from(redis.clients.jedis.JedisClientConfig)}.
   */
  @Deprecated
  public static DefaultJedisClientConfig copyConfig(JedisClientConfig copy) {
    Builder builder = builder();
    builder.protocol(copy.getRedisProtocol());
    builder.connectionTimeoutMillis(copy.getConnectionTimeoutMillis());
    builder.socketTimeoutMillis(copy.getSocketTimeoutMillis());
    builder.blockingSocketTimeoutMillis(copy.getBlockingSocketTimeoutMillis());

    Supplier<RedisCredentials> credentialsProvider = copy.getCredentialsProvider();
    if (credentialsProvider != null) {
      builder.credentialsProvider(credentialsProvider);
    } else {
      builder.user(copy.getUser());
      builder.password(copy.getPassword());
    }

    builder.database(copy.getDatabase());
    builder.clientName(copy.getClientName());

    builder.ssl(copy.isSsl());
    builder.sslSocketFactory(copy.getSslSocketFactory());
    builder.sslParameters(copy.getSslParameters());
    builder.hostnameVerifier(copy.getHostnameVerifier());
    builder.sslOptions(copy.getSslOptions());
    builder.hostAndPortMapper(copy.getHostAndPortMapper());

    builder.clientSetInfoConfig(copy.getClientSetInfoConfig());
    if (copy.isReadOnlyForRedisClusterReplicas()) {
      builder.readOnlyForRedisClusterReplicas();
    }

    builder.authXManager(copy.getAuthXManager());

    return builder.build();
  }
}
