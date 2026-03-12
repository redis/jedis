package redis.clients.jedis;

import redis.clients.jedis.builders.SentinelClientBuilder;
import redis.clients.jedis.csc.Cache;

import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;
import redis.clients.jedis.sentinel.api.SentinelInstanceClient;

import java.util.Set;

// @formatter:off
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
 * <b>Relationship to {@code JedisSentineled}:</b></p>
 * <ul>
 * <li>{@code RedisSentinelClient} is the recommended replacement for the deprecated
 * {@code JedisSentineled} class.</li>
 * <li>It offers improved API consistency, better failover handling, and a fluent builder for
 * configuration.</li>
 * <li>Use {@code RedisSentinelClient} for new codebases and when migrating from
 * {@code JedisSentineled}.</li>
 * </ul>
 */
 // @formatter:on
public class RedisSentinelClient extends UnifiedJedis {
  private final SentinelConfiguration sentinelConfiguration;

  private RedisSentinelClient(CommandExecutor commandExecutor,
      ConnectionProvider connectionProvider, CommandObjects commandObjects,
      RedisProtocol redisProtocol, Cache cache, SentinelConfiguration sentinelConfiguration) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
    this.sentinelConfiguration = sentinelConfiguration;
  }

  /**
   * Fluent builder for {@link RedisSentinelClient} (Redis Sentinel).
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  public static class Builder extends SentinelClientBuilder<RedisSentinelClient> {
    private String masterName;
    private Set<HostAndPort> sentinels;
    private JedisClientConfig sentinelClientConfig;

    @Override
    public SentinelClientBuilder<RedisSentinelClient> masterName(String masterName) {
      this.masterName = masterName;
      super.masterName(masterName);
      return this;
    }

    @Override
    public SentinelClientBuilder<RedisSentinelClient> sentinels(Set<HostAndPort> sentinels) {
      this.sentinels = sentinels;
      super.sentinels(sentinels);
      return this;
    }

    @Override
    public SentinelClientBuilder<RedisSentinelClient> sentinelClientConfig(
        JedisClientConfig sentinelClientConfig) {
      this.sentinelClientConfig = sentinelClientConfig;
      super.sentinelClientConfig(sentinelClientConfig);
      return this;
    }

    @Override
    protected RedisSentinelClient createClient() {
      SentinelConfiguration sentinelConfiguration = SentinelConfigurationImpl.builder()
          .masterName(masterName).sentinels(sentinels).sentinelClientConfig(sentinelClientConfig)
          .build();

      return new RedisSentinelClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache, sentinelConfiguration);
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

  /**
   * Returns a connection to the first available Sentinel instance.
   * <p>
   * This method cycles through the configured Sentinel instances and attempts to connect to each
   * one. The first Sentinel that responds to a PING command with PONG is returned as a
   * {@link SentinelInstanceClient}.
   * </p>
   * <p>
   * The returned connection is not managed by the client and should be closed by the caller when no
   * longer needed.
   * </p>
   * @return a {@link SentinelInstanceClient} to the first available Sentinel instance
   * @throws JedisConnectionException if no Sentinel instances are available or all fail to respond
   */
  public SentinelInstanceClient getAvailableSentinel() {
    for (HostAndPort sentinel : sentinelConfiguration.getSentinels()) {
      SentinelInstanceClient client = null;
      try {
        // Create a new connection to the sentinel
        client = RedisSentinelInstanceClient.builder().hostAndPort(sentinel)
            .clientConfig(sentinelConfiguration.getSentinelClientConfig()).build();

        // Test connection with PING
        String response = client.ping();
        if ("PONG".equals(response)) {
          return client;
        }

        client.close();
      } catch (Exception e) {
        if (client != null) {
          try {
            client.close();
          } catch (Exception closeException) {
            // Ignore close exceptions
          }
        }
      }
    }

    // No sentinel was available
    throw new JedisConnectionException(
        "All sentinels are down, cannot connect to any sentinel instance.");
  }
}
