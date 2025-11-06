package redis.clients.jedis;

import redis.clients.jedis.builders.SentinelClientBuilder;
import redis.clients.jedis.csc.Cache;

import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;

/**
 * A high-level client for interacting with Redis Sentinel-managed Redis deployments.
 * <p>
 * {@code RedisSentinelClient} provides robust support for automatic master failover, connection
 * management, and command execution in environments where Redis Sentinel is used to monitor and
 * manage Redis servers.
 * </p>
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * RedisSentinelClient client = RedisSentinelClient.builder().sentinel("localhost", 26379)
 *     .masterName("mymaster").build();
 * </pre>
 * <p>
 * <b>Relationship to {@code JedisSentineled}:</b>
 * <ul>
 * <li>{@code RedisSentinelClient} is the recommended replacement for the deprecated
 * {@code JedisSentineled} class.</li>
 * <li>It offers improved API consistency, better failover handling, and a fluent builder for
 * configuration.</li>
 * <li>Use {@code RedisSentinelClient} for new codebases and when migrating from
 * {@code JedisSentineled}.</li>
 * </ul>
 * </p>
 */
public class RedisSentinelClient extends UnifiedJedis {
  private RedisSentinelClient(CommandExecutor commandExecutor,
      ConnectionProvider connectionProvider, CommandObjects commandObjects,
      RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Fluent builder for {@link RedisSentinelClient} (Redis Sentinel).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  public static class Builder extends SentinelClientBuilder<RedisSentinelClient> {

    @Override
    protected RedisSentinelClient createClient() {
      return new RedisSentinelClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache);
    }
  }

  /**
   * Create a new builder for configuring RedisSentinelClient instances.
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
