package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.SENTINEL;
import static redis.clients.jedis.Protocol.SentinelKeyword.*;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.SentinelCommands;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.SafeEncoder;

public class Sentinel implements SentinelCommands, Closeable {

  protected final Connection connection;
  protected static final byte[][] DUMMY_ARRAY = new byte[0][];

  /**
   * @deprecated This constructor will not support a host string in future. It will accept only a
   * uri string. {@link JedisURIHelper#isValid(java.net.URI)} can used before this. If this
   * constructor was being used with a host, it can be replaced with
   * {@link #BinaryJedis(java.lang.String, int)} with the host and {@link Protocol#DEFAULT_PORT}.
   * @param uriString
   */
  @Deprecated
  public Sentinel(final String uriString) {
//    URI uri = URI.create(uriString);
//    if (JedisURIHelper.isValid(uri)) {
//      connection = createClientFromURI(uri);
//      initializeFromURI(uri);
//    } else {
//      throw new InvalidURIException(uriString);
//    }
    this(URI.create(uriString));
  }

  public Sentinel(final HostAndPort hp) {
    connection = new Connection(hp);
  }

  public Sentinel(final String host, final int port) {
    connection = new Connection(host, port);
  }

  public Sentinel(final String host, final int port, final JedisClientConfig config) {
    this(new HostAndPort(host, port), config);
  }

  public Sentinel(final HostAndPort hostPort, final JedisClientConfig config) {
    connection = new Connection(hostPort, config);
//    initializeFromClientConfig(config);
  }

  public Sentinel(final String host, final int port, final boolean ssl) {
    this(host, port, DefaultJedisClientConfig.builder().ssl(ssl).build());
  }

  public Sentinel(final String host, final int port, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder().ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(final String host, final int port, final int timeout) {
    this(host, port, timeout, timeout);
  }

  public Sentinel(final String host, final int port, final int timeout, final boolean ssl) {
    this(host, port, timeout, timeout, ssl);
  }

  public Sentinel(final String host, final int port, final int timeout, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, timeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public Sentinel(final String host, final int port, final int connectionTimeout,
      final int soTimeout) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).build());
  }

  public Sentinel(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).build());
  }

  public Sentinel(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).ssl(ssl)
        .build());
  }

  public Sentinel(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(URI uri) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
          "Cannot open Redis connection due invalid URI \"%s\".", uri.toString()));
    }
    connection = new Connection(new HostAndPort(uri.getHost(), uri.getPort()),
        DefaultJedisClientConfig.builder().user(JedisURIHelper.getUser(uri))
            .password(JedisURIHelper.getPassword(uri)).database(JedisURIHelper.getDBIndex(uri))
            .ssl(JedisURIHelper.isRedisSSLScheme(uri)).build());
  }

  public Sentinel(URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(final URI uri, final int timeout) {
    this(uri, timeout, timeout);
  }

  public Sentinel(final URI uri, final int timeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, timeout, timeout, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public Sentinel(final URI uri, final int connectionTimeout, final int soTimeout) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).build());
  }

  public Sentinel(final URI uri, final int connectionTimeout, final int soTimeout,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(final URI uri, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Sentinel(final URI uri, JedisClientConfig config) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
          "Cannot open Redis connection due invalid URI \"%s\".", uri.toString()));
    }
    connection = new Connection(new HostAndPort(uri.getHost(), uri.getPort()),
        DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(config.getConnectionTimeoutMillis())
            .socketTimeoutMillis(config.getSocketTimeoutMillis())
            .blockingSocketTimeoutMillis(config.getBlockingSocketTimeoutMillis())
            .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
            .database(JedisURIHelper.getDBIndex(uri)).clientName(config.getClientName())
            .ssl(JedisURIHelper.isRedisSSLScheme(uri)).sslSocketFactory(config.getSslSocketFactory())
            .sslParameters(config.getSslParameters()).hostnameVerifier(config.getHostnameVerifier())
            .build());
