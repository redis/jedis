package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.SentineledConnectionProvider;

public class JedisSentineled extends UnifiedJedis {

  /**
   * This constructor is here for easier transition from {@link JedisSentinelPool#JedisSentinelPool(
   * java.lang.String, java.util.Set, redis.clients.jedis.JedisClientConfig, redis.clients.jedis.JedisClientConfig)}.
   *
   * @deprecated Use {@link #JedisSentineled(java.lang.String, redis.clients.jedis.JedisClientConfig,
   * java.util.Set, redis.clients.jedis.JedisClientConfig)}.
   */
  @Deprecated
  // Legacy
  public JedisSentineled(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, sentinels, sentinelClientConfig);
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(new SentineledConnectionProvider(masterName, masterClientConfig, sentinels, sentinelClientConfig));
  }

  /**
   * This constructor is here for easier transition from {@link JedisSentinelPool#JedisSentinelPool(
   * java.lang.String, java.util.Set, org.apache.commons.pool2.impl.GenericObjectPoolConfig,
   * redis.clients.jedis.JedisClientConfig, redis.clients.jedis.JedisClientConfig)}.
   *
   * @deprecated Use {@link #JedisSentineled(java.lang.String, redis.clients.jedis.JedisClientConfig,
   * org.apache.commons.pool2.impl.GenericObjectPoolConfig, java.util.Set, redis.clients.jedis.JedisClientConfig)}.
   */
  @Deprecated
  // Legacy
  public JedisSentineled(String masterName, Set<HostAndPort> sentinels,
      final GenericObjectPoolConfig<Connection> poolConfig, final JedisClientConfig masterClientConfig,
      final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig);
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(new SentineledConnectionProvider(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig));
  }

  public JedisSentineled(SentineledConnectionProvider sentineledConnectionProvider) {
    super(sentineledConnectionProvider);
  }

  public HostAndPort getCurrentMaster() {
    return ((SentineledConnectionProvider) provider).getCurrentMaster();
  }
}
