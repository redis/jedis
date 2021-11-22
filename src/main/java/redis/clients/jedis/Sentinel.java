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

import redis.clients.jedis.commands.SentinelCommands;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.SafeEncoder;

public class Sentinel implements SentinelCommands, Closeable {

  protected final Connection connection;
  protected static final byte[][] DUMMY_ARRAY = new byte[0][];

  /**
   * This constructor only accepts a URI string. {@link JedisURIHelper#isValid(java.net.URI)} can be
   * used before this.
   * @param uriString
   */
  public Sentinel(final String uriString) {
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

  /**
   * <pre>
   * redis 127.0.0.1:26381&gt; sentinel masters
   * 1)  1) "name"
   *     2) "mymaster"
   *     3) "ip"
   *     4) "127.0.0.1"
   *     5) "port"
   *     6) "6379"
   *     7) "runid"
   *     8) "93d4d4e6e9c06d0eea36e27f31924ac26576081d"
   *     9) "flags"
   *    10) "master"
   *    11) "pending-commands"
   *    12) "0"
   *    13) "last-ok-ping-reply"
   *    14) "423"
   *    15) "last-ping-reply"
   *    16) "423"
   *    17) "info-refresh"
   *    18) "6107"
   *    19) "num-slaves"
   *    20) "1"
   *    21) "num-other-sentinels"
   *    22) "2"
   *    23) "quorum"
   *    24) "2"
   *
   * </pre>
   * @return
   */
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

  /**
   * <pre>
   * redis 127.0.0.1:26381&gt; sentinel get-master-addr-by-name mymaster
   * 1) "127.0.0.1"
   * 2) "6379"
   * </pre>
   * @param masterName
   * @return two elements list of strings : host and port.
   */
  @Override
  public List<String> sentinelGetMasterAddrByName(String masterName) {
    connection.sendCommand(SENTINEL, GET_MASTER_ADDR_BY_NAME.getRaw(), SafeEncoder.encode(masterName));
    return connection.getMultiBulkReply();
  }

  /**
   * <pre>
   * redis 127.0.0.1:26381&gt; sentinel reset mymaster
   * (integer) 1
   * </pre>
   * @param pattern
   * @return
   */
  @Override
  public Long sentinelReset(String pattern) {
    connection.sendCommand(SENTINEL, RESET.name(), pattern);
    return connection.getIntegerReply();
  }

  /**
   * <pre>
   * redis 127.0.0.1:26381&gt; sentinel slaves mymaster
   * 1)  1) "name"
   *     2) "127.0.0.1:6380"
   *     3) "ip"
   *     4) "127.0.0.1"
   *     5) "port"
   *     6) "6380"
   *     7) "runid"
   *     8) "d7f6c0ca7572df9d2f33713df0dbf8c72da7c039"
   *     9) "flags"
   *    10) "slave"
   *    11) "pending-commands"
   *    12) "0"
   *    13) "last-ok-ping-reply"
   *    14) "47"
   *    15) "last-ping-reply"
   *    16) "47"
   *    17) "info-refresh"
   *    18) "657"
   *    19) "master-link-down-time"
   *    20) "0"
   *    21) "master-link-status"
   *    22) "ok"
   *    23) "master-host"
   *    24) "localhost"
   *    25) "master-port"
   *    26) "6379"
   *    27) "slave-priority"
   *    28) "100"
   * </pre>
   * @param masterName
   * @return
   */
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
