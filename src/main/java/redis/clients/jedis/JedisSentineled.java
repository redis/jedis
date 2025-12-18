package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.builders.SentinelClientBuilder;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;

import java.util.Set;

/**
 * JedisSentineled is a client for Redis Sentinel deployments.
 *
 * @deprecated Use {@link RedisSentinelClient} instead. RedisSentinelClient provides the same functionality
 *             with a cleaner API. Use {@link RedisSentinelClient#builder()} to configure the client
 *             with sentinel settings, master configuration, and connection pooling options.
 */
@Deprecated
public class JedisSentineled extends UnifiedJedis {

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, sentinels, sentinelClientConfig),
        masterClientConfig.getRedisProtocol());
  }

  @Experimental
  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig, CacheConfig cacheConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, CacheFactory.getCache(cacheConfig),
        sentinels, sentinelClientConfig);
  }

  @Experimental
  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig, Cache clientSideCache,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, clientSideCache,
        sentinels, sentinelClientConfig), masterClientConfig.getRedisProtocol(), clientSideCache);
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig),
        masterClientConfig.getRedisProtocol());
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
                         final GenericObjectPoolConfig<Connection> poolConfig,
                         Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig, ReadFrom readFrom) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig, readFrom),
            masterClientConfig.getRedisProtocol());
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
                         final GenericObjectPoolConfig<Connection> poolConfig,
                         Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig, ReadFrom readFrom,
                         ReadOnlyPredicate readOnlyPredicate) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig, readFrom, readOnlyPredicate),
            masterClientConfig.getRedisProtocol());
  }

  @Experimental
  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig, Cache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, clientSideCache, poolConfig,
        sentinels, sentinelClientConfig), masterClientConfig.getRedisProtocol(), clientSideCache);
  }

  public JedisSentineled(SentineledConnectionProvider sentineledConnectionProvider) {
    super(sentineledConnectionProvider);
  }

  private JedisSentineled(CommandExecutor commandExecutor, ConnectionProvider connectionProvider, CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Fluent builder for {@link JedisSentineled} (Redis Sentinel).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  static public class Builder extends SentinelClientBuilder<JedisSentineled> {

    @Override
    protected JedisSentineled createClient() {
      return new JedisSentineled(commandExecutor, connectionProvider, commandObjects, clientConfig.getRedisProtocol(),
          cache);
    }
  }

  /**
   * Create a new builder for configuring JedisSentineled instances.
   *
   * @return a new {@link JedisSentineled.Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public HostAndPort getCurrentMaster() {
    return ((SentineledConnectionProvider) provider).getCurrentMaster();
  }

  @Override
  public Pipeline pipelined() {
    return (Pipeline) super.pipelined();
  }
}
