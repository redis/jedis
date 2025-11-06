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

public class RedisSentinelClient extends UnifiedJedis {

  private RedisSentinelClient(CommandExecutor commandExecutor, ConnectionProvider connectionProvider, CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Fluent builder for {@link RedisSentinelClient} (Redis Sentinel).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  static public class Builder extends SentinelClientBuilder<RedisSentinelClient> {

    @Override
    protected RedisSentinelClient createClient() {
      return new RedisSentinelClient(commandExecutor, connectionProvider, commandObjects, clientConfig.getRedisProtocol(),
          cache);
    }
  }

  /**
   * Create a new builder for configuring RedisSentinelClient instances.
   *
   * @return a new {@link RedisSentinelClient.Builder} instance
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