//    initializeFromURI(uri);
  }

  public Sentinel(final Connection connection) {
    this.connection = connection;
  }

  @Override
  public String toString() {
    return "Sentinel{" + connection + '}';
  }

  // Legacy
  public Connection getClient() {
    return getConnection();
  }

  public Connection getConnection() {
    return connection;
  }

  // Legacy
  public void connect() {
    connection.connect();
  }

  // Legacy
  public void disconnect() {
    connection.disconnect();
  }

  public boolean isConnected() {
    return connection.isConnected();
  }

  public boolean isBroken() {
    return connection.isBroken();
  }

  @Override
  public void close() {
    connection.close();
  }

  @Override
  public String sentinelMyId() {
    connection.sendCommand(SENTINEL, MYID);
    return connection.getBulkReply();
  }

  @Override
  public List<Map<String, String>> sentinelMasters() {
    connection.sendCommand(SENTINEL, MASTERS);
    return connection.getObjectMultiBulkReply().stream()
        .map(BuilderFactory.STRING_MAP::build).collect(Collectors.toList());
  }

  @Override
  public Map<String, String> sentinelMaster(String masterName) {
    connection.sendCommand(SENTINEL, MASTER.name(), masterName);
    return BuilderFactory.STRING_MAP.build(connection.getOne());
  }

  @Override
  public List<Map<String, String>> sentinelSentinels(String masterName) {
    connection.sendCommand(SENTINEL, SENTINELS.name(), masterName);
    return connection.getObjectMultiBulkReply().stream()
        .map(BuilderFactory.STRING_MAP::build).collect(Collectors.toList());
  }

  @Override
  public List<String> sentinelGetMasterAddrByName(String masterName) {
    connection.sendCommand(SENTINEL, GET_MASTER_ADDR_BY_NAME.getRaw(), SafeEncoder.encode(masterName));
    return connection.getMultiBulkReply();
  }

  @Override
  public Long sentinelReset(String pattern) {
    connection.sendCommand(SENTINEL, RESET.name(), pattern);
    return connection.getIntegerReply();
  }

  @Override
  public List<Map<String, String>> sentinelSlaves(String masterName) {
    connection.sendCommand(SENTINEL, SLAVES.name(), masterName);
    return connection.getObjectMultiBulkReply().stream()
        .map(BuilderFactory.STRING_MAP::build).collect(Collectors.toList());
  }

  @Override
  public List<Map<String, String>> sentinelReplicas(String masterName) {
    connection.sendCommand(SENTINEL, REPLICAS.name(), masterName);
    return connection.getObjectMultiBulkReply().stream()
        .map(BuilderFactory.STRING_MAP::build).collect(Collectors.toList());
  }

  @Override
  public String sentinelFailover(String masterName) {
    connection.sendCommand(SENTINEL, FAILOVER.name(), masterName);
    return connection.getStatusCodeReply();
  }

  @Override
  public String sentinelMonitor(String masterName, String ip, int port, int quorum) {
    CommandArguments args = new CommandArguments(SENTINEL).add(MONITOR)
        .add(masterName).add(ip).add(port).add(quorum);
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  @Override
  public String sentinelRemove(String masterName) {
    connection.sendCommand(SENTINEL, REMOVE.name(), masterName);
    return connection.getStatusCodeReply();
  }

  @Override
  public String sentinelSet(String masterName, Map<String, String> parameterMap) {
    CommandArguments args = new CommandArguments(SENTINEL).add(SET).add(masterName);
    parameterMap.entrySet().forEach(entry -> args.add(entry.getKey()).add(entry.getValue()));
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceed(connection, channels);
    } finally {
      connection.rollbackTimeout();
    }
  }

  public void psubscribe(final JedisPubSub jedisPubSub, final String... channels) {
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceedWithPatterns(connection, channels);
    } finally {
      connection.rollbackTimeout();
    }
  }

}
