package redis.clients.jedis.builders;

import java.time.Duration;
import java.util.Set;
import redis.clients.jedis.*;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Builder for creating JedisCluster instances (Redis Cluster connections).
 * <p>
 * This builder provides methods specific to Redis Cluster deployments, including cluster nodes
 * configuration, retry settings, and topology refresh configuration.
 * </p>
 */
public abstract class ClusterClientBuilder<C>
    extends AbstractClientBuilder<ClusterClientBuilder<C>, C> {

  // Cluster-specific configuration fields
  private Set<HostAndPort> nodes = null;
  private Duration topologyRefreshPeriod = null;
  private CommandFlagsRegistry commandFlags = null;

  /**
   * Sets the cluster nodes to connect to.
   * <p>
   * At least one node must be specified. The client will discover other nodes in the cluster
   * automatically.
   * @param nodes the set of cluster nodes
   * @return this builder
   */
  public ClusterClientBuilder<C> nodes(Set<HostAndPort> nodes) {
    this.nodes = nodes;
    return this;
  }

  /**
   * Sets the topology refresh period for cluster slot mapping updates.
   * <p>
   * The client will periodically refresh its view of the cluster topology to handle slot migrations
   * and node changes. A shorter period provides faster adaptation to cluster changes but increases
   * overhead.
   * @param topologyRefreshPeriod the topology refresh period
   * @return this builder
   */
  public ClusterClientBuilder<C> topologyRefreshPeriod(Duration topologyRefreshPeriod) {
    this.topologyRefreshPeriod = topologyRefreshPeriod;
    return this;
  }

  /**
   * Overrides the default command flags registry.
   * @param commandFlags custom command flags registry
   * @return this builder
   */
  public ClusterClientBuilder<C> commandFlags(CommandFlagsRegistry commandFlags) {
    this.commandFlags = commandFlags;
    return this;
  }

  /**
   * Gets the command flags registry, initializing it if necessary.
   * @return the command flags registry
   */
  protected CommandFlagsRegistry getCommandFlags() {
    if (this.commandFlags == null) {
      this.commandFlags = createDefaultCommandFlagsRegistry();
    }
    return this.commandFlags;
  }

  @Override
  protected ClusterClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {
    return new ClusterConnectionProvider(this.nodes, this.clientConfig, this.cache, this.poolConfig,
        this.topologyRefreshPeriod);
  }

  /**
   * Creates a default command flags registry based on the current configuration.
   * @return CommandFlagsRegistry
   */
  protected CommandFlagsRegistry createDefaultCommandFlagsRegistry() {
    return StaticCommandFlagsRegistry.registry();
  }

  @Override
  protected CommandExecutor createDefaultCommandExecutor() {
    if (this.commandFlags == null) {
      this.commandFlags = createDefaultCommandFlagsRegistry();
    }

    return new ClusterCommandExecutor((ClusterConnectionProvider) this.connectionProvider,
        this.maxAttempts, this.maxTotalRetriesDuration, this.commandFlags);
  }

  @Override
  protected CommandObjects createDefaultCommandObjects() {
    return new ClusterCommandObjects();
  }

  @Override
  protected void validateSpecificConfiguration() {
    validateCommonConfiguration();

    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one cluster node must be specified for cluster mode");
    }

    if (topologyRefreshPeriod != null && topologyRefreshPeriod.isNegative()) {
      throw new IllegalArgumentException(
          "Topology refresh period cannot be negative for cluster mode");
    }
  }

}
