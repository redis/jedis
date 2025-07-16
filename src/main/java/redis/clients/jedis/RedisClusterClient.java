package redis.clients.jedis;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.commands.ClusterCommands;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisClusterCRC16;

/**
 * Redis Cluster client that provides a clean, developer-friendly API for Redis Cluster operations.
 * <p>
 * This client extends {@link BaseRedisClient} to inherit all Redis command implementations and
 * provides a Builder pattern for configuration. It uses {@link ClusterConnectionProvider} for
 * cluster topology management and automatic slot-based routing.
 * <p>
 * Key features:
 * <ul>
 * <li>Automatic cluster topology discovery and management</li>
 * <li>Slot-based command routing with automatic retries</li>
 * <li>Builder pattern for complex configuration</li>
 * <li>Automatic connection pooling and resource management</li>
 * <li>Full Redis command support including modules</li>
 * <li>Cluster-specific operations (CLUSTER commands)</li>
 * <li>Pipeline support with cluster awareness</li>
 * </ul>
 * <p>
 * Basic usage:
 *
 * <pre>{@code
 * // Simple cluster connection
 * Set<HostAndPort> nodes = Set.of(new HostAndPort("node1", 7000), new HostAndPort("node2", 7001),
 *   new HostAndPort("node3", 7002));
 * try (RedisClusterClient client = new RedisClusterClient(nodes)) {
 *   client.set("key", "value");
 *   String value = client.get("key");
 * }
 *
 * // With configuration
 * JedisClientConfig config = DefaultJedisClientConfig.builder().password("secret").build();
 * try (RedisClusterClient client = RedisClusterClient.builder().nodes(nodes).clientConfig(config)
 *     .maxAttempts(5).maxTotalRetriesDuration(Duration.ofSeconds(10)).build()) {
 *   client.set("key", "value");
 * }
 * }</pre>
 *
 * @see BaseRedisClient
 * @see ClusterConnectionProvider
 * @see ClusterCommands
 * @see Builder
 */
public class RedisClusterClient extends BaseRedisClient implements ClusterCommands, AutoCloseable {

  private final ClusterCommandExecutor executor;
  private final ClusterConnectionProvider provider;
  private final ClusterCommandObjects commandObjects;
  private final Cache cache;

  /**
   * Creates a RedisClusterClient with default configuration.
   * @param nodes the cluster nodes to connect to
   */
  public RedisClusterClient(Set<HostAndPort> nodes) {
    this(builder().nodes(nodes));
  }

  /**
   * Creates a RedisClusterClient with custom client configuration.
   * @param nodes the cluster nodes to connect to
   * @param clientConfig the client configuration
   */
  public RedisClusterClient(Set<HostAndPort> nodes, JedisClientConfig clientConfig) {
    this(builder().nodes(nodes).clientConfig(clientConfig));
  }

  /**
   * Package-private constructor for builder.
   */
  RedisClusterClient(Builder builder) {
    // Use custom connection provider if provided, otherwise create default cluster provider
    if (builder.connectionProvider != null) {
      if (!(builder.connectionProvider instanceof ClusterConnectionProvider)) {
        throw new IllegalArgumentException(
            "ConnectionProvider must be a ClusterConnectionProvider");
      }
      this.provider = (ClusterConnectionProvider) builder.connectionProvider;
    } else {
      if (builder.nodes == null || builder.nodes.isEmpty()) {
        throw new IllegalArgumentException("Cluster nodes must be provided");
      }

      if (builder.cache != null) {
        if (builder.topologyRefreshPeriod != null) {
          this.provider = new ClusterConnectionProvider(builder.nodes, builder.clientConfig,
              builder.cache, builder.poolConfig, builder.topologyRefreshPeriod);
        } else {
          this.provider = new ClusterConnectionProvider(builder.nodes, builder.clientConfig,
              builder.cache, builder.poolConfig);
        }
      } else {
        if (builder.topologyRefreshPeriod != null) {
          this.provider = new ClusterConnectionProvider(builder.nodes, builder.clientConfig,
              builder.poolConfig, builder.topologyRefreshPeriod);
        } else {
          this.provider = new ClusterConnectionProvider(builder.nodes, builder.clientConfig,
              builder.poolConfig);
        }
      }
    }

    this.executor = new ClusterCommandExecutor(provider, builder.maxAttempts,
        builder.maxTotalRetriesDuration);
    this.commandObjects = new ClusterCommandObjects();

    if (builder.clientConfig.getRedisProtocol() != null) {
      this.commandObjects.setProtocol(builder.clientConfig.getRedisProtocol());
    }

    if (builder.cache != null) {
      this.cache = builder.cache;
    } else {
      this.cache = null;
    }

    // Apply common configuration from AbstractRedisClientBuilder
    builder.applyCommonConfiguration(this.commandObjects);
  }

