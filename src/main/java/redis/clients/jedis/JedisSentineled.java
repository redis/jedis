package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.providers.SentineledConnectionProvider;

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

  public HostAndPort getCurrentMaster() {
    return ((SentineledConnectionProvider) provider).getCurrentMaster();
  }

  @Override
  public Pipeline pipelined() {
    return (Pipeline) super.pipelined();
  }
}
