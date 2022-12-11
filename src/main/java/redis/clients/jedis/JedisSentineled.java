package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.SentineledConnectionProvider;

public class JedisSentineled extends UnifiedJedis {

  public JedisSentineled(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masteClientConfig, final JedisClientConfig sentinelClientConfig) {
    this(new SentineledConnectionProvider(masterName, sentinels, masteClientConfig, sentinelClientConfig));
  }

  /**
   * This constructor is here for easier transition from {@link JedisSentinelPool#JedisSentinelPool(
   * java.lang.String, java.util.Set, org.apache.commons.pool2.impl.GenericObjectPoolConfig,
   * redis.clients.jedis.JedisClientConfig, redis.clients.jedis.JedisClientConfig)}.
   *
   * @deprecated Use {@link #JedisSentineled(java.lang.String, java.util.Set,
   *             redis.clients.jedis.JedisClientConfig, redis.clients.jedis.JedisClientConfig,
   *             org.apache.commons.pool2.impl.GenericObjectPoolConfig)}.
   */
  @Deprecated
  // Legacy
  public JedisSentineled(String masterName, Set<HostAndPort> sentinels,
      final GenericObjectPoolConfig<Connection> poolConfig, final JedisClientConfig masterClientConfig,
      final JedisClientConfig sentinelClientConfig) {
    this(masterName, sentinels, masterClientConfig, sentinelClientConfig, poolConfig);
  }

  public JedisSentineled(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(new SentineledConnectionProvider(masterName, sentinels, masterClientConfig, sentinelClientConfig, poolConfig));
  }

  public JedisSentineled(SentineledConnectionProvider sentineledConnectionProvider) {
    super(sentineledConnectionProvider);
  }

  public HostAndPort getCurrentMaster() {
    return ((SentineledConnectionProvider) provider).getCurrentMaster();
  }
}