  // Abstract method implementations from BaseRedisClient

  @Override
  protected CommandObjects getCommandObjects() {
    return commandObjects;
  }

  @Override
  protected ConnectionProvider getConnectionProvider() {
    return provider;
  }

  @Override
  public Cache getCache() {
    return cache;
  }

  @Override
  public <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  @Override
  public <T> T broadcastCommand(CommandObject<T> commandObject) {
    return executor.broadcastCommand(commandObject);
  }

  @Override
  protected <T> T checkAndBroadcastCommand(CommandObject<T> commandObject) {
    // For cluster, we need to broadcast to all nodes
    return broadcastCommand(commandObject);
  }

  // Cluster-specific methods

  /**
   * Executes a command on a replica node if available.
   * @param commandObject the command to execute
   * @param <T> the return type
   * @return the command result
   */
  public <T> T executeCommandToReplica(CommandObject<T> commandObject) {
    return executor.executeCommandToReplica(commandObject);
  }

  /**
   * Creates a new cluster-aware pipeline for batching commands.
   * @return a new ClusterPipeline instance
   */
  public ClusterPipeline pipelined() {
    return new ClusterPipeline(provider, commandObjects);
  }

  /**
   * Transactions are not supported in cluster mode.
   * @param doMulti ignored parameter
   * @return nothing
   * @throws UnsupportedOperationException always
   */
  public AbstractTransaction transaction(boolean doMulti) {
    throw new UnsupportedOperationException("Transactions are not supported in cluster mode");
  }

  // Sharded Pub/Sub methods

  /**
   * Publishes a message to a sharded channel.
   * @param channel the channel to publish to
   * @param message the message to publish
   * @return the number of clients that received the message
   */
  public long spublish(String channel, String message) {
    return executeCommand(commandObjects.spublish(channel, message));
  }

  /**
   * Publishes a message to a sharded channel.
   * @param channel the channel to publish to
   * @param message the message to publish
   * @return the number of clients that received the message
   */
  public long spublish(byte[] channel, byte[] message) {
    return executeCommand(commandObjects.spublish(channel, message));
  }

