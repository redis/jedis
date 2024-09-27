package redis.clients.jedis;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.json.JSONArray;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.args.*;
import redis.clients.jedis.bloom.*;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.commands.JedisBinaryCommands;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.commands.SampleBinaryKeyedCommands;
import redis.clients.jedis.commands.SampleKeyedCommands;
import redis.clients.jedis.commands.RedisModuleCommands;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.executors.*;
import redis.clients.jedis.gears.TFunctionListParams;
import redis.clients.jedis.gears.TFunctionLoadParams;
import redis.clients.jedis.gears.resps.GearsLibraryInfo;
import redis.clients.jedis.graph.GraphCommandObjects;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.mcf.CircuitBreakerCommandExecutor;
import redis.clients.jedis.mcf.MultiClusterPipeline;
import redis.clients.jedis.mcf.MultiClusterTransaction;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.FtAggregateIteration;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.timeseries.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.KeyValue;

public class UnifiedJedis implements JedisCommands, JedisBinaryCommands,
    SampleKeyedCommands, SampleBinaryKeyedCommands, RedisModuleCommands,
    AutoCloseable {

  @Deprecated
  protected RedisProtocol protocol = null;
  protected final ConnectionProvider provider;
  protected final CommandExecutor executor;
  protected final CommandObjects commandObjects;
  private final GraphCommandObjects graphCommandObjects;
  private JedisBroadcastAndRoundRobinConfig broadcastAndRoundRobinConfig = null;
  private final Cache cache;

  public UnifiedJedis() {
    this(new HostAndPort(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT));
  }

  public UnifiedJedis(HostAndPort hostAndPort) {
    this(new PooledConnectionProvider(hostAndPort), (RedisProtocol) null);
  }

  public UnifiedJedis(final String url) {
    this(URI.create(url));
  }

  public UnifiedJedis(final URI uri) {
    this(JedisURIHelper.getHostAndPort(uri), DefaultJedisClientConfig.builder()
        .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
        .database(JedisURIHelper.getDBIndex(uri)).protocol(JedisURIHelper.getRedisProtocol(uri))
        .ssl(JedisURIHelper.isRedisSSLScheme(uri)).build());
  }

  public UnifiedJedis(final URI uri, JedisClientConfig config) {
    this(JedisURIHelper.getHostAndPort(uri), DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(config.getConnectionTimeoutMillis())
        .socketTimeoutMillis(config.getSocketTimeoutMillis())
        .blockingSocketTimeoutMillis(config.getBlockingSocketTimeoutMillis())
        .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
        .database(JedisURIHelper.getDBIndex(uri)).clientName(config.getClientName())
        .protocol(JedisURIHelper.getRedisProtocol(uri))
        .ssl(JedisURIHelper.isRedisSSLScheme(uri)).sslSocketFactory(config.getSslSocketFactory())
        .sslParameters(config.getSslParameters()).hostnameVerifier(config.getHostnameVerifier()).build());
  }

  public UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new PooledConnectionProvider(hostAndPort, clientConfig), clientConfig.getRedisProtocol());
  }

  @Experimental
  public UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig, CacheConfig cacheConfig) {
    this(hostAndPort, clientConfig, CacheFactory.getCache(cacheConfig));
  }

  @Experimental
  public UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig, Cache cache) {
    this(new PooledConnectionProvider(hostAndPort, clientConfig, cache), clientConfig.getRedisProtocol(), cache);
  }

  public UnifiedJedis(ConnectionProvider provider) {
    this(new DefaultCommandExecutor(provider), provider);
  }

  protected UnifiedJedis(ConnectionProvider provider, RedisProtocol protocol) {
    this(new DefaultCommandExecutor(provider), provider, new CommandObjects(), protocol);
  }

  @Experimental
  protected UnifiedJedis(ConnectionProvider provider, RedisProtocol protocol, Cache cache) {
    this(new DefaultCommandExecutor(provider), provider, new CommandObjects(), protocol, cache);
  }

  /**
   * The constructor to directly use a custom {@link JedisSocketFactory}.
   * <p>
   * WARNING: Using this constructor means a {@link NullPointerException} will be occurred if
   * {@link UnifiedJedis#provider} is accessed.
   */
  public UnifiedJedis(JedisSocketFactory socketFactory) {
    this(new Connection(socketFactory));
  }

  /**
   * The constructor to directly use a custom {@link JedisSocketFactory}.
   * <p>
   * WARNING: Using this constructor means a {@link NullPointerException} will be occurred if
   * {@link UnifiedJedis#provider} is accessed.
   */
  public UnifiedJedis(JedisSocketFactory socketFactory, JedisClientConfig clientConfig) {
    this(new Connection(socketFactory, clientConfig));
  }

  /**
   * The constructor to directly use a {@link Connection}.
   * <p>
   * WARNING: Using this constructor means a {@link NullPointerException} will be occurred if
   * {@link UnifiedJedis#provider} is accessed.
   */
  public UnifiedJedis(Connection connection) {
    this.provider = null;
    this.executor = new SimpleCommandExecutor(connection);
    this.commandObjects = new CommandObjects();
    RedisProtocol proto = connection.getRedisProtocol();
    if (proto != null) {
      this.commandObjects.setProtocol(proto);
    }
    this.graphCommandObjects = new GraphCommandObjects(this);
    if (connection instanceof CacheConnection) {
      this.cache = ((CacheConnection) connection).getCache();
    } else {
      this.cache = null;
    }
  }

  @Deprecated
  public UnifiedJedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig, int maxAttempts) {
    this(jedisClusterNodes, clientConfig, maxAttempts,
        Duration.ofMillis(maxAttempts * clientConfig.getSocketTimeoutMillis()));
  }

  @Deprecated
  public UnifiedJedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(jedisClusterNodes, clientConfig), maxAttempts, maxTotalRetriesDuration,
        clientConfig.getRedisProtocol());
  }

  @Deprecated
  public UnifiedJedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new ClusterConnectionProvider(jedisClusterNodes, clientConfig, poolConfig), maxAttempts,
        maxTotalRetriesDuration, clientConfig.getRedisProtocol());
  }

  // Uses a fetched connection to process protocol. Should be avoided if possible.
  public UnifiedJedis(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new ClusterCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration), provider,
        new ClusterCommandObjects());
  }

  protected UnifiedJedis(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol) {
    this(new ClusterCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration), provider,
        new ClusterCommandObjects(), protocol);
  }

  @Experimental
  protected UnifiedJedis(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration,
      RedisProtocol protocol, Cache cache) {
    this(new ClusterCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration), provider,
        new ClusterCommandObjects(), protocol, cache);
  }

  /**
   * @deprecated Sharding/Sharded feature will be removed in next major release.
   */
  @Deprecated
  public UnifiedJedis(ShardedConnectionProvider provider) {
    this(new DefaultCommandExecutor(provider), provider, new ShardedCommandObjects(provider.getHashingAlgo()));
  }

  /**
   * @deprecated Sharding/Sharded feature will be removed in next major release.
   */
  @Deprecated
  public UnifiedJedis(ShardedConnectionProvider provider, Pattern tagPattern) {
    this(new DefaultCommandExecutor(provider), provider,
        new ShardedCommandObjects(provider.getHashingAlgo(), tagPattern));
  }

  public UnifiedJedis(ConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new RetryableCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration), provider);
  }

  /**
   * Constructor which supports multiple cluster/database endpoints each with their own isolated connection pool.
   * <p>
   * With this Constructor users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
   * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
   * <p>
   */
  @Experimental
  public UnifiedJedis(MultiClusterPooledConnectionProvider provider) {
    this(new CircuitBreakerCommandExecutor(provider), provider);
  }

  /**
   * The constructor to use a custom {@link CommandExecutor}.
   * <p>
   * WARNING: Using this constructor means a {@link NullPointerException} will be occurred if
   * {@link UnifiedJedis#provider} is accessed.
   */
  public UnifiedJedis(CommandExecutor executor) {
    this(executor, (ConnectionProvider) null);
  }

  private UnifiedJedis(CommandExecutor executor, ConnectionProvider provider) {
    this(executor, provider, new CommandObjects());
  }

  // Uses a fetched connection to process protocol. Should be avoided if possible.
  @VisibleForTesting
  public UnifiedJedis(CommandExecutor executor, ConnectionProvider provider, CommandObjects commandObjects) {
    this(executor, provider, commandObjects, null, null);
    if (this.provider != null) {
      try (Connection conn = this.provider.getConnection()) {
        if (conn != null) {
          RedisProtocol proto = conn.getRedisProtocol();
          if (proto != null) {
            this.commandObjects.setProtocol(proto);
          }
        }
      } catch (JedisException je) {
      }
    }
  }

  @Experimental
  private UnifiedJedis(CommandExecutor executor, ConnectionProvider provider, CommandObjects commandObjects,
      RedisProtocol protocol) {
    this(executor, provider, commandObjects, protocol, (Cache) null);
  }

  @Experimental
  private UnifiedJedis(CommandExecutor executor, ConnectionProvider provider, CommandObjects commandObjects,
      RedisProtocol protocol, Cache cache) {

    if (cache != null && protocol != RedisProtocol.RESP3) {
      throw new IllegalArgumentException("Client-side caching is only supported with RESP3.");
    }

    this.provider = provider;
    this.executor = executor;

    this.commandObjects = commandObjects;
    if (protocol != null) {
      this.commandObjects.setProtocol(protocol);
    }

    this.graphCommandObjects = new GraphCommandObjects(this);
    this.graphCommandObjects.setBaseCommandArgumentsCreator((comm) -> this.commandObjects.commandArguments(comm));
    this.cache = cache;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.executor);
  }

  @Deprecated
  protected final void setProtocol(RedisProtocol protocol) {
    this.protocol = protocol;
    this.commandObjects.setProtocol(this.protocol);
  }

  public final <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  public final <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executor.broadcastCommand(commandObject);
  }

  private <T> T checkAndBroadcastCommand(CommandObject<T> commandObject) {
    boolean broadcast = true;

    if (broadcastAndRoundRobinConfig == null) {
    } else if (commandObject.getArguments().getCommand() instanceof SearchProtocol.SearchCommand
        && broadcastAndRoundRobinConfig
            .getRediSearchModeInCluster() == JedisBroadcastAndRoundRobinConfig.RediSearchMode.LIGHT) {
      broadcast = false;
    }

    return broadcast ? broadcastCommand(commandObject) : executeCommand(commandObject);
  }

  public void setBroadcastAndRoundRobinConfig(JedisBroadcastAndRoundRobinConfig config) {
    this.broadcastAndRoundRobinConfig = config;
    this.commandObjects.setBroadcastAndRoundRobinConfig(this.broadcastAndRoundRobinConfig);
  }

  public Cache getCache() {
    return cache;
  }

  public String ping() {
    return checkAndBroadcastCommand(commandObjects.ping());
  }

  public String flushDB() {
    return checkAndBroadcastCommand(commandObjects.flushDB());
  }

  public String flushAll() {
    return checkAndBroadcastCommand(commandObjects.flushAll());
  }

  public String configSet(String parameter, String value) {
    return checkAndBroadcastCommand(commandObjects.configSet(parameter, value));
  }

  // Key commands
  @Override
  public boolean exists(String key) {
    return executeCommand(commandObjects.exists(key));
  }

  @Override
  public long exists(String... keys) {
    return executeCommand(commandObjects.exists(keys));
  }

  @Override
  public long persist(String key) {
    return executeCommand(commandObjects.persist(key));
  }

  @Override
  public String type(String key) {
    return executeCommand(commandObjects.type(key));
  }

  @Override
  public boolean exists(byte[] key) {
    return executeCommand(commandObjects.exists(key));
  }

  @Override
  public long exists(byte[]... keys) {
    return executeCommand(commandObjects.exists(keys));
  }

  @Override
  public long persist(byte[] key) {
    return executeCommand(commandObjects.persist(key));
  }

  @Override
  public String type(byte[] key) {
    return executeCommand(commandObjects.type(key));
  }

  @Override
  public byte[] dump(String key) {
    return executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public byte[] dump(byte[] key) {
    return executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public long expire(String key, long seconds) {
    return executeCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public long expire(String key, long seconds, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.expire(key, seconds, expiryOption));
  }

  @Override
  public long pexpire(String key, long milliseconds) {
    return executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long pexpire(String key, long milliseconds, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.pexpire(key, milliseconds, expiryOption));
  }

  @Override
  public long expireTime(String key) {
    return executeCommand(commandObjects.expireTime(key));
  }

  @Override
  public long pexpireTime(String key) {
    return executeCommand(commandObjects.pexpireTime(key));
  }

  @Override
  public long expireAt(String key, long unixTime) {
    return executeCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public long expireAt(String key, long unixTime, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.expireAt(key, unixTime, expiryOption));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption));
  }

  @Override
  public long expire(byte[] key, long seconds) {
    return executeCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public long expire(byte[] key, long seconds, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.expire(key, seconds, expiryOption));
  }

  @Override
  public long pexpire(byte[] key, long milliseconds) {
    return executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long pexpire(byte[] key, long milliseconds, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.pexpire(key, milliseconds, expiryOption));
  }

  @Override
  public long expireTime(byte[] key) {
    return executeCommand(commandObjects.expireTime(key));
  }

  @Override
  public long pexpireTime(byte[] key) {
    return executeCommand(commandObjects.pexpireTime(key));
  }

  @Override
  public long expireAt(byte[] key, long unixTime) {
    return executeCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public long expireAt(byte[] key, long unixTime, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.expireAt(key, unixTime, expiryOption));
  }

  @Override
  public long pexpireAt(byte[] key, long millisecondsTimestamp) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pexpireAt(byte[] key, long millisecondsTimestamp, ExpiryOption expiryOption) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption));
  }

  @Override
  public long ttl(String key) {
    return executeCommand(commandObjects.ttl(key));
  }

  @Override
  public long pttl(String key) {
    return executeCommand(commandObjects.pttl(key));
  }

  @Override
  public long touch(String key) {
    return executeCommand(commandObjects.touch(key));
  }

  @Override
  public long touch(String... keys) {
    return executeCommand(commandObjects.touch(keys));
  }

  @Override
  public long ttl(byte[] key) {
    return executeCommand(commandObjects.ttl(key));
  }

  @Override
  public long pttl(byte[] key) {
    return executeCommand(commandObjects.pttl(key));
  }

  @Override
  public long touch(byte[] key) {
    return executeCommand(commandObjects.touch(key));
  }

  @Override
  public long touch(byte[]... keys) {
    return executeCommand(commandObjects.touch(keys));
  }

  @Override
  public List<String> sort(String key) {
    return executeCommand(commandObjects.sort(key));
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParams) {
    return executeCommand(commandObjects.sort(key, sortingParams));
  }

  @Override
  public long sort(String key, String dstkey) {
    return executeCommand(commandObjects.sort(key, dstkey));
  }

  @Override
  public long sort(String key, SortingParams sortingParams, String dstkey) {
    return executeCommand(commandObjects.sort(key, sortingParams, dstkey));
  }

  @Override
  public List<String> sortReadonly(String key, SortingParams sortingParams) {
    return executeCommand(commandObjects.sortReadonly(key, sortingParams));
  }

  @Override
  public List<byte[]> sort(byte[] key) {
    return executeCommand(commandObjects.sort(key));
  }

  @Override
  public List<byte[]> sort(byte[] key, SortingParams sortingParams) {
    return executeCommand(commandObjects.sort(key, sortingParams));
  }

  @Override
  public long sort(byte[] key, byte[] dstkey) {
    return executeCommand(commandObjects.sort(key, dstkey));
  }

  @Override
  public List<byte[]> sortReadonly(byte[] key, SortingParams sortingParams) {
    return executeCommand(commandObjects.sortReadonly(key, sortingParams));
  }

  @Override
  public long sort(byte[] key, SortingParams sortingParams, byte[] dstkey) {
    return executeCommand(commandObjects.sort(key, sortingParams, dstkey));
  }

  @Override
  public long del(String key) {
    return executeCommand(commandObjects.del(key));
  }

  @Override
  public long del(String... keys) {
    return executeCommand(commandObjects.del(keys));
  }

  @Override
  public long unlink(String key) {
    return executeCommand(commandObjects.unlink(key));
  }

  @Override
  public long unlink(String... keys) {
    return executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public long del(byte[] key) {
    return executeCommand(commandObjects.del(key));
  }

  @Override
  public long del(byte[]... keys) {
    return executeCommand(commandObjects.del(keys));
  }

  @Override
  public long unlink(byte[] key) {
    return executeCommand(commandObjects.unlink(key));
  }

  @Override
  public long unlink(byte[]... keys) {
    return executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public Long memoryUsage(String key) {
    return executeCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Long memoryUsage(String key, int samples) {
    return executeCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public Long memoryUsage(byte[] key) {
    return executeCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Long memoryUsage(byte[] key, int samples) {
    return executeCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public boolean copy(String srcKey, String dstKey, boolean replace) {
    return executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(String oldkey, String newkey) {
    return executeCommand(commandObjects.rename(oldkey, newkey));
  }

  @Override
  public long renamenx(String oldkey, String newkey) {
    return executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  @Override
  public boolean copy(byte[] srcKey, byte[] dstKey, boolean replace) {
    return executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    return executeCommand(commandObjects.rename(oldkey, newkey));
  }

  @Override
  public long renamenx(byte[] oldkey, byte[] newkey) {
    return executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  public long dbSize() {
    return executeCommand(commandObjects.dbSize());
  }

  @Override
  public Set<String> keys(String pattern) {
    return executeCommand(commandObjects.keys(pattern));
  }

  @Override
  public ScanResult<String> scan(String cursor) {
    return executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params) {
    return executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params, String type) {
    return executeCommand(commandObjects.scan(cursor, params, type));
  }

  /**
   * @param batchCount COUNT for each batch execution
   * @param match pattern
   * @return scan iteration
   */
  public ScanIteration scanIteration(int batchCount, String match) {
    return new ScanIteration(provider, batchCount, match);
  }

  /**
   * @param batchCount COUNT for each batch execution
   * @param match pattern
   * @param type key type
   * @return scan iteration
   */
  public ScanIteration scanIteration(int batchCount, String match, String type) {
    return new ScanIteration(provider, batchCount, match, type);
  }

  @Override
  public Set<byte[]> keys(byte[] pattern) {
    return executeCommand(commandObjects.keys(pattern));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor) {
    return executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type) {
    return executeCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public String randomKey() {
    return executeCommand(commandObjects.randomKey());
  }

  @Override
  public byte[] randomBinaryKey() {
    return executeCommand(commandObjects.randomBinaryKey());
  }
  // Key commands

  // String commands
  @Override
  public String set(String key, String value) {
    return executeCommand(commandObjects.set(key, value));
  }

  @Override
  public String set(String key, String value, SetParams params) {
    return executeCommand(commandObjects.set(key, value, params));
  }

  @Override
  public String get(String key) {
    return executeCommand(commandObjects.get(key));
  }

  @Override
  public String setGet(String key, String value) {
    return executeCommand(commandObjects.setGet(key, value));
  }

  @Override
  public String setGet(String key, String value, SetParams params) {
    return executeCommand(commandObjects.setGet(key, value, params));
  }

  @Override
  public String getDel(String key) {
    return executeCommand(commandObjects.getDel(key));
  }

  @Override
  public String getEx(String key, GetExParams params) {
    return executeCommand(commandObjects.getEx(key, params));
  }

  @Override
  public String set(byte[] key, byte[] value) {
    return executeCommand(commandObjects.set(key, value));
  }

  @Override
  public String set(byte[] key, byte[] value, SetParams params) {
    return executeCommand(commandObjects.set(key, value, params));
  }

  @Override
  public byte[] get(byte[] key) {
    return executeCommand(commandObjects.get(key));
  }

  @Override
  public byte[] setGet(byte[] key, byte[] value) {
    return executeCommand(commandObjects.setGet(key, value));
  }

  @Override
  public byte[] setGet(byte[] key, byte[] value, SetParams params) {
    return executeCommand(commandObjects.setGet(key, value, params));
  }

  @Override
  public byte[] getDel(byte[] key) {
    return executeCommand(commandObjects.getDel(key));
  }

  @Override
  public byte[] getEx(byte[] key, GetExParams params) {
    return executeCommand(commandObjects.getEx(key, params));
  }

  @Override
  public boolean setbit(String key, long offset, boolean value) {
    return executeCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public boolean getbit(String key, long offset) {
    return executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(String key, long offset, String value) {
    return executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    return executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public boolean setbit(byte[] key, long offset, boolean value) {
    return executeCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public boolean getbit(byte[] key, long offset) {
    return executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(byte[] key, long offset, byte[] value) {
    return executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public byte[] getrange(byte[] key, long startOffset, long endOffset) {
    return executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  /**
   * @deprecated Use {@link UnifiedJedis#setGet(java.lang.String, java.lang.String)}.
   */
  @Deprecated
  @Override
  public String getSet(String key, String value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  @Override
  public long setnx(String key, String value) {
    return executeCommand(commandObjects.setnx(key, value));
  }

  @Override
  public String setex(String key, long seconds, String value) {
    return executeCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public String psetex(String key, long milliseconds, String value) {
    return executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  /**
   * @deprecated Use {@link UnifiedJedis#setGet(byte[], byte[])}.
   */
  @Deprecated
  @Override
  public byte[] getSet(byte[] key, byte[] value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  @Override
  public long setnx(byte[] key, byte[] value) {
    return executeCommand(commandObjects.setnx(key, value));
  }

  @Override
  public String setex(byte[] key, long seconds, byte[] value) {
    return executeCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public String psetex(byte[] key, long milliseconds, byte[] value) {
    return executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public long incr(String key) {
    return executeCommand(commandObjects.incr(key));
  }

  @Override
  public long incrBy(String key, long increment) {
    return executeCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public double incrByFloat(String key, double increment) {
    return executeCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public long decr(String key) {
    return executeCommand(commandObjects.decr(key));
  }

  @Override
  public long decrBy(String key, long decrement) {
    return executeCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public long incr(byte[] key) {
    return executeCommand(commandObjects.incr(key));
  }

  @Override
  public long incrBy(byte[] key, long increment) {
    return executeCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public double incrByFloat(byte[] key, double increment) {
    return executeCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public long decr(byte[] key) {
    return executeCommand(commandObjects.decr(key));
  }

  @Override
  public long decrBy(byte[] key, long decrement) {
    return executeCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public List<String> mget(String... keys) {
    return executeCommand(commandObjects.mget(keys));
  }

  @Override
  public String mset(String... keysvalues) {
    return executeCommand(commandObjects.mset(keysvalues));
  }

  @Override
  public long msetnx(String... keysvalues) {
    return executeCommand(commandObjects.msetnx(keysvalues));
  }

  @Override
  public List<byte[]> mget(byte[]... keys) {
    return executeCommand(commandObjects.mget(keys));
  }

  @Override
  public String mset(byte[]... keysvalues) {
    return executeCommand(commandObjects.mset(keysvalues));
  }

  @Override
  public long msetnx(byte[]... keysvalues) {
    return executeCommand(commandObjects.msetnx(keysvalues));
  }

  @Override
  public long append(String key, String value) {
    return executeCommand(commandObjects.append(key, value));
  }

  @Override
  public String substr(String key, int start, int end) {
    return executeCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public long strlen(String key) {
    return executeCommand(commandObjects.strlen(key));
  }

  @Override
  public long append(byte[] key, byte[] value) {
    return executeCommand(commandObjects.append(key, value));
  }

  @Override
  public byte[] substr(byte[] key, int start, int end) {
    return executeCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public long strlen(byte[] key) {
    return executeCommand(commandObjects.strlen(key));
  }

  @Override
  public long bitcount(String key) {
    return executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(String key, long start, long end) {
    return executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitcount(String key, long start, long end, BitCountOption option) {
    return executeCommand(commandObjects.bitcount(key, start, end, option));
  }

  @Override
  public long bitpos(String key, boolean value) {
    return executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(String key, boolean value, BitPosParams params) {
    return executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public long bitcount(byte[] key) {
    return executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(byte[] key, long start, long end) {
    return executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitcount(byte[] key, long start, long end, BitCountOption option) {
    return executeCommand(commandObjects.bitcount(key, start, end, option));
  }

  @Override
  public long bitpos(byte[] key, boolean value) {
    return executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(byte[] key, boolean value, BitPosParams params) {
    return executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public List<Long> bitfield(String key, String... arguments) {
    return executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(String key, String... arguments) {
    return executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public List<Long> bitfield(byte[] key, byte[]... arguments) {
    return executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(byte[] key, byte[]... arguments) {
    return executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public long bitop(BitOP op, String destKey, String... srcKeys) {
    return executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    return executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public LCSMatchResult lcs(String keyA, String keyB, LCSParams params) {
    return executeCommand(commandObjects.lcs(keyA, keyB, params));
  }

  @Override
  public LCSMatchResult lcs(byte[] keyA, byte[] keyB, LCSParams params) {
    return executeCommand(commandObjects.lcs(keyA, keyB, params));
  }
  // String commands

  // List commands
  @Override
  public long rpush(String key, String... string) {
    return executeCommand(commandObjects.rpush(key, string));
  }

  @Override
  public long lpush(String key, String... string) {
    return executeCommand(commandObjects.lpush(key, string));
  }

  @Override
  public long llen(String key) {
    return executeCommand(commandObjects.llen(key));
  }

  @Override
  public List<String> lrange(String key, long start, long stop) {
    return executeCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public String ltrim(String key, long start, long stop) {
    return executeCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public String lindex(String key, long index) {
    return executeCommand(commandObjects.lindex(key, index));
  }

  @Override
  public long rpush(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.rpush(key, args));
  }

  @Override
  public long lpush(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.lpush(key, args));
  }

  @Override
  public long llen(byte[] key) {
    return executeCommand(commandObjects.llen(key));
  }

  @Override
  public List<byte[]> lrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public String ltrim(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public byte[] lindex(byte[] key, long index) {
    return executeCommand(commandObjects.lindex(key, index));
  }

  @Override
  public String lset(String key, long index, String value) {
    return executeCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public long lrem(String key, long count, String value) {
    return executeCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public String lpop(String key) {
    return executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<String> lpop(String key, int count) {
    return executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public String lset(byte[] key, long index, byte[] value) {
    return executeCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public long lrem(byte[] key, long count, byte[] value) {
    return executeCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public byte[] lpop(byte[] key) {
    return executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<byte[]> lpop(byte[] key, int count) {
    return executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public Long lpos(String key, String element) {
    return executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(String key, String element, LPosParams params) {
    return executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(String key, String element, LPosParams params, long count) {
    return executeCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public Long lpos(byte[] key, byte[] element) {
    return executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(byte[] key, byte[] element, LPosParams params) {
    return executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(byte[] key, byte[] element, LPosParams params, long count) {
    return executeCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public String rpop(String key) {
    return executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<String> rpop(String key, int count) {
    return executeCommand(commandObjects.rpop(key, count));
  }

  @Override
  public byte[] rpop(byte[] key) {
    return executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<byte[]> rpop(byte[] key, int count) {
    return executeCommand(commandObjects.rpop(key, count));
  }

  @Override
  public long linsert(String key, ListPosition where, String pivot, String value) {
    return executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(String key, String... strings) {
    return executeCommand(commandObjects.lpushx(key, strings));
  }

  @Override
  public long rpushx(String key, String... strings) {
    return executeCommand(commandObjects.rpushx(key, strings));
  }

  @Override
  public long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
    return executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.lpushx(key, args));
  }

  @Override
  public long rpushx(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.rpushx(key, args));
  }

  @Override
  public List<String> blpop(int timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public KeyValue<String, String> blpop(double timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public List<String> brpop(int timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public KeyValue<String, String> brpop(double timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public List<String> blpop(int timeout, String... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public KeyValue<String, String> blpop(double timeout, String... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<String> brpop(int timeout, String... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public KeyValue<String, String> brpop(double timeout, String... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(int timeout, byte[]... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public KeyValue<byte[], byte[]> blpop(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<byte[]> brpop(int timeout, byte[]... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public KeyValue<byte[], byte[]> brpop(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public String rpoplpush(String srckey, String dstkey) {
    return executeCommand(commandObjects.rpoplpush(srckey, dstkey));
  }

  @Override
  public String brpoplpush(String source, String destination, int timeout) {
    return executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
    return executeCommand(commandObjects.rpoplpush(srckey, dstkey));
  }

  @Override
  public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
    return executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    return executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout) {
    return executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public KeyValue<String, List<String>> lmpop(ListDirection direction, String... keys) {
    return executeCommand(commandObjects.lmpop(direction, keys));
  }

  @Override
  public KeyValue<String, List<String>> lmpop(ListDirection direction, int count, String... keys) {
    return executeCommand(commandObjects.lmpop(direction, count, keys));
  }

  @Override
  public KeyValue<String, List<String>> blmpop(double timeout, ListDirection direction, String... keys) {
    return executeCommand(commandObjects.blmpop(timeout, direction, keys));
  }

  @Override
  public KeyValue<String, List<String>> blmpop(double timeout, ListDirection direction, int count, String... keys) {
    return executeCommand(commandObjects.blmpop(timeout, direction, count, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, byte[]... keys) {
    return executeCommand(commandObjects.lmpop(direction, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, int count, byte[]... keys) {
    return executeCommand(commandObjects.lmpop(direction, count, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> blmpop(double timeout, ListDirection direction, byte[]... keys) {
    return executeCommand(commandObjects.blmpop(timeout, direction, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> blmpop(double timeout, ListDirection direction, int count, byte[]... keys) {
    return executeCommand(commandObjects.blmpop(timeout, direction, count, keys));
  }
  // List commands

  // Hash commands
  @Override
  public long hset(String key, String field, String value) {
    return executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hset(key, hash));
  }

  @Override
  public String hget(String key, String field) {
    return executeCommand(commandObjects.hget(key, field));
  }

  @Override
  public long hsetnx(String key, String field, String value) {
    return executeCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    return executeCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public long hset(byte[] key, byte[] field, byte[] value) {
    return executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(commandObjects.hset(key, hash));
  }

  @Override
  public byte[] hget(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hget(key, field));
  }

  @Override
  public long hsetnx(byte[] key, byte[] field, byte[] value) {
    return executeCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public long hincrBy(String key, String field, long value) {
    return executeCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(String key, String field, double value) {
    return executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(String key, String field) {
    return executeCommand(commandObjects.hexists(key, field));
  }

  @Override
  public long hdel(String key, String... field) {
    return executeCommand(commandObjects.hdel(key, field));
  }

  @Override
  public long hlen(String key) {
    return executeCommand(commandObjects.hlen(key));
  }

  @Override
  public long hincrBy(byte[] key, byte[] field, long value) {
    return executeCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(byte[] key, byte[] field, double value) {
    return executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hexists(key, field));
  }

  @Override
  public long hdel(byte[] key, byte[]... field) {
    return executeCommand(commandObjects.hdel(key, field));
  }

  @Override
  public long hlen(byte[] key) {
    return executeCommand(commandObjects.hlen(key));
  }

  @Override
  public Set<String> hkeys(String key) {
    return executeCommand(commandObjects.hkeys(key));
  }

  @Override
  public List<String> hvals(String key) {
    return executeCommand(commandObjects.hvals(key));
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    return executeCommand(commandObjects.hgetAll(key));
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return executeCommand(commandObjects.hkeys(key));
  }

  @Override
  public List<byte[]> hvals(byte[] key) {
    return executeCommand(commandObjects.hvals(key));
  }

  @Override
  public Map<byte[], byte[]> hgetAll(byte[] key) {
    return executeCommand(commandObjects.hgetAll(key));
  }

  @Override
  public String hrandfield(String key) {
    return executeCommand(commandObjects.hrandfield(key));
  }

  @Override
  public List<String> hrandfield(String key, long count) {
    return executeCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public List<Map.Entry<String, String>> hrandfieldWithValues(String key, long count) {
    return executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public ScanResult<String> hscanNoValues(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.hscanNoValues(key, cursor, params));
  }

  @Override
  public long hstrlen(String key, String field) {
    return executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public byte[] hrandfield(byte[] key) {
    return executeCommand(commandObjects.hrandfield(key));
  }

  @Override
  public List<byte[]> hrandfield(byte[] key, long count) {
    return executeCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public List<Map.Entry<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count) {
    return executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public ScanResult<byte[]> hscanNoValues(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.hscanNoValues(key, cursor, params));
  }

  @Override
  public long hstrlen(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public List<Long> hexpire(String key, long seconds, String... fields) {
    return executeCommand(commandObjects.hexpire(key, seconds, fields));
  }

  @Override
  public List<Long> hexpire(String key, long seconds, ExpiryOption condition, String... fields) {
    return executeCommand(commandObjects.hexpire(key, seconds, condition, fields));
  }

  @Override
  public List<Long> hpexpire(String key, long milliseconds, String... fields) {
    return executeCommand(commandObjects.hpexpire(key, milliseconds, fields));
  }

  @Override
  public List<Long> hpexpire(String key, long milliseconds, ExpiryOption condition, String... fields) {
    return executeCommand(commandObjects.hpexpire(key, milliseconds, condition, fields));
  }

  @Override
  public List<Long> hexpireAt(String key, long unixTimeSeconds, String... fields) {
    return executeCommand(commandObjects.hexpireAt(key, unixTimeSeconds, fields));
  }

  @Override
  public List<Long> hexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... fields) {
    return executeCommand(commandObjects.hexpireAt(key, unixTimeSeconds, condition, fields));
  }

  @Override
  public List<Long> hpexpireAt(String key, long unixTimeMillis, String... fields) {
    return executeCommand(commandObjects.hpexpireAt(key, unixTimeMillis, fields));
  }

  @Override
  public List<Long> hpexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... fields) {
    return executeCommand(commandObjects.hpexpireAt(key, unixTimeMillis, condition, fields));
  }

  @Override
  public List<Long> hexpire(byte[] key, long seconds, byte[]... fields) {
    return executeCommand(commandObjects.hexpire(key, seconds, fields));
  }

  @Override
  public List<Long> hexpire(byte[] key, long seconds, ExpiryOption condition, byte[]... fields) {
    return executeCommand(commandObjects.hexpire(key, seconds, condition, fields));
  }

  @Override
  public List<Long> hpexpire(byte[] key, long milliseconds, byte[]... fields) {
    return executeCommand(commandObjects.hpexpire(key, milliseconds, fields));
  }

  @Override
  public List<Long> hpexpire(byte[] key, long milliseconds, ExpiryOption condition, byte[]... fields) {
    return executeCommand(commandObjects.hpexpire(key, milliseconds, condition, fields));
  }

  @Override
  public List<Long> hexpireAt(byte[] key, long unixTimeSeconds, byte[]... fields) {
    return executeCommand(commandObjects.hexpireAt(key, unixTimeSeconds, fields));
  }

  @Override
  public List<Long> hexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition, byte[]... fields) {
    return executeCommand(commandObjects.hexpireAt(key, unixTimeSeconds, condition, fields));
  }

  @Override
  public List<Long> hpexpireAt(byte[] key, long unixTimeMillis, byte[]... fields) {
    return executeCommand(commandObjects.hpexpireAt(key, unixTimeMillis, fields));
  }

  @Override
  public List<Long> hpexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition, byte[]... fields) {
    return executeCommand(commandObjects.hpexpireAt(key, unixTimeMillis, condition, fields));
  }

  @Override
  public List<Long> hexpireTime(String key, String... fields) {
    return executeCommand(commandObjects.hexpireTime(key, fields));
  }

  @Override
  public List<Long> hpexpireTime(String key, String... fields) {
    return executeCommand(commandObjects.hpexpireTime(key, fields));
  }

  @Override
  public List<Long> httl(String key, String... fields) {
    return executeCommand(commandObjects.httl(key, fields));
  }

  @Override
  public List<Long> hpttl(String key, String... fields) {
    return executeCommand(commandObjects.hpttl(key, fields));
  }

  @Override
  public List<Long> hexpireTime(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hexpireTime(key, fields));
  }

  @Override
  public List<Long> hpexpireTime(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hpexpireTime(key, fields));
  }

  @Override
  public List<Long> httl(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.httl(key, fields));
  }

  @Override
  public List<Long> hpttl(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hpttl(key, fields));
  }

  @Override
  public List<Long> hpersist(String key, String... fields) {
    return executeCommand(commandObjects.hpersist(key, fields));
  }

  @Override
  public List<Long> hpersist(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hpersist(key, fields));
  }
  // Hash commands

  // Set commands
  @Override
  public long sadd(String key, String... members) {
    return executeCommand(commandObjects.sadd(key, members));
  }

  @Override
  public Set<String> smembers(String key) {
    return executeCommand(commandObjects.smembers(key));
  }

  @Override
  public long srem(String key, String... members) {
    return executeCommand(commandObjects.srem(key, members));
  }

  @Override
  public String spop(String key) {
    return executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<String> spop(String key, long count) {
    return executeCommand(commandObjects.spop(key, count));
  }

  @Override
  public long scard(String key) {
    return executeCommand(commandObjects.scard(key));
  }

  @Override
  public boolean sismember(String key, String member) {
    return executeCommand(commandObjects.sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(String key, String... members) {
    return executeCommand(commandObjects.smismember(key, members));
  }

  @Override
  public long sadd(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.sadd(key, members));
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    return executeCommand(commandObjects.smembers(key));
  }

  @Override
  public long srem(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.srem(key, members));
  }

  @Override
  public byte[] spop(byte[] key) {
    return executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    return executeCommand(commandObjects.spop(key, count));
  }

  @Override
  public long scard(byte[] key) {
    return executeCommand(commandObjects.scard(key));
  }

  @Override
  public boolean sismember(byte[] key, byte[] member) {
    return executeCommand(commandObjects.sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.smismember(key, members));
  }

  @Override
  public String srandmember(String key) {
    return executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<String> srandmember(String key, int count) {
    return executeCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public byte[] srandmember(byte[] key) {
    return executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<byte[]> srandmember(byte[] key, int count) {
    return executeCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public Set<String> sdiff(String... keys) {
    return executeCommand(commandObjects.sdiff(keys));
  }

  @Override
  public long sdiffstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sdiffstore(dstkey, keys));
  }

  @Override
  public Set<String> sinter(String... keys) {
    return executeCommand(commandObjects.sinter(keys));
  }

  @Override
  public long sinterstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  @Override
  public long sintercard(String... keys) {
    return executeCommand(commandObjects.sintercard(keys));
  }

  @Override
  public long sintercard(int limit, String... keys) {
    return executeCommand(commandObjects.sintercard(limit, keys));
  }

  @Override
  public Set<String> sunion(String... keys) {
    return executeCommand(commandObjects.sunion(keys));
  }

  @Override
  public long sunionstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sunionstore(dstkey, keys));
  }

  @Override
  public long smove(String srckey, String dstkey, String member) {
    return executeCommand(commandObjects.smove(srckey, dstkey, member));
  }

  @Override
  public Set<byte[]> sdiff(byte[]... keys) {
    return executeCommand(commandObjects.sdiff(keys));
  }

  @Override
  public long sdiffstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sdiffstore(dstkey, keys));
  }

  @Override
  public Set<byte[]> sinter(byte[]... keys) {
    return executeCommand(commandObjects.sinter(keys));
  }

  @Override
  public long sinterstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  @Override
  public long sintercard(byte[]... keys) {
    return executeCommand(commandObjects.sintercard(keys));
  }

  @Override
  public long sintercard(int limit, byte[]... keys) {
    return executeCommand(commandObjects.sintercard(limit, keys));
  }

  @Override
  public Set<byte[]> sunion(byte[]... keys) {
    return executeCommand(commandObjects.sunion(keys));
  }

  @Override
  public long sunionstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sunionstore(dstkey, keys));
  }

  @Override
  public long smove(byte[] srckey, byte[] dstkey, byte[] member) {
    return executeCommand(commandObjects.smove(srckey, dstkey, member));
  }
  // Set commands

  // Sorted Set commands
  @Override
  public long zadd(String key, double score, String member) {
    return executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers) {
    return executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member) {
    return executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    return executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public long zrem(String key, String... members) {
    return executeCommand(commandObjects.zrem(key, members));
  }

  @Override
  public double zincrby(String key, double increment, String member) {
    return executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
    return executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(String key, String member) {
    return executeCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Long zrevrank(String key, String member) {
    return executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrankWithScore(String key, String member) {
    return executeCommand(commandObjects.zrankWithScore(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrevrankWithScore(String key, String member) {
    return executeCommand(commandObjects.zrevrankWithScore(key, member));
  }

  @Override
  public long zrem(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.zrem(key, members));
  }

  @Override
  public double zincrby(byte[] key, double increment, byte[] member) {
    return executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
    return executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Long zrevrank(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrankWithScore(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrankWithScore(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrevrankWithScore(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrevrankWithScore(key, member));
  }

  @Override
  public String zrandmember(String key) {
    return executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public List<String> zrandmember(String key, long count) {
    return executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(String key, long count) {
    return executeCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(String key) {
    return executeCommand(commandObjects.zcard(key));
  }

  @Override
  public Double zscore(String key, String member) {
    return executeCommand(commandObjects.zscore(key, member));
  }

  @Override
  public List<Double> zmscore(String key, String... members) {
    return executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public byte[] zrandmember(byte[] key) {
    return executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public List<byte[]> zrandmember(byte[] key, long count) {
    return executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(byte[] key, long count) {
    return executeCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(byte[] key) {
    return executeCommand(commandObjects.zcard(key));
  }

  @Override
  public Double zscore(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zscore(key, member));
  }

  @Override
  public List<Double> zmscore(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(String key) {
    return executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(String key, int count) {
    return executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(String key) {
    return executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(String key, int count) {
    return executeCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public long zcount(String key, double min, double max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(String key, String min, String max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Tuple zpopmax(byte[] key) {
    return executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(byte[] key, int count) {
    return executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(byte[] key) {
    return executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(byte[] key, int count) {
    return executeCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public long zcount(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public List<String> zrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public List<String> zrevrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public List<String> zrange(String key, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrange(key, zRangeParams));
  }

  @Override
  public List<Tuple> zrangeWithScores(String key, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrangeWithScores(key, zRangeParams));
  }

  @Override
  public long zrangestore(String dest, String src, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrangestore(dest, src, zRangeParams));
  }

  @Override
  public List<String> zrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<String> zrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<String> zrevrangeByScore(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<byte[]> zrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public List<byte[]> zrevrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public List<byte[]> zrange(byte[] key, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrange(key, zRangeParams));
  }

  @Override
  public List<Tuple> zrangeWithScores(byte[] key, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrangeWithScores(key, zRangeParams));
  }

  @Override
  public long zrangestore(byte[] dest, byte[] src, ZRangeParams zRangeParams) {
    return executeCommand(commandObjects.zrangestore(dest, src, zRangeParams));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByRank(String key, long start, long stop) {
    return executeCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByRank(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zlexcount(String key, String min, String max) {
    return executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByLex(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public List<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public long zlexcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public KeyValue<String, Tuple> bzpopmax(double timeout, String... keys) {
    return executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public KeyValue<String, Tuple> bzpopmin(double timeout, String... keys) {
    return executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public KeyValue<byte[], Tuple> bzpopmax(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public KeyValue<byte[], Tuple> bzpopmin(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public List<String> zdiff(String... keys) {
    return executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public List<Tuple> zdiffWithScores(String... keys) {
    return executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  @Deprecated
  public long zdiffStore(String dstkey, String... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Override
  public long zdiffstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.zdiffstore(dstkey, keys));
  }

  @Override
  public List<byte[]> zdiff(byte[]... keys) {
    return executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public List<Tuple> zdiffWithScores(byte[]... keys) {
    return executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  @Deprecated
  public long zdiffStore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Override
  public long zdiffstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.zdiffstore(dstkey, keys));
  }

  @Override
  public long zinterstore(String dstkey, String... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public List<String> zinter(ZParams params, String... keys) {
    return executeCommand(commandObjects.zinter(params, keys));
  }

  @Override
  public List<Tuple> zinterWithScores(ZParams params, String... keys) {
    return executeCommand(commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public long zinterstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public long zintercard(byte[]... keys) {
    return executeCommand(commandObjects.zintercard(keys));
  }

  @Override
  public long zintercard(long limit, byte[]... keys) {
    return executeCommand(commandObjects.zintercard(limit, keys));
  }

  @Override
  public long zintercard(String... keys) {
    return executeCommand(commandObjects.zintercard(keys));
  }

  @Override
  public long zintercard(long limit, String... keys) {
    return executeCommand(commandObjects.zintercard(limit, keys));
  }

  @Override
  public List<byte[]> zinter(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zinter(params, keys));
  }

  @Override
  public List<Tuple> zinterWithScores(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public List<String> zunion(ZParams params, String... keys) {
    return executeCommand(commandObjects.zunion(params, keys));
  }

  @Override
  public List<Tuple> zunionWithScores(ZParams params, String... keys) {
    return executeCommand(commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(String dstkey, String... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }

  @Override
  public List<byte[]> zunion(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zunion(params, keys));
  }

  @Override
  public List<Tuple> zunionWithScores(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }

  @Override
  public KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, String... keys) {
    return executeCommand(commandObjects.zmpop(option, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, int count, String... keys) {
    return executeCommand(commandObjects.zmpop(option, count, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> bzmpop(double timeout, SortedSetOption option, String... keys) {
    return executeCommand(commandObjects.bzmpop(timeout, option, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> bzmpop(double timeout, SortedSetOption option, int count, String... keys) {
    return executeCommand(commandObjects.bzmpop(timeout, option, count, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, byte[]... keys) {
    return executeCommand(commandObjects.zmpop(option, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, int count, byte[]... keys) {
    return executeCommand(commandObjects.zmpop(option, count, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option, byte[]... keys) {
    return executeCommand(commandObjects.bzmpop(timeout, option, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option, int count, byte[]... keys) {
    return executeCommand(commandObjects.bzmpop(timeout, option, count, keys));
  }
  // Sorted Set commands

  // Geo commands
  @Override
  public long geoadd(String key, double longitude, double latitude, String member) {
    return executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(String key, String member1, String member2) {
    return executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(String key, String member1, String member2, GeoUnit unit) {
    return executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<String> geohash(String key, String... members) {
    return executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(String key, String... members) {
    return executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    return executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(byte[] key, GeoAddParams params, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2) {
    return executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    return executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<byte[]> geohash(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, coord, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, String member, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, member, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, coord, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearch(key, params));
  }

  @Override
  public long geosearchStore(String dest, String src, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, member, radius, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoCoordinate coord, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, coord, radius, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, String member, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, member, width, height, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoCoordinate coord, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, coord, width, height, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearchStore(dest, src, params));
  }

  @Override
  public long geosearchStoreStoreDist(String dest, String src, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearchStoreStoreDist(dest, src, params));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoCoordinate coord, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, coord, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, byte[] member, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, member, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoCoordinate coord, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearch(key, coord, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearch(key, params));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, member, radius, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, coord, radius, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, byte[] member, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, member, width, height, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double width, double height, GeoUnit unit) {
    return executeCommand(commandObjects.geosearchStore(dest, src, coord, width, height, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearchStore(dest, src, params));
  }

  @Override
  public long geosearchStoreStoreDist(byte[] dest, byte[] src, GeoSearchParam params) {
    return executeCommand(commandObjects.geosearchStoreStoreDist(dest, src, params));
  }
  // Geo commands

  // Hyper Log Log commands
  @Override
  public long pfadd(String key, String... elements) {
    return executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public String pfmerge(String destkey, String... sourcekeys) {
    return executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(String key) {
    return executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public long pfcount(String... keys) {
    return executeCommand(commandObjects.pfcount(keys));
  }

  @Override
  public long pfadd(byte[] key, byte[]... elements) {
    return executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public String pfmerge(byte[] destkey, byte[]... sourcekeys) {
    return executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(byte[] key) {
    return executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public long pfcount(byte[]... keys) {
    return executeCommand(commandObjects.pfcount(keys));
  }
  // Hyper Log Log commands

  // Stream commands
  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return executeCommand(commandObjects.xadd(key, id, hash));
  }

  @Override
  public StreamEntryID xadd(String key, XAddParams params, Map<String, String> hash) {
    return executeCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public long xlen(String key) {
    return executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public List<StreamEntry> xrange(String key, String start, String end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, String start, String end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, String end, String start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, String end, String start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(String key, String group, StreamEntryID... ids) {
    return executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(String key, String groupName, StreamEntryID id, boolean makeStream) {
    return executeCommand(commandObjects.xgroupCreate(key, groupName, id, makeStream));
  }

  @Override
  public String xgroupSetID(String key, String groupName, StreamEntryID id) {
    return executeCommand(commandObjects.xgroupSetID(key, groupName, id));
  }

  @Override
  public long xgroupDestroy(String key, String groupName) {
    return executeCommand(commandObjects.xgroupDestroy(key, groupName));
  }

  @Override
  public boolean xgroupCreateConsumer(String key, String groupName, String consumerName) {
    return executeCommand(commandObjects.xgroupCreateConsumer(key, groupName, consumerName));
  }

  @Override
  public long xgroupDelConsumer(String key, String groupName, String consumerName) {
    return executeCommand(commandObjects.xgroupDelConsumer(key, groupName, consumerName));
  }

  @Override
  public StreamPendingSummary xpending(String key, String groupName) {
    return executeCommand(commandObjects.xpending(key, groupName));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupName, XPendingParams params) {
    return executeCommand(commandObjects.xpending(key, groupName, params));
  }

  @Override
  public long xdel(String key, StreamEntryID... ids) {
    return executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(String key, long maxLen, boolean approximate) {
    return executeCommand(commandObjects.xtrim(key, maxLen, approximate));
  }

  @Override
  public long xtrim(String key, XTrimParams params) {
    return executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumerName, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumerName, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaimJustId(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaim(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public StreamInfo xinfoStream(String key) {
    return executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public StreamFullInfo xinfoStreamFull(String key) {
    return executeCommand(commandObjects.xinfoStreamFull(key));
  }

  @Override
  public StreamFullInfo xinfoStreamFull(String key, int count) {
    return executeCommand(commandObjects.xinfoStreamFull(key, count));
  }

  @Override
  public List<StreamGroupInfo> xinfoGroups(String key) {
    return executeCommand(commandObjects.xinfoGroups(key));
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    return executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public List<StreamConsumerInfo> xinfoConsumers2(String key, String group) {
    return executeCommand(commandObjects.xinfoConsumers2(key, group));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public Map<String, List<StreamEntry>> xreadAsMap(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xreadAsMap(xReadParams, streams));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupName, String consumer, XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, streams));
  }

  @Override
  public Map<String, List<StreamEntry>> xreadGroupAsMap(String groupName, String consumer, XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xreadGroupAsMap(groupName, consumer, xReadGroupParams, streams));
  }

  @Override
  public byte[] xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash) {
    return executeCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public long xlen(byte[] key) {
    return executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<Object> xrange(byte[] key, byte[] start, byte[] end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<Object> xrange(byte[] key, byte[] start, byte[] end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<Object> xrevrange(byte[] key, byte[] end, byte[] start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<Object> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(byte[] key, byte[] group, byte[]... ids) {
    return executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(byte[] key, byte[] groupName, byte[] id, boolean makeStream) {
    return executeCommand(commandObjects.xgroupCreate(key, groupName, id, makeStream));
  }

  @Override
  public String xgroupSetID(byte[] key, byte[] groupName, byte[] id) {
    return executeCommand(commandObjects.xgroupSetID(key, groupName, id));
  }

  @Override
  public long xgroupDestroy(byte[] key, byte[] groupName) {
    return executeCommand(commandObjects.xgroupDestroy(key, groupName));
  }

  @Override
  public boolean xgroupCreateConsumer(byte[] key, byte[] groupName, byte[] consumerName) {
    return executeCommand(commandObjects.xgroupCreateConsumer(key, groupName, consumerName));
  }

  @Override
  public long xgroupDelConsumer(byte[] key, byte[] groupName, byte[] consumerName) {
    return executeCommand(commandObjects.xgroupDelConsumer(key, groupName, consumerName));
  }

  @Override
  public long xdel(byte[] key, byte[]... ids) {
    return executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(byte[] key, long maxLen, boolean approximateLength) {
    return executeCommand(commandObjects.xtrim(key, maxLen, approximateLength));
  }

  @Override
  public long xtrim(byte[] key, XTrimParams params) {
    return executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public Object xpending(byte[] key, byte[] groupName) {
    return executeCommand(commandObjects.xpending(key, groupName));
  }

  @Override
  public List<Object> xpending(byte[] key, byte[] groupName, XPendingParams params) {
    return executeCommand(commandObjects.xpending(key, groupName, params));
  }

  @Override
  public List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids) {
    return executeCommand(commandObjects.xclaimJustId(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaim(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public Object xinfoStream(byte[] key) {
    return executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public Object xinfoStreamFull(byte[] key) {
    return executeCommand(commandObjects.xinfoStreamFull(key));
  }

  @Override
  public Object xinfoStreamFull(byte[] key, int count) {
    return executeCommand(commandObjects.xinfoStreamFull(key, count));
  }

  @Override
  public List<Object> xinfoGroups(byte[] key) {
    return executeCommand(commandObjects.xinfoGroups(key));
  }

  @Override
  public List<Object> xinfoConsumers(byte[] key, byte[] group) {
    return executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public List<Object> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public List<Object> xreadGroup(byte[] groupName, byte[] consumer, XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, streams));
  }
  // Stream commands

  // Scripting commands
  @Override
  public Object eval(String script) {
    return executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object eval(String script, int keyCount, String... params) {
    return executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(String script, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Object evalReadonly(String script, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.evalReadonly(script, keys, args));
  }

  @Override
  public Object evalsha(String sha1) {
    return executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(String sha1, int keyCount, String... params) {
    return executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(String sha1, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Object evalshaReadonly(String sha1, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.evalshaReadonly(sha1, keys, args));
  }

  @Override
  public Object eval(byte[] script) {
    return executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object eval(byte[] script, int keyCount, byte[]... params) {
    return executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Object evalReadonly(byte[] script, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.evalReadonly(script, keys, args));
  }

  @Override
  public Object evalsha(byte[] sha1) {
    return executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
    return executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Object evalshaReadonly(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.evalshaReadonly(sha1, keys, args));
  }

  @Override
  public Object fcall(String name, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.fcall(name, keys, args));
  }

  @Override
  public Object fcallReadonly(String name, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.fcallReadonly(name, keys, args));
  }

  @Override
  public String functionDelete(String libraryName) {
    return checkAndBroadcastCommand(commandObjects.functionDelete(libraryName));
  }

  @Override
  public String functionFlush() {
    return checkAndBroadcastCommand(commandObjects.functionFlush());
  }

  @Override
  public String functionFlush(FlushMode mode) {
    return checkAndBroadcastCommand(commandObjects.functionFlush(mode));
  }

  @Override
  public String functionKill() {
    return checkAndBroadcastCommand(commandObjects.functionKill());
  }

  @Override
  public List<LibraryInfo> functionList() {
    return executeCommand(commandObjects.functionList());
  }

  @Override
  public List<LibraryInfo> functionList(String libraryNamePattern) {
    return executeCommand(commandObjects.functionList(libraryNamePattern));
  }

  @Override
  public List<LibraryInfo> functionListWithCode() {
    return executeCommand(commandObjects.functionListWithCode());
  }

  @Override
  public List<LibraryInfo> functionListWithCode(String libraryNamePattern) {
    return executeCommand(commandObjects.functionListWithCode(libraryNamePattern));
  }

  @Override
  public String functionLoad(String functionCode) {
    return checkAndBroadcastCommand(commandObjects.functionLoad(functionCode));
  }

  @Override
  public String functionLoadReplace(String functionCode) {
    return checkAndBroadcastCommand(commandObjects.functionLoadReplace(functionCode));
  }

  @Override
  public FunctionStats functionStats() {
    return executeCommand(commandObjects.functionStats());
  }

  @Override
  public Object fcall(byte[] name, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.fcall(name, keys, args));
  }

  @Override
  public Object fcallReadonly(byte[] name, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.fcallReadonly(name, keys, args));
  }

  @Override
  public String functionDelete(byte[] libraryName) {
    return checkAndBroadcastCommand(commandObjects.functionDelete(libraryName));
  }

  @Override
  public byte[] functionDump() {
    return executeCommand(commandObjects.functionDump());
  }

  @Override
  public List<Object> functionListBinary() {
    return executeCommand(commandObjects.functionListBinary());
  }

  @Override
  public List<Object> functionList(final byte[] libraryNamePattern) {
    return executeCommand(commandObjects.functionList(libraryNamePattern));
  }

  @Override
  public List<Object> functionListWithCodeBinary() {
    return executeCommand(commandObjects.functionListWithCodeBinary());
  }

  @Override
  public List<Object> functionListWithCode(final byte[] libraryNamePattern) {
    return executeCommand(commandObjects.functionListWithCode(libraryNamePattern));
  }

  @Override
  public String functionLoad(byte[] functionCode) {
    return checkAndBroadcastCommand(commandObjects.functionLoad(functionCode));
  }

  @Override
  public String functionLoadReplace(byte[] functionCode) {
    return checkAndBroadcastCommand(commandObjects.functionLoadReplace(functionCode));
  }

  @Override
  public String functionRestore(byte[] serializedValue) {
    return checkAndBroadcastCommand(commandObjects.functionRestore(serializedValue));
  }

  @Override
  public String functionRestore(byte[] serializedValue, FunctionRestorePolicy policy) {
    return checkAndBroadcastCommand(commandObjects.functionRestore(serializedValue, policy));
  }

  @Override
  public Object functionStatsBinary() {
    return executeCommand(commandObjects.functionStatsBinary());
  }
  // Scripting commands

  // Other key commands
  @Override
  public Long objectRefcount(String key) {
    return executeCommand(commandObjects.objectRefcount(key));
  }

  @Override
  public String objectEncoding(String key) {
    return executeCommand(commandObjects.objectEncoding(key));
  }

  @Override
  public Long objectIdletime(String key) {
    return executeCommand(commandObjects.objectIdletime(key));
  }

  @Override
  public Long objectFreq(String key) {
    return executeCommand(commandObjects.objectFreq(key));
  }

  @Override
  public Long objectRefcount(byte[] key) {
    return executeCommand(commandObjects.objectRefcount(key));
  }

  @Override
  public byte[] objectEncoding(byte[] key) {
    return executeCommand(commandObjects.objectEncoding(key));
  }

  @Override
  public Long objectIdletime(byte[] key) {
    return executeCommand(commandObjects.objectIdletime(key));
  }

  @Override
  public Long objectFreq(byte[] key) {
    return executeCommand(commandObjects.objectFreq(key));
  }

  @Override
  public String migrate(String host, int port, String key, int timeout) {
    return executeCommand(commandObjects.migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, String... keys) {
    return executeCommand(commandObjects.migrate(host, port, timeout, params, keys));
  }

  @Override
  public String migrate(String host, int port, byte[] key, int timeout) {
    return executeCommand(commandObjects.migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys) {
    return executeCommand(commandObjects.migrate(host, port, timeout, params, keys));
  }
  // Other key commands

  // Sample key commands
  @Override
  public long waitReplicas(String sampleKey, int replicas, long timeout) {
    return executeCommand(commandObjects.waitReplicas(sampleKey, replicas, timeout));
  }

  @Override
  public long waitReplicas(byte[] sampleKey, int replicas, long timeout) {
    return executeCommand(commandObjects.waitReplicas(sampleKey, replicas, timeout));
  }

  @Override
  public KeyValue<Long, Long> waitAOF(String sampleKey, long numLocal, long numReplicas, long timeout) {
    return executeCommand(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout));
  }

  @Override
  public KeyValue<Long, Long> waitAOF(byte[] sampleKey, long numLocal, long numReplicas, long timeout) {
    return executeCommand(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout));
  }

  @Override
  public Object eval(String script, String sampleKey) {
    return executeCommand(commandObjects.eval(script, sampleKey));
  }

  @Override
  public Object evalsha(String sha1, String sampleKey) {
    return executeCommand(commandObjects.evalsha(sha1, sampleKey));
  }

  @Override
  public Object eval(byte[] script, byte[] sampleKey) {
    return executeCommand(commandObjects.eval(script, sampleKey));
  }

  @Override
  public Object evalsha(byte[] sha1, byte[] sampleKey) {
    return executeCommand(commandObjects.evalsha(sha1, sampleKey));
  }

  public List<Boolean> scriptExists(List<String> sha1s) {
    return checkAndBroadcastCommand(commandObjects.scriptExists(sha1s));
  }

  @Override
  public Boolean scriptExists(String sha1, String sampleKey) {
    return scriptExists(sampleKey, new String[] { sha1 }).get(0);
  }

  @Override
  public List<Boolean> scriptExists(String sampleKey, String... sha1s) {
    return executeCommand(commandObjects.scriptExists(sampleKey, sha1s));
  }

  @Override
  public Boolean scriptExists(byte[] sha1, byte[] sampleKey) {
    return scriptExists(sampleKey, new byte[][] { sha1 }).get(0);
  }

  @Override
  public List<Boolean> scriptExists(byte[] sampleKey, byte[]... sha1s) {
    return executeCommand(commandObjects.scriptExists(sampleKey, sha1s));
  }

  public String scriptLoad(String script) {
    return checkAndBroadcastCommand(commandObjects.scriptLoad(script));
  }

  @Override
  public String scriptLoad(String script, String sampleKey) {
    return executeCommand(commandObjects.scriptLoad(script, sampleKey));
  }

  public String scriptFlush() {
    return checkAndBroadcastCommand(commandObjects.scriptFlush());
  }

  @Override
  public String scriptFlush(String sampleKey) {
    return executeCommand(commandObjects.scriptFlush(sampleKey));
  }

  @Override
  public String scriptFlush(String sampleKey, FlushMode flushMode) {
    return executeCommand(commandObjects.scriptFlush(sampleKey, flushMode));
  }

  public String scriptKill() {
    return checkAndBroadcastCommand(commandObjects.scriptKill());
  }

  @Override
  public String scriptKill(String sampleKey) {
    return executeCommand(commandObjects.scriptKill(sampleKey));
  }

  @Override
  public byte[] scriptLoad(byte[] script, byte[] sampleKey) {
    return executeCommand(commandObjects.scriptLoad(script, sampleKey));
  }

  @Override
  public String scriptFlush(byte[] sampleKey) {
    return executeCommand(commandObjects.scriptFlush(sampleKey));
  }

  @Override
  public String scriptFlush(byte[] sampleKey, FlushMode flushMode) {
    return executeCommand(commandObjects.scriptFlush(sampleKey, flushMode));
  }

  @Override
  public String scriptKill(byte[] sampleKey) {
    return executeCommand(commandObjects.scriptKill(sampleKey));
  }

  public String slowlogReset() {
    return checkAndBroadcastCommand(commandObjects.slowlogReset());
  }
  // Sample key commands

  // Random node commands
  public long publish(String channel, String message) {
    return executeCommand(commandObjects.publish(channel, message));
  }

  public long publish(byte[] channel, byte[] message) {
    return executeCommand(commandObjects.publish(channel, message));
  }

  public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    try (Connection connection = this.provider.getConnection()) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  public void psubscribe(final JedisPubSub jedisPubSub, final String... patterns) {
    try (Connection connection = this.provider.getConnection()) {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    }
  }

  public void subscribe(BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    try (Connection connection = this.provider.getConnection()) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  public void psubscribe(BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    try (Connection connection = this.provider.getConnection()) {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    }
  }
  // Random node commands

  // RediSearch commands
  public long hsetObject(String key, String field, Object value) {
    return executeCommand(commandObjects.hsetObject(key, field, value));
  }

  public long hsetObject(String key, Map<String, Object> hash) {
    return executeCommand(commandObjects.hsetObject(key, hash));
  }

  @Override
  public String ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    return checkAndBroadcastCommand(commandObjects.ftCreate(indexName, indexOptions, schema));
  }

  @Override
  public String ftCreate(String indexName, FTCreateParams createParams, Iterable<SchemaField> schemaFields) {
    return checkAndBroadcastCommand(commandObjects.ftCreate(indexName, createParams, schemaFields));
  }

  @Override
  public String ftAlter(String indexName, Schema schema) {
    return checkAndBroadcastCommand(commandObjects.ftAlter(indexName, schema));
  }

  @Override
  public String ftAlter(String indexName, Iterable<SchemaField> schemaFields) {
    return checkAndBroadcastCommand(commandObjects.ftAlter(indexName, schemaFields));
  }

  @Override
  public String ftAliasAdd(String aliasName, String indexName) {
    return checkAndBroadcastCommand(commandObjects.ftAliasAdd(aliasName, indexName));
  }

  @Override
  public String ftAliasUpdate(String aliasName, String indexName) {
    return checkAndBroadcastCommand(commandObjects.ftAliasUpdate(aliasName, indexName));
  }

  @Override
  public String ftAliasDel(String aliasName) {
    return checkAndBroadcastCommand(commandObjects.ftAliasDel(aliasName));
  }

  @Override
  public String ftDropIndex(String indexName) {
    return checkAndBroadcastCommand(commandObjects.ftDropIndex(indexName));
  }

  @Override
  public String ftDropIndexDD(String indexName) {
    return checkAndBroadcastCommand(commandObjects.ftDropIndexDD(indexName));
  }

  @Override
  public SearchResult ftSearch(String indexName, String query) {
    return executeCommand(commandObjects.ftSearch(indexName, query));
  }

  @Override
  public SearchResult ftSearch(String indexName, String query, FTSearchParams params) {
    return executeCommand(commandObjects.ftSearch(indexName, query, params));
  }

  /**
   * {@link FTSearchParams#limit(int, int)} will be ignored.
   * 
   * @param batchSize batch size
   * @param indexName index name
   * @param query query
   * @param params limit will be ignored
   * @return search iteration
   */
  public FtSearchIteration ftSearchIteration(int batchSize, String indexName, String query, FTSearchParams params) {
    return new FtSearchIteration(provider, commandObjects.getProtocol(), batchSize, indexName, query, params);
  }

  @Override
  public SearchResult ftSearch(String indexName, Query query) {
    return executeCommand(commandObjects.ftSearch(indexName, query));
  }

  /**
   * {@link Query#limit(java.lang.Integer, java.lang.Integer)} will be ignored.
   * @param batchSize batch size
   * @param indexName index name
   * @param query limit will be ignored
   * @return search iteration
   */
  public FtSearchIteration ftSearchIteration(int batchSize, String indexName, Query query) {
    return new FtSearchIteration(provider, commandObjects.getProtocol(), batchSize, indexName, query);
  }

  @Override
  @Deprecated
  public SearchResult ftSearch(byte[] indexName, Query query) {
    return executeCommand(commandObjects.ftSearch(indexName, query));
  }

  @Override
  public String ftExplain(String indexName, Query query) {
    return executeCommand(commandObjects.ftExplain(indexName, query));
  }

  @Override
  public List<String> ftExplainCLI(String indexName, Query query) {
    return executeCommand(commandObjects.ftExplainCLI(indexName, query));
  }

  @Override
  public AggregationResult ftAggregate(String indexName, AggregationBuilder aggr) {
    return executeCommand(commandObjects.ftAggregate(indexName, aggr));
  }

  @Override
  public AggregationResult ftCursorRead(String indexName, long cursorId, int count) {
    return executeCommand(commandObjects.ftCursorRead(indexName, cursorId, count));
  }

  @Override
  public String ftCursorDel(String indexName, long cursorId) {
    return executeCommand(commandObjects.ftCursorDel(indexName, cursorId));
  }

  /**
   * {@link AggregationBuilder#cursor(int, long) CURSOR} must be set.
   * @param indexName index name
   * @param aggr cursor must be set
   * @return aggregate iteration
   */
  public FtAggregateIteration ftAggregateIteration(String indexName, AggregationBuilder aggr) {
    return new FtAggregateIteration(provider, indexName, aggr);
  }

  @Override
  public Map.Entry<AggregationResult, Map<String, Object>> ftProfileAggregate(String indexName,
      FTProfileParams profileParams, AggregationBuilder aggr) {
    return executeCommand(commandObjects.ftProfileAggregate(indexName, profileParams, aggr));
  }

  @Override
  public Map.Entry<SearchResult, Map<String, Object>> ftProfileSearch(String indexName,
      FTProfileParams profileParams, Query query) {
    return executeCommand(commandObjects.ftProfileSearch(indexName, profileParams, query));
  }

  @Override
  public Map.Entry<SearchResult, Map<String, Object>> ftProfileSearch(String indexName,
      FTProfileParams profileParams, String query, FTSearchParams searchParams) {
    return executeCommand(commandObjects.ftProfileSearch(indexName, profileParams, query, searchParams));
  }

  @Override
  public String ftSynUpdate(String indexName, String synonymGroupId, String... terms) {
    return executeCommand(commandObjects.ftSynUpdate(indexName, synonymGroupId, terms));
  }

  @Override
  public Map<String, List<String>> ftSynDump(String indexName) {
    return executeCommand(commandObjects.ftSynDump(indexName));
  }

  @Override
  public long ftDictAdd(String dictionary, String... terms) {
    return executeCommand(commandObjects.ftDictAdd(dictionary, terms));
  }

  @Override
  public long ftDictDel(String dictionary, String... terms) {
    return executeCommand(commandObjects.ftDictDel(dictionary, terms));
  }

  @Override
  public Set<String> ftDictDump(String dictionary) {
    return executeCommand(commandObjects.ftDictDump(dictionary));
  }

  @Override
  public long ftDictAddBySampleKey(String indexName, String dictionary, String... terms) {
    return executeCommand(commandObjects.ftDictAddBySampleKey(indexName, dictionary, terms));
  }

  @Override
  public long ftDictDelBySampleKey(String indexName, String dictionary, String... terms) {
    return executeCommand(commandObjects.ftDictDelBySampleKey(indexName, dictionary, terms));
  }

  @Override
  public Set<String> ftDictDumpBySampleKey(String indexName, String dictionary) {
    return executeCommand(commandObjects.ftDictDumpBySampleKey(indexName, dictionary));
  }

  @Override
  public Map<String, Map<String, Double>> ftSpellCheck(String index, String query) {
    return executeCommand(commandObjects.ftSpellCheck(index, query));
  }

  @Override
  public Map<String, Map<String, Double>> ftSpellCheck(String index, String query,
      FTSpellCheckParams spellCheckParams) {
    return executeCommand(commandObjects.ftSpellCheck(index, query, spellCheckParams));
  }

  @Override
  public Map<String, Object> ftInfo(String indexName) {
    return executeCommand(commandObjects.ftInfo(indexName));
  }

  @Override
  public Set<String> ftTagVals(String indexName, String fieldName) {
    return executeCommand(commandObjects.ftTagVals(indexName, fieldName));
  }

  @Override
  public Map<String, Object> ftConfigGet(String option) {
    return executeCommand(commandObjects.ftConfigGet(option));
  }

  @Override
  public Map<String, Object> ftConfigGet(String indexName, String option) {
    return executeCommand(commandObjects.ftConfigGet(indexName, option));
  }

  @Override
  public String ftConfigSet(String option, String value) {
    return executeCommand(commandObjects.ftConfigSet(option, value));
  }

  @Override
  public String ftConfigSet(String indexName, String option, String value) {
    return executeCommand(commandObjects.ftConfigSet(indexName, option, value));
  }

  @Override
  public long ftSugAdd(String key, String string, double score) {
    return executeCommand(commandObjects.ftSugAdd(key, string, score));
  }

  @Override
  public long ftSugAddIncr(String key, String string, double score) {
    return executeCommand(commandObjects.ftSugAddIncr(key, string, score));
  }

  @Override
  public List<String> ftSugGet(String key, String prefix) {
    return executeCommand(commandObjects.ftSugGet(key, prefix));
  }

  @Override
  public List<String> ftSugGet(String key, String prefix, boolean fuzzy, int max) {
    return executeCommand(commandObjects.ftSugGet(key, prefix, fuzzy, max));
  }

  @Override
  public List<Tuple> ftSugGetWithScores(String key, String prefix) {
    return executeCommand(commandObjects.ftSugGetWithScores(key, prefix));
  }

  @Override
  public List<Tuple> ftSugGetWithScores(String key, String prefix, boolean fuzzy, int max) {
    return executeCommand(commandObjects.ftSugGetWithScores(key, prefix, fuzzy, max));
  }

  @Override
  public boolean ftSugDel(String key, String string) {
    return executeCommand(commandObjects.ftSugDel(key, string));
  }

  @Override
  public long ftSugLen(String key) {
    return executeCommand(commandObjects.ftSugLen(key));
  }

  @Override
  public Set<String> ftList() {
    return executeCommand(commandObjects.ftList());
  }
  // RediSearch commands

  // RedisJSON commands
  @Override
  public String jsonSet(String key, Path2 path, Object object) {
    return executeCommand(commandObjects.jsonSet(key, path, object));
  }

  @Override
  public String jsonSetWithEscape(String key, Path2 path, Object object) {
    return executeCommand(commandObjects.jsonSetWithEscape(key, path, object));
  }

  @Override
  @Deprecated
  public String jsonSet(String key, Path path, Object pojo) {
    return executeCommand(commandObjects.jsonSet(key, path, pojo));
  }

  @Override
  @Deprecated
  public String jsonSetWithPlainString(String key, Path path, String string) {
    return executeCommand(commandObjects.jsonSetWithPlainString(key, path, string));
  }

  @Override
  public String jsonSet(String key, Path2 path, Object pojo, JsonSetParams params) {
    return executeCommand(commandObjects.jsonSet(key, path, pojo, params));
  }

  @Override
  public String jsonSetWithEscape(String key, Path2 path, Object pojo, JsonSetParams params) {
    return executeCommand(commandObjects.jsonSetWithEscape(key, path, pojo, params));
  }

  @Override
  @Deprecated
  public String jsonSet(String key, Path path, Object pojo, JsonSetParams params) {
    return executeCommand(commandObjects.jsonSet(key, path, pojo, params));
  }

  @Override
  public String jsonMerge(String key, Path2 path, Object object) {
    return executeCommand(commandObjects.jsonMerge(key, path, object));
  }

  @Override
  @Deprecated
  public String jsonMerge(String key, Path path, Object pojo) {
    return executeCommand(commandObjects.jsonMerge(key, path, pojo));
  }

  @Override
  public Object jsonGet(String key) {
    return executeCommand(commandObjects.jsonGet(key));
  }

  @Override
  @Deprecated
  public <T> T jsonGet(String key, Class<T> clazz) {
    return executeCommand(commandObjects.jsonGet(key, clazz));
  }

  @Override
  public Object jsonGet(String key, Path2... paths) {
    return executeCommand(commandObjects.jsonGet(key, paths));
  }

  @Override
  @Deprecated
  public Object jsonGet(String key, Path... paths) {
    return executeCommand(commandObjects.jsonGet(key, paths));
  }

  @Override
  @Deprecated
  public String jsonGetAsPlainString(String key, Path path) {
    return executeCommand(commandObjects.jsonGetAsPlainString(key, path));
  }

  @Override
  @Deprecated
  public <T> T jsonGet(String key, Class<T> clazz, Path... paths) {
    return executeCommand(commandObjects.jsonGet(key, clazz, paths));
  }

  @Override
  public List<JSONArray> jsonMGet(Path2 path, String... keys) {
    return executeCommand(commandObjects.jsonMGet(path, keys));
  }

  @Override
  @Deprecated
  public <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys) {
    return executeCommand(commandObjects.jsonMGet(path, clazz, keys));
  }

  @Override
  public long jsonDel(String key) {
    return executeCommand(commandObjects.jsonDel(key));
  }

  @Override
  public long jsonDel(String key, Path2 path) {
    return executeCommand(commandObjects.jsonDel(key, path));
  }

  @Override
  @Deprecated
  public long jsonDel(String key, Path path) {
    return executeCommand(commandObjects.jsonDel(key, path));
  }

  @Override
  public long jsonClear(String key) {
    return executeCommand(commandObjects.jsonClear(key));
  }

  @Override
  public long jsonClear(String key, Path2 path) {
    return executeCommand(commandObjects.jsonClear(key, path));
  }

  @Override
  @Deprecated
  public long jsonClear(String key, Path path) {
    return executeCommand(commandObjects.jsonClear(key, path));
  }

  @Override
  public List<Boolean> jsonToggle(String key, Path2 path) {
    return executeCommand(commandObjects.jsonToggle(key, path));
  }

  @Override
  @Deprecated
  public String jsonToggle(String key, Path path) {
    return executeCommand(commandObjects.jsonToggle(key, path));
  }

  @Override
  @Deprecated
  public Class<?> jsonType(String key) {
    return executeCommand(commandObjects.jsonType(key));
  }

  @Override
  public List<Class<?>> jsonType(String key, Path2 path) {
    return executeCommand(commandObjects.jsonType(key, path));
  }

  @Override
  @Deprecated
  public Class<?> jsonType(String key, Path path) {
    return executeCommand(commandObjects.jsonType(key, path));
  }

  @Override
  @Deprecated
  public long jsonStrAppend(String key, Object string) {
    return executeCommand(commandObjects.jsonStrAppend(key, string));
  }

  @Override
  public List<Long> jsonStrAppend(String key, Path2 path, Object string) {
    return executeCommand(commandObjects.jsonStrAppend(key, path, string));
  }

  @Override
  @Deprecated
  public long jsonStrAppend(String key, Path path, Object string) {
    return executeCommand(commandObjects.jsonStrAppend(key, path, string));
  }

  @Override
  @Deprecated
  public Long jsonStrLen(String key) {
    return executeCommand(commandObjects.jsonStrLen(key));
  }

  @Override
  public List<Long> jsonStrLen(String key, Path2 path) {
    return executeCommand(commandObjects.jsonStrLen(key, path));
  }

  @Override
  @Deprecated
  public Long jsonStrLen(String key, Path path) {
    return executeCommand(commandObjects.jsonStrLen(key, path));
  }

  @Override
  public Object jsonNumIncrBy(String key, Path2 path, double value) {
    return executeCommand(commandObjects.jsonNumIncrBy(key, path, value));
  }

  @Override
  @Deprecated
  public double jsonNumIncrBy(String key, Path path, double value) {
    return executeCommand(commandObjects.jsonNumIncrBy(key, path, value));
  }

  @Override
  public List<Long> jsonArrAppend(String key, Path2 path, Object... objects) {
    return executeCommand(commandObjects.jsonArrAppend(key, path, objects));
  }

  @Override
  public List<Long> jsonArrAppendWithEscape(String key, Path2 path, Object... objects) {
    return executeCommand(commandObjects.jsonArrAppendWithEscape(key, path, objects));
  }

  @Override
  @Deprecated
  public Long jsonArrAppend(String key, Path path, Object... pojos) {
    return executeCommand(commandObjects.jsonArrAppend(key, path, pojos));
  }

  @Override
  public List<Long> jsonArrIndex(String key, Path2 path, Object scalar) {
    return executeCommand(commandObjects.jsonArrIndex(key, path, scalar));
  }

  @Override
  public List<Long> jsonArrIndexWithEscape(String key, Path2 path, Object scalar) {
    return executeCommand(commandObjects.jsonArrIndexWithEscape(key, path, scalar));
  }

  @Override
  @Deprecated
  public long jsonArrIndex(String key, Path path, Object scalar) {
    return executeCommand(commandObjects.jsonArrIndex(key, path, scalar));
  }

  @Override
  public List<Long> jsonArrInsert(String key, Path2 path, int index, Object... objects) {
    return executeCommand(commandObjects.jsonArrInsert(key, path, index, objects));
  }

  @Override
  public List<Long> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects) {
    return executeCommand(commandObjects.jsonArrInsertWithEscape(key, path, index, objects));
  }

  @Override
  @Deprecated
  public long jsonArrInsert(String key, Path path, int index, Object... pojos) {
    return executeCommand(commandObjects.jsonArrInsert(key, path, index, pojos));
  }

  @Override
  @Deprecated
  public Object jsonArrPop(String key) {
    return executeCommand(commandObjects.jsonArrPop(key));
  }

  @Override
  @Deprecated
  public <T> T jsonArrPop(String key, Class<T> clazz) {
    return executeCommand(commandObjects.jsonArrPop(key, clazz));
  }

  @Override
  public List<Object> jsonArrPop(String key, Path2 path) {
    return executeCommand(commandObjects.jsonArrPop(key, path));
  }

  @Override
  @Deprecated
  public Object jsonArrPop(String key, Path path) {
    return executeCommand(commandObjects.jsonArrPop(key, path));
  }

  @Override
  @Deprecated
  public <T> T jsonArrPop(String key, Class<T> clazz, Path path) {
    return executeCommand(commandObjects.jsonArrPop(key, clazz, path));
  }

  @Override
  public List<Object> jsonArrPop(String key, Path2 path, int index) {
    return executeCommand(commandObjects.jsonArrPop(key, path, index));
  }

  @Override
  @Deprecated
  public Object jsonArrPop(String key, Path path, int index) {
    return executeCommand(commandObjects.jsonArrPop(key, path, index));
  }

  @Override
  @Deprecated
  public <T> T jsonArrPop(String key, Class<T> clazz, Path path, int index) {
    return executeCommand(commandObjects.jsonArrPop(key, clazz, path, index));
  }

  @Override
  @Deprecated
  public Long jsonArrLen(String key) {
    return executeCommand(commandObjects.jsonArrLen(key));
  }

  @Override
  public List<Long> jsonArrLen(String key, Path2 path) {
    return executeCommand(commandObjects.jsonArrLen(key, path));
  }

  @Override
  @Deprecated
  public Long jsonArrLen(String key, Path path) {
    return executeCommand(commandObjects.jsonArrLen(key, path));
  }

  @Override
  public List<Long> jsonArrTrim(String key, Path2 path, int start, int stop) {
    return executeCommand(commandObjects.jsonArrTrim(key, path, start, stop));
  }

  @Override
  @Deprecated
  public Long jsonArrTrim(String key, Path path, int start, int stop) {
    return executeCommand(commandObjects.jsonArrTrim(key, path, start, stop));
  }

  @Override
  @Deprecated
  public Long jsonObjLen(String key) {
    return executeCommand(commandObjects.jsonObjLen(key));
  }

  @Override
  @Deprecated
  public Long jsonObjLen(String key, Path path) {
    return executeCommand(commandObjects.jsonObjLen(key, path));
  }

  @Override
  public List<Long> jsonObjLen(String key, Path2 path) {
    return executeCommand(commandObjects.jsonObjLen(key, path));
  }

  @Override
  @Deprecated
  public List<String> jsonObjKeys(String key) {
    return executeCommand(commandObjects.jsonObjKeys(key));
  }

  @Override
  @Deprecated
  public List<String> jsonObjKeys(String key, Path path) {
    return executeCommand(commandObjects.jsonObjKeys(key, path));
  }

  @Override
  public List<List<String>> jsonObjKeys(String key, Path2 path) {
    return executeCommand(commandObjects.jsonObjKeys(key, path));
  }

  @Override
  @Deprecated
  public long jsonDebugMemory(String key) {
    return executeCommand(commandObjects.jsonDebugMemory(key));
  }

  @Override
  @Deprecated
  public long jsonDebugMemory(String key, Path path) {
    return executeCommand(commandObjects.jsonDebugMemory(key, path));
  }

  @Override
  public List<Long> jsonDebugMemory(String key, Path2 path) {
    return executeCommand(commandObjects.jsonDebugMemory(key, path));
  }
  // RedisJSON commands

  // RedisTimeSeries commands
  @Override
  public String tsCreate(String key) {
    return executeCommand(commandObjects.tsCreate(key));
  }

  @Override
  public String tsCreate(String key, TSCreateParams createParams) {
    return executeCommand(commandObjects.tsCreate(key, createParams));
  }

  @Override
  public long tsDel(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(commandObjects.tsDel(key, fromTimestamp, toTimestamp));
  }

  @Override
  public String tsAlter(String key, TSAlterParams alterParams) {
    return executeCommand(commandObjects.tsAlter(key, alterParams));
  }

  @Override
  public long tsAdd(String key, double value) {
    return executeCommand(commandObjects.tsAdd(key, value));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value) {
    return executeCommand(commandObjects.tsAdd(key, timestamp, value));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value, TSCreateParams createParams) {
    return executeCommand(commandObjects.tsAdd(key, timestamp, value, createParams));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value, TSAddParams addParams) {
    return executeCommand(commandObjects.tsAdd(key, timestamp, value, addParams));
  }

  @Override
  public List<Long> tsMAdd(Map.Entry<String, TSElement>... entries) {
    return executeCommand(commandObjects.tsMAdd(entries));
  }

  @Override
  public long tsIncrBy(String key, double value) {
    return executeCommand(commandObjects.tsIncrBy(key, value));
  }

  @Override
  public long tsIncrBy(String key, double value, long timestamp) {
    return executeCommand(commandObjects.tsIncrBy(key, value, timestamp));
  }

  @Override
  public long tsIncrBy(String key, double addend, TSIncrByParams incrByParams) {
    return executeCommand(commandObjects.tsIncrBy(key, addend, incrByParams));
  }

  @Override
  public long tsDecrBy(String key, double value) {
    return executeCommand(commandObjects.tsDecrBy(key, value));
  }

  @Override
  public long tsDecrBy(String key, double value, long timestamp) {
    return executeCommand(commandObjects.tsDecrBy(key, value, timestamp));
  }

  @Override
  public long tsDecrBy(String key, double subtrahend, TSDecrByParams decrByParams) {
    return executeCommand(commandObjects.tsDecrBy(key, subtrahend, decrByParams));
  }

  @Override
  public List<TSElement> tsRange(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(commandObjects.tsRange(key, fromTimestamp, toTimestamp));
  }

  @Override
  public List<TSElement> tsRange(String key, TSRangeParams rangeParams) {
    return executeCommand(commandObjects.tsRange(key, rangeParams));
  }

  @Override
  public List<TSElement> tsRevRange(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(commandObjects.tsRevRange(key, fromTimestamp, toTimestamp));
  }

  @Override
  public List<TSElement> tsRevRange(String key, TSRangeParams rangeParams) {
    return executeCommand(commandObjects.tsRevRange(key, rangeParams));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRange(long fromTimestamp, long toTimestamp, String... filters) {
    return executeCommand(commandObjects.tsMRange(fromTimestamp, toTimestamp, filters));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRange(TSMRangeParams multiRangeParams) {
    return executeCommand(commandObjects.tsMRange(multiRangeParams));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRevRange(long fromTimestamp, long toTimestamp, String... filters) {
    return executeCommand(commandObjects.tsMRevRange(fromTimestamp, toTimestamp, filters));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRevRange(TSMRangeParams multiRangeParams) {
    return executeCommand(commandObjects.tsMRevRange(multiRangeParams));
  }

  @Override
  public TSElement tsGet(String key) {
    return executeCommand(commandObjects.tsGet(key));
  }

  @Override
  public TSElement tsGet(String key, TSGetParams getParams) {
    return executeCommand(commandObjects.tsGet(key, getParams));
  }

  @Override
  public Map<String, TSMGetElement> tsMGet(TSMGetParams multiGetParams, String... filters) {
    return executeCommand(commandObjects.tsMGet(multiGetParams, filters));
  }

  @Override
  public String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long timeBucket) {
    return executeCommand(commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, timeBucket));
  }

  @Override
  public String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long bucketDuration, long alignTimestamp) {
    return executeCommand(
        commandObjects.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration, alignTimestamp));
  }

  @Override
  public String tsDeleteRule(String sourceKey, String destKey) {
    return executeCommand(commandObjects.tsDeleteRule(sourceKey, destKey));
  }

  @Override
  public List<String> tsQueryIndex(String... filters) {
    return executeCommand(commandObjects.tsQueryIndex(filters));
  }

  @Override
  public TSInfo tsInfo(String key) {
    return executeCommand(commandObjects.tsInfo(key));
  }

  @Override
  public TSInfo tsInfoDebug(String key) {
    return executeCommand(commandObjects.tsInfoDebug(key));
  }
  // RedisTimeSeries commands

  // RedisBloom commands
  @Override
  public String bfReserve(String key, double errorRate, long capacity) {
    return executeCommand(commandObjects.bfReserve(key, errorRate, capacity));
  }

  @Override
  public String bfReserve(String key, double errorRate, long capacity, BFReserveParams reserveParams) {
    return executeCommand(commandObjects.bfReserve(key, errorRate, capacity, reserveParams));
  }

  @Override
  public boolean bfAdd(String key, String item) {
    return executeCommand(commandObjects.bfAdd(key, item));
  }

  @Override
  public List<Boolean> bfMAdd(String key, String... items) {
    return executeCommand(commandObjects.bfMAdd(key, items));
  }

  @Override
  public List<Boolean> bfInsert(String key, String... items) {
    return executeCommand(commandObjects.bfInsert(key, items));
  }

  @Override
  public List<Boolean> bfInsert(String key, BFInsertParams insertParams, String... items) {
    return executeCommand(commandObjects.bfInsert(key, insertParams, items));
  }

  @Override
  public boolean bfExists(String key, String item) {
    return executeCommand(commandObjects.bfExists(key, item));
  }

  @Override
  public List<Boolean> bfMExists(String key, String... items) {
    return executeCommand(commandObjects.bfMExists(key, items));
  }

  @Override
  public Map.Entry<Long, byte[]> bfScanDump(String key, long iterator) {
    return executeCommand(commandObjects.bfScanDump(key, iterator));
  }

  @Override
  public String bfLoadChunk(String key, long iterator, byte[] data) {
    return executeCommand(commandObjects.bfLoadChunk(key, iterator, data));
  }

  @Override
  public long bfCard(String key) {
    return executeCommand(commandObjects.bfCard(key));
  }

  @Override
  public Map<String, Object> bfInfo(String key) {
    return executeCommand(commandObjects.bfInfo(key));
  }

  @Override
  public String cfReserve(String key, long capacity) {
    return executeCommand(commandObjects.cfReserve(key, capacity));
  }

  @Override
  public String cfReserve(String key, long capacity, CFReserveParams reserveParams) {
    return executeCommand(commandObjects.cfReserve(key, capacity, reserveParams));
  }

  @Override
  public boolean cfAdd(String key, String item) {
    return executeCommand(commandObjects.cfAdd(key, item));
  }

  @Override
  public boolean cfAddNx(String key, String item) {
    return executeCommand(commandObjects.cfAddNx(key, item));
  }

  @Override
  public List<Boolean> cfInsert(String key, String... items) {
    return executeCommand(commandObjects.cfInsert(key, items));
  }

  @Override
  public List<Boolean> cfInsert(String key, CFInsertParams insertParams, String... items) {
    return executeCommand(commandObjects.cfInsert(key, insertParams, items));
  }

  @Override
  public List<Boolean> cfInsertNx(String key, String... items) {
    return executeCommand(commandObjects.cfInsertNx(key, items));
  }

  @Override
  public List<Boolean> cfInsertNx(String key, CFInsertParams insertParams, String... items) {
    return executeCommand(commandObjects.cfInsertNx(key, insertParams, items));
  }

  @Override
  public boolean cfExists(String key, String item) {
    return executeCommand(commandObjects.cfExists(key, item));
  }

  @Override
  public List<Boolean> cfMExists(String key, String... items) {
    return executeCommand(commandObjects.cfMExists(key, items));
  }

  @Override
  public boolean cfDel(String key, String item) {
    return executeCommand(commandObjects.cfDel(key, item));
  }

  @Override
  public long cfCount(String key, String item) {
    return executeCommand(commandObjects.cfCount(key, item));
  }

  @Override
  public Map.Entry<Long, byte[]> cfScanDump(String key, long iterator) {
    return executeCommand(commandObjects.cfScanDump(key, iterator));
  }

  @Override
  public String cfLoadChunk(String key, long iterator, byte[] data) {
    return executeCommand(commandObjects.cfLoadChunk(key, iterator, data));
  }

  @Override
  public Map<String, Object> cfInfo(String key) {
    return executeCommand(commandObjects.cfInfo(key));
  }

  @Override
  public String cmsInitByDim(String key, long width, long depth) {
    return executeCommand(commandObjects.cmsInitByDim(key, width, depth));
  }

  @Override
  public String cmsInitByProb(String key, double error, double probability) {
    return executeCommand(commandObjects.cmsInitByProb(key, error, probability));
  }

  @Override
  public List<Long> cmsIncrBy(String key, Map<String, Long> itemIncrements) {
    return executeCommand(commandObjects.cmsIncrBy(key, itemIncrements));
  }

  @Override
  public List<Long> cmsQuery(String key, String... items) {
    return executeCommand(commandObjects.cmsQuery(key, items));
  }

  @Override
  public String cmsMerge(String destKey, String... keys) {
    return executeCommand(commandObjects.cmsMerge(destKey, keys));
  }

  @Override
  public String cmsMerge(String destKey, Map<String, Long> keysAndWeights) {
    return executeCommand(commandObjects.cmsMerge(destKey, keysAndWeights));
  }

  @Override
  public Map<String, Object> cmsInfo(String key) {
    return executeCommand(commandObjects.cmsInfo(key));
  }

  @Override
  public String topkReserve(String key, long topk) {
    return executeCommand(commandObjects.topkReserve(key, topk));
  }

  @Override
  public String topkReserve(String key, long topk, long width, long depth, double decay) {
    return executeCommand(commandObjects.topkReserve(key, topk, width, depth, decay));
  }

  @Override
  public List<String> topkAdd(String key, String... items) {
    return executeCommand(commandObjects.topkAdd(key, items));
  }

  @Override
  public List<String> topkIncrBy(String key, Map<String, Long> itemIncrements) {
    return executeCommand(commandObjects.topkIncrBy(key, itemIncrements));
  }

  @Override
  public List<Boolean> topkQuery(String key, String... items) {
    return executeCommand(commandObjects.topkQuery(key, items));
  }

  @Override
  public List<String> topkList(String key) {
    return executeCommand(commandObjects.topkList(key));
  }

  @Override
  public Map<String, Long> topkListWithCount(String key) {
    return executeCommand(commandObjects.topkListWithCount(key));
  }

  @Override
  public Map<String, Object> topkInfo(String key) {
    return executeCommand(commandObjects.topkInfo(key));
  }

  @Override
  public String tdigestCreate(String key) {
    return executeCommand(commandObjects.tdigestCreate(key));
  }

  @Override
  public String tdigestCreate(String key, int compression) {
    return executeCommand(commandObjects.tdigestCreate(key, compression));
  }

  @Override
  public String tdigestReset(String key) {
    return executeCommand(commandObjects.tdigestReset(key));
  }

  @Override
  public String tdigestMerge(String destinationKey, String... sourceKeys) {
    return executeCommand(commandObjects.tdigestMerge(destinationKey, sourceKeys));
  }

  @Override
  public String tdigestMerge(TDigestMergeParams mergeParams, String destinationKey, String... sourceKeys) {
    return executeCommand(commandObjects.tdigestMerge(mergeParams, destinationKey, sourceKeys));
  }

  @Override
  public Map<String, Object> tdigestInfo(String key) {
    return executeCommand(commandObjects.tdigestInfo(key));
  }

  @Override
  public String tdigestAdd(String key, double... values) {
    return executeCommand(commandObjects.tdigestAdd(key, values));
  }

  @Override
  public List<Double> tdigestCDF(String key, double... values) {
    return executeCommand(commandObjects.tdigestCDF(key, values));
  }

  @Override
  public List<Double> tdigestQuantile(String key, double... quantiles) {
    return executeCommand(commandObjects.tdigestQuantile(key, quantiles));
  }

  @Override
  public double tdigestMin(String key) {
    return executeCommand(commandObjects.tdigestMin(key));
  }

  @Override
  public double tdigestMax(String key) {
    return executeCommand(commandObjects.tdigestMax(key));
  }

  @Override
  public double tdigestTrimmedMean(String key, double lowCutQuantile, double highCutQuantile) {
    return executeCommand(commandObjects.tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile));
  }

  @Override
  public List<Long> tdigestRank(String key, double... values) {
    return executeCommand(commandObjects.tdigestRank(key, values));
  }

  @Override
  public List<Long> tdigestRevRank(String key, double... values) {
    return executeCommand(commandObjects.tdigestRevRank(key, values));
  }

  @Override
  public List<Double> tdigestByRank(String key, long... ranks) {
    return executeCommand(commandObjects.tdigestByRank(key, ranks));
  }

  @Override
  public List<Double> tdigestByRevRank(String key, long... ranks) {
    return executeCommand(commandObjects.tdigestByRevRank(key, ranks));
  }
  // RedisBloom commands

  // RedisGraph commands
  @Override
  @Deprecated
  public ResultSet graphQuery(String name, String query) {
    return executeCommand(graphCommandObjects.graphQuery(name, query));
  }

  @Override
  @Deprecated
  public ResultSet graphReadonlyQuery(String name, String query) {
    return executeCommand(graphCommandObjects.graphReadonlyQuery(name, query));
  }

  @Override
  @Deprecated
  public ResultSet graphQuery(String name, String query, long timeout) {
    return executeCommand(graphCommandObjects.graphQuery(name, query, timeout));
  }

  @Override
  @Deprecated
  public ResultSet graphReadonlyQuery(String name, String query, long timeout) {
    return executeCommand(graphCommandObjects.graphReadonlyQuery(name, query, timeout));
  }

  @Override
  @Deprecated
  public ResultSet graphQuery(String name, String query, Map<String, Object> params) {
    return executeCommand(graphCommandObjects.graphQuery(name, query, params));
  }

  @Override
  @Deprecated
  public ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params) {
    return executeCommand(graphCommandObjects.graphReadonlyQuery(name, query, params));
  }

  @Override
  @Deprecated
  public ResultSet graphQuery(String name, String query, Map<String, Object> params, long timeout) {
    return executeCommand(graphCommandObjects.graphQuery(name, query, params, timeout));
  }

  @Override
  @Deprecated
  public ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout) {
    return executeCommand(graphCommandObjects.graphReadonlyQuery(name, query, params, timeout));
  }

  @Override
  @Deprecated
  public String graphDelete(String name) {
    return executeCommand(graphCommandObjects.graphDelete(name));
  }

  @Override
  @Deprecated
  public List<String> graphList() {
    return executeCommand(commandObjects.graphList());
  }

  @Override
  @Deprecated
  public List<String> graphProfile(String graphName, String query) {
    return executeCommand(commandObjects.graphProfile(graphName, query));
  }

  @Override
  @Deprecated
  public List<String> graphExplain(String graphName, String query) {
    return executeCommand(commandObjects.graphExplain(graphName, query));
  }

  @Override
  @Deprecated
  public List<List<Object>> graphSlowlog(String graphName) {
    return executeCommand(commandObjects.graphSlowlog(graphName));
  }

  @Override
  @Deprecated
  public String graphConfigSet(String configName, Object value) {
    return executeCommand(commandObjects.graphConfigSet(configName, value));
  }

  @Override
  @Deprecated
  public Map<String, Object> graphConfigGet(String configName) {
    return executeCommand(commandObjects.graphConfigGet(configName));
  }
  // RedisGraph commands

  // RedisGears commands
  @Deprecated
  @Override
  public String tFunctionLoad(String libraryCode, TFunctionLoadParams params) {
    return executeCommand(commandObjects.tFunctionLoad(libraryCode, params));
  }

  @Deprecated
  @Override
  public String tFunctionDelete(String libraryName) {
    return executeCommand(commandObjects.tFunctionDelete(libraryName));
  }

  @Deprecated
  @Override
  public List<GearsLibraryInfo> tFunctionList(TFunctionListParams params) {
    return executeCommand(commandObjects.tFunctionList(params));
  }

  @Deprecated
  @Override
  public Object tFunctionCall(String library, String function, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.tFunctionCall(library, function, keys, args));
  }

  @Deprecated
  @Override
  public Object tFunctionCallAsync(String library, String function, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.tFunctionCallAsync(library, function, keys, args));
  }
  // RedisGears commands

  /**
   * @return pipeline object. Use {@link AbstractPipeline} instead of {@link PipelineBase}.
   */
  public PipelineBase pipelined() {
    if (provider == null) {
      throw new IllegalStateException("It is not allowed to create Pipeline from this " + getClass());
    } else if (provider instanceof MultiClusterPooledConnectionProvider) {
      return new MultiClusterPipeline((MultiClusterPooledConnectionProvider) provider, commandObjects);
    } else {
      return new Pipeline(provider.getConnection(), true, commandObjects);
    }
  }

  /**
   * @return transaction object
   */
  public AbstractTransaction multi() {
    return transaction(true);
  }

  /**
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   * @return transaction object
   */
  public AbstractTransaction transaction(boolean doMulti) {
    if (provider == null) {
      throw new IllegalStateException("It is not allowed to create Transaction from this " + getClass());
    } else if (provider instanceof MultiClusterPooledConnectionProvider) {
      return new MultiClusterTransaction((MultiClusterPooledConnectionProvider) provider, doMulti, commandObjects);
    } else {
      return new Transaction(provider.getConnection(), doMulti, true, commandObjects);
    }
  }

  public Object sendCommand(ProtocolCommand cmd) {
    return executeCommand(commandObjects.commandArguments(cmd));
  }

  public Object sendCommand(ProtocolCommand cmd, byte[]... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args));
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, byte[]... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args).blocking());
  }

  public Object sendCommand(ProtocolCommand cmd, String... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args));
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, String... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args).blocking());
  }

  public Object sendCommand(byte[] sampleKey, ProtocolCommand cmd, byte[]... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args).processKey(sampleKey));
  }

  public Object sendBlockingCommand(byte[] sampleKey, ProtocolCommand cmd, byte[]... args) {
    return executeCommand(
        commandObjects.commandArguments(cmd).addObjects((Object[]) args).blocking().processKey(sampleKey));
  }

  public Object sendCommand(String sampleKey, ProtocolCommand cmd, String... args) {
    return executeCommand(commandObjects.commandArguments(cmd).addObjects((Object[]) args).processKey(sampleKey));
  }

  public Object sendBlockingCommand(String sampleKey, ProtocolCommand cmd, String... args) {
    return executeCommand(
        commandObjects.commandArguments(cmd).addObjects((Object[]) args).blocking().processKey(sampleKey));
  }

  public Object executeCommand(CommandArguments args) {
    return executeCommand(new CommandObject<>(args, BuilderFactory.RAW_OBJECT));
  }

  @Experimental
  public void setKeyArgumentPreProcessor(CommandKeyArgumentPreProcessor keyPreProcessor) {
    this.commandObjects.setKeyArgumentPreProcessor(keyPreProcessor);
  }

  public void setJsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
    this.commandObjects.setJsonObjectMapper(jsonObjectMapper);
  }

  public void setDefaultSearchDialect(int dialect) {
    this.commandObjects.setDefaultSearchDialect(dialect);
  }
}
