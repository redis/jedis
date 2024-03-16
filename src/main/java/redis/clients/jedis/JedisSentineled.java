package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.providers.SentineledConnectionProvider;

public class JedisSentineled extends UnifiedJedis {

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, sentinels, sentinelClientConfig),
        masterClientConfig.getRedisProtocol());
  }

  public JedisSentineled(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    super(new SentineledConnectionProvider(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig),
        masterClientConfig.getRedisProtocol());
  }

  public JedisSentineled(SentineledConnectionProvider sentineledConnectionProvider) {
    super(sentineledConnectionProvider);
  }

  /**
   * Constructor that allows CommandObjects to be customized. The RedisProtocol specified will be written into the given
   * CommandObjects.
   *
   * @param provider The SentineledConnectionProvider.
   * @param commandObjects The CommandObjects.
   * @param redisProtocol The RedisProtocol that will be written into the given CommandObjects.
   */
  public JedisSentineled(SentineledConnectionProvider provider, CommandObjects commandObjects, RedisProtocol redisProtocol) {
    super(new DefaultCommandExecutor(provider), provider, commandObjects, redisProtocol);
  }

  public HostAndPort getCurrentMaster() {
    return ((SentineledConnectionProvider) provider).getCurrentMaster();
  }

  @Override
  public Pipeline pipelined() {
    return (Pipeline) super.pipelined();
  }
}
