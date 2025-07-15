package redis.clients.jedis;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.executors.*;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.commands.RedisJsonV1Commands;
import redis.clients.jedis.mcf.CircuitBreakerCommandExecutor;
import redis.clients.jedis.mcf.MultiClusterPipeline;
import redis.clients.jedis.mcf.MultiClusterTransaction;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.*;
import redis.clients.jedis.search.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisURIHelper;

public class UnifiedJedis extends BaseRedisClient implements RedisJsonV1Commands, AutoCloseable {

  @Deprecated
  protected RedisProtocol protocol = null;
  protected final ConnectionProvider provider;
  protected final CommandExecutor executor;
  protected final CommandObjects commandObjects;
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

  /**
   * Create a new UnifiedJedis with the provided URI and JedisClientConfig object. Note that all fields
   * that can be parsed from the URI will be used instead of the corresponding configuration values. This includes
   * the following fields: user, password, database, protocol version, and whether to use SSL.
   *
   * For example, if the URI is "redis://user:password@localhost:6379/1", the user and password fields will be set
   * to "user" and "password" respectively, the database field will be set to 1. Those fields will be ignored
   * from the JedisClientConfig object.
   *
   * @param uri The URI to connect to
   * @param config The JedisClientConfig object to use
   */
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

  @Override
  protected CommandObjects getCommandObjects() {
    return commandObjects;
  }

  @Override
  protected ConnectionProvider getConnectionProvider() {
    return provider;
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  public Object executeCommand(CommandArguments args) {
    return executeCommand(new CommandObject<>(args, BuilderFactory.RAW_OBJECT));
  }

  @Override
  public final <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executor.broadcastCommand(commandObject);
  }

  @Override
  protected <T> T checkAndBroadcastCommand(CommandObject<T> commandObject) {
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

  // String commands
  /**
   * @deprecated Use {@link UnifiedJedis#setGet(java.lang.String, java.lang.String)}.
   */
  @Deprecated
  public String getSet(String key, String value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  /**
   * @deprecated Use {@link UnifiedJedis#setGet(byte[], byte[])}.
   */
  @Deprecated
  public byte[] getSet(byte[] key, byte[] value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  // Sorted Set commands
  @Deprecated
  public long zdiffStore(String dstkey, String... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Deprecated
  public long zdiffStore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }
  // Sorted Set commands

  // Stream commands
  /**
   * @deprecated As of Jedis 6.1.0, use
   *     {@link #xreadBinary(XReadParams, Map)} or
   *     {@link #xreadBinaryAsMap(XReadParams, Map)} for type safety and better stream entry
   *     parsing.
   */
  @Deprecated
  public List<Object> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(commandObjects.xread(xReadParams, streams));
  }

  /**
   * @deprecated As of Jedis 6.1.0, use
   *     {@link #xreadGroupBinary(byte[], byte[], XReadGroupParams, Map)} or
   *     {@link #xreadGroupBinaryAsMap(byte[], byte[], XReadGroupParams, Map)} instead.
   */
  @Deprecated
  public List<Object> xreadGroup(byte[] groupName, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(
        commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, streams));
  }
  // Stream commands

  // RediSearch commands
  @Deprecated
  public SearchResult ftSearch(byte[] indexName, Query query) {
    return executeCommand(commandObjects.ftSearch(indexName, query));
  }

  @Deprecated
  public Map<String, Object> ftConfigGet(String option) {
    return executeCommand(commandObjects.ftConfigGet(option));
  }

  @Deprecated
  public Map<String, Object> ftConfigGet(String indexName, String option) {
    return executeCommand(commandObjects.ftConfigGet(indexName, option));
  }

  @Deprecated
  public String ftConfigSet(String option, String value) {
    return executeCommand(commandObjects.ftConfigSet(option, value));
  }

  @Deprecated
  public String ftConfigSet(String indexName, String option, String value) {
    return executeCommand(commandObjects.ftConfigSet(indexName, option, value));
  }
  // RediSearch commands

  // RedisJSON commands
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
  @Deprecated
  public String jsonSet(String key, Path path, Object pojo, JsonSetParams params) {
    return executeCommand(commandObjects.jsonSet(key, path, pojo, params));
  }

  @Override
  @Deprecated
  public String jsonMerge(String key, Path path, Object pojo) {
    return executeCommand(commandObjects.jsonMerge(key, path, pojo));
  }

  @Override
  @Deprecated
  public <T> T jsonGet(String key, Class<T> clazz) {
    return executeCommand(commandObjects.jsonGet(key, clazz));
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
  @Deprecated
  public <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys) {
    return executeCommand(commandObjects.jsonMGet(path, clazz, keys));
  }

  @Override
  @Deprecated
  public long jsonDel(String key, Path path) {
    return executeCommand(commandObjects.jsonDel(key, path));
  }

  @Override
  @Deprecated
  public long jsonClear(String key, Path path) {
    return executeCommand(commandObjects.jsonClear(key, path));
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
  @Deprecated
  public Long jsonStrLen(String key, Path path) {
    return executeCommand(commandObjects.jsonStrLen(key, path));
  }

  @Override
  @Deprecated
  public double jsonNumIncrBy(String key, Path path, double value) {
    return executeCommand(commandObjects.jsonNumIncrBy(key, path, value));
  }

  @Override
  @Deprecated
  public Long jsonArrAppend(String key, Path path, Object... pojos) {
    return executeCommand(commandObjects.jsonArrAppend(key, path, pojos));
  }

  @Override
  @Deprecated
  public long jsonArrIndex(String key, Path path, Object scalar) {
    return executeCommand(commandObjects.jsonArrIndex(key, path, scalar));
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
  @Deprecated
  public Long jsonArrLen(String key, Path path) {
    return executeCommand(commandObjects.jsonArrLen(key, path));
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
  @Deprecated
  public long jsonDebugMemory(String key) {
    return executeCommand(commandObjects.jsonDebugMemory(key));
  }

  @Override
  @Deprecated
  public long jsonDebugMemory(String key, Path path) {
    return executeCommand(commandObjects.jsonDebugMemory(key, path));
  }
  // RedisJSON commands

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