  /**
   * Subscribes to sharded channels.
   * @param jedisPubSub the pub/sub handler
   * @param channels the channels to subscribe to
   */
  public void ssubscribe(final JedisShardedPubSub jedisPubSub, final String... channels) {
    try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  /**
   * Subscribes to sharded channels.
   * @param jedisPubSub the pub/sub handler
   * @param channels the channels to subscribe to
   */
  public void ssubscribe(BinaryJedisShardedPubSub jedisPubSub, final byte[]... channels) {
    try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  // Cluster utility methods

  /**
   * Returns all nodes that were configured to connect to in key-value pairs ({@link Map}). Key is
   * the HOST:PORT and the value is the connection pool.
   * @return the map of all connections.
   */
  public Map<String, ConnectionPool> getClusterNodes() {
    return provider.getNodes();
  }

  /**
   * Returns the connection for one of the 16,384 slots.
   * @param slot the slot to retrieve the connection for.
   * @return connection of the provided slot. {@code close()} of this connection must be called
   *         after use.
   */
  public Connection getConnectionFromSlot(int slot) {
    return provider.getConnectionFromSlot(slot);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.executor);
  }

  // ClusterCommands interface implementation
  // Note: These commands use sendCommand pattern since they are not available in CommandObjects

  @Override
  public String asking() {
    return (String) sendCommand(Protocol.Command.ASKING);
  }

  @Override
  public String readonly() {
    return (String) sendCommand(Protocol.Command.READONLY);
  }

  @Override
  public String readwrite() {
    return (String) sendCommand(Protocol.Command.READWRITE);
  }

  @Override
  public String clusterNodes() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.NODES.name());
  }

  @Override
  public String clusterMeet(String ip, int port) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.MEET.name(), ip,
      String.valueOf(port));
  }

  @Override
  public String clusterAddSlots(int... slots) {
    String[] args = new String[slots.length + 1];
    args[0] = Protocol.ClusterKeyword.ADDSLOTS.name();
    for (int i = 0; i < slots.length; i++) {
      args[i + 1] = String.valueOf(slots[i]);
    }
    return (String) sendCommand(Protocol.Command.CLUSTER, args);
  }

  @Override
  public String clusterDelSlots(int... slots) {
    String[] args = new String[slots.length + 1];
    args[0] = Protocol.ClusterKeyword.DELSLOTS.name();
    for (int i = 0; i < slots.length; i++) {
      args[i + 1] = String.valueOf(slots[i]);
    }
    return (String) sendCommand(Protocol.Command.CLUSTER, args);
  }

  @Override
  public String clusterInfo() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.INFO.name());
  }

  @Override
  public java.util.List<String> clusterGetKeysInSlot(int slot, int count) {
    return (java.util.List<String>) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.GETKEYSINSLOT.name(), String.valueOf(slot), String.valueOf(count));
  }

  @Override
  public java.util.List<byte[]> clusterGetKeysInSlotBinary(int slot, int count) {
    return (java.util.List<byte[]>) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.GETKEYSINSLOT.name(), String.valueOf(slot), String.valueOf(count));
  }

  @Override
  public String clusterSetSlotNode(int slot, String nodeId) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.SETSLOT.name(),
      String.valueOf(slot), Protocol.ClusterKeyword.NODE.name(), nodeId);
  }

  @Override
  public String clusterSetSlotMigrating(int slot, String nodeId) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.SETSLOT.name(),
      String.valueOf(slot), Protocol.ClusterKeyword.MIGRATING.name(), nodeId);
  }

  @Override
  public String clusterSetSlotImporting(int slot, String nodeId) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.SETSLOT.name(),
      String.valueOf(slot), Protocol.ClusterKeyword.IMPORTING.name(), nodeId);
  }

  @Override
  public String clusterSetSlotStable(int slot) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.SETSLOT.name(),
      String.valueOf(slot), Protocol.ClusterKeyword.STABLE.name());
  }

  @Override
  public String clusterForget(String nodeId) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.FORGET.name(),
      nodeId);
  }

  @Override
  public String clusterFlushSlots() {
    return (String) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.FLUSHSLOTS.name());
  }

  @Override
  public long clusterKeySlot(String key) {
    return (Long) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.KEYSLOT.name(),
      key);
  }

  @Override
  public long clusterCountFailureReports(String nodeId) {
    // Note: Using the actual keyword string since COUNT_FAILURE_REPORTS doesn't exist
    return (Long) sendCommand(Protocol.Command.CLUSTER, "COUNT-FAILURE-REPORTS", nodeId);
  }

  @Override
  public long clusterCountKeysInSlot(int slot) {
    return (Long) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.COUNTKEYSINSLOT.name(), String.valueOf(slot));
  }

  @Override
  public String clusterSaveConfig() {
    return (String) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.SAVECONFIG.name());
  }

  @Override
  public String clusterSetConfigEpoch(long configEpoch) {
    // Note: Using the actual keyword string since SET_CONFIG_EPOCH doesn't exist
    return (String) sendCommand(Protocol.Command.CLUSTER, "SET-CONFIG-EPOCH",
      String.valueOf(configEpoch));
  }

  @Override
  public String clusterBumpEpoch() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.BUMPEPOCH.name());
  }

  @Override
  public String clusterReplicate(String nodeId) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.REPLICATE.name(),
      nodeId);
  }

  @Override
  public java.util.List<String> clusterReplicas(String nodeId) {
    return (java.util.List<String>) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.REPLICAS.name(), nodeId);
  }

  @Override
  public String clusterFailover() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.FAILOVER.name());
  }

  @Override
  public String clusterFailover(redis.clients.jedis.args.ClusterFailoverOption failoverOption) {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.FAILOVER.name(),
      failoverOption.name());
  }

  @Override
  public java.util.List<redis.clients.jedis.resps.ClusterShardInfo> clusterShards() {
    return (java.util.List<redis.clients.jedis.resps.ClusterShardInfo>) sendCommand(
      Protocol.Command.CLUSTER, Protocol.ClusterKeyword.SHARDS.name());
  }

  @Override
  public String clusterReset() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.RESET.name());
  }

  @Override
  public String clusterReset(redis.clients.jedis.args.ClusterResetType resetType) {
    if (resetType == null) {
      return clusterReset();
    }
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.RESET.name(),
      resetType.name());
  }

  @Override
  public String clusterMyId() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.MYID.name());
  }

  @Override
  public String clusterMyShardId() {
    return (String) sendCommand(Protocol.Command.CLUSTER, Protocol.ClusterKeyword.MYSHARDID.name());
  }

  @Override
  public java.util.List<java.util.Map<String, Object>> clusterLinks() {
    return (java.util.List<java.util.Map<String, Object>>) sendCommand(Protocol.Command.CLUSTER,
      Protocol.ClusterKeyword.LINKS.name());
  }

  @Override
  public String clusterAddSlotsRange(int... ranges) {
    String[] args = new String[ranges.length + 1];
    args[0] = Protocol.ClusterKeyword.ADDSLOTSRANGE.name();
    for (int i = 0; i < ranges.length; i++) {
      args[i + 1] = String.valueOf(ranges[i]);
    }
    return (String) sendCommand(Protocol.Command.CLUSTER, args);
  }

  @Override
  public String clusterDelSlotsRange(int... ranges) {
    String[] args = new String[ranges.length + 1];
    args[0] = Protocol.ClusterKeyword.DELSLOTSRANGE.name();
    for (int i = 0; i < ranges.length; i++) {
      args[i + 1] = String.valueOf(ranges[i]);
    }
    return (String) sendCommand(Protocol.Command.CLUSTER, args);
  }

  /**
   * Creates a new builder for configuring RedisClusterClient instances.
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for configuring RedisClusterClient instances.
   * <p>
   * Provides a fluent API for setting cluster configuration and advanced Redis client features. For
   * detailed client configuration (authentication, SSL, timeouts, etc.), use
   * {@link DefaultJedisClientConfig.Builder} and pass the result to
   * {@link #clientConfig(JedisClientConfig)}.
   * <p>
   * Example usage:
   *
   * <pre>{@code
   * // Simple configuration
   * Set<HostAndPort> nodes = Set.of(new HostAndPort("node1", 7000), new HostAndPort("node2", 7001),
   *   new HostAndPort("node3", 7002));
   * RedisClusterClient client = RedisClusterClient.builder().nodes(nodes).build();
   *
   * // Advanced configuration with custom configs
   * JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password("secret").ssl(true)
   *     .connectionTimeoutMillis(5000).socketTimeoutMillis(10000).build();
   *
   * ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
   * poolConfig.setMaxTotal(20);
   * poolConfig.setMaxIdle(10);
   *
   * RedisClusterClient client = RedisClusterClient.builder().nodes(nodes).clientConfig(clientConfig)
   *     .poolConfig(poolConfig).maxAttempts(5).maxTotalRetriesDuration(Duration.ofSeconds(10))
   *     .topologyRefreshPeriod(Duration.ofMinutes(5)).build();
   * }</pre>
   */
  public static class Builder extends AbstractRedisClientBuilder<Builder, RedisClusterClient> {
    // Cluster-specific configuration
    private Set<HostAndPort> nodes;
    private JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();
    private int maxAttempts = JedisCluster.DEFAULT_MAX_ATTEMPTS;
    private Duration maxTotalRetriesDuration = Duration.ofMillis(JedisCluster.DEFAULT_TIMEOUT);
    private Duration topologyRefreshPeriod = null;

    private Builder() {
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public RedisClusterClient build() {
      validateCommonConfiguration();
      if (nodes == null || nodes.isEmpty()) {
        throw new IllegalArgumentException("Cluster nodes must be provided");
      }
      return new RedisClusterClient(this);
    }

    /**
     * Sets the cluster nodes to connect to.
     * <p>
     * At least one node must be provided. The client will discover other nodes in the cluster
     * automatically through the CLUSTER NODES command.
     * @param nodes the cluster nodes
     * @return this builder
     */
    public Builder nodes(Set<HostAndPort> nodes) {
      this.nodes = nodes;
      return this;
    }

    /**
     * Sets the client configuration for cluster connections.
     * <p>
     * Use {@link DefaultJedisClientConfig.Builder} to create advanced configurations with
     * authentication, SSL, timeouts, and other Redis client settings.
     * @param clientConfig the client configuration
     * @return this builder
     */
    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    /**
     * Sets the maximum number of attempts for cluster operations.
     * <p>
     * When a cluster operation fails due to redirection or node failure, the client will retry up
     * to this many times before giving up.
     * @param maxAttempts the maximum number of attempts (must be > 0)
     * @return this builder
     */
    public Builder maxAttempts(int maxAttempts) {
      if (maxAttempts <= 0) {
        throw new IllegalArgumentException("maxAttempts must be greater than 0");
      }
      this.maxAttempts = maxAttempts;
      return this;
    }

    /**
     * Sets the maximum total duration for retries.
     * <p>
     * The client will stop retrying cluster operations after this duration has elapsed, even if
     * maxAttempts has not been reached.
     * @param maxTotalRetriesDuration the maximum total retry duration
     * @return this builder
     */
    public Builder maxTotalRetriesDuration(Duration maxTotalRetriesDuration) {
      this.maxTotalRetriesDuration = maxTotalRetriesDuration;
      return this;
    }

    /**
     * Sets the topology refresh period.
     * <p>
     * The client will automatically refresh the cluster topology (node discovery and slot mapping)
     * at this interval.
     * @param topologyRefreshPeriod the topology refresh period
     * @return this builder
     */
    public Builder topologyRefreshPeriod(Duration topologyRefreshPeriod) {
      this.topologyRefreshPeriod = topologyRefreshPeriod;
      return this;
    }
  }
}
