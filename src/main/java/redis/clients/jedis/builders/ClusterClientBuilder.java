package redis.clients.jedis.builders;

import java.time.Duration;
import java.util.Set;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Builder for creating JedisCluster instances (Redis Cluster connections).
 * <p>
 * This builder provides methods specific to Redis Cluster deployments, including cluster nodes
 * configuration, retry settings, and topology refresh configuration.
 * <p>
 * Example usage:
 *
 */
public abstract class ClusterClientBuilder<C extends JedisCluster>
    extends AbstractClientBuilder<ClusterClientBuilder<C>, C> {

  // Cluster-specific configuration fields
  private Set<HostAndPort> nodes = null;
  private JedisClientConfig clientConfig = null;
  private int maxAttempts = JedisCluster.DEFAULT_MAX_ATTEMPTS;
  private Duration maxTotalRetriesDuration = null;
  private Duration topologyRefreshPeriod = null;

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
   * Sets the client configuration for Redis connections.
   * <p>
   * The client configuration includes authentication, timeouts, SSL settings, and other
   * connection-specific parameters.
   * @param clientConfig the client configuration
   * @return this builder
   */
  public ClusterClientBuilder<C> clientConfig(JedisClientConfig clientConfig) {
    this.clientConfig = clientConfig;
    return this;
  }

  /**
   * Sets the maximum number of attempts for cluster operations.
   * <p>
   * When a cluster operation fails (e.g., due to node failure or slot migration), the client will
   * retry up to this many times before giving up.
   * @param maxAttempts the maximum number of attempts (must be positive)
   * @return this builder
   */
  public ClusterClientBuilder<C> maxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  /**
   * Sets the maximum total duration for retries across all attempts.
   * <p>
   * This provides a time-based limit on retries in addition to the attempt-based limit. If not set,
   * it will be calculated as socketTimeout * maxAttempts.
   * @param maxTotalRetriesDuration the maximum total retry duration
   * @return this builder
   */
  public ClusterClientBuilder<C> maxTotalRetriesDuration(Duration maxTotalRetriesDuration) {
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
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

  @Override
  protected ClusterClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {
    JedisClientConfig config = this.clientConfig != null ? this.clientConfig
        : DefaultJedisClientConfig.builder().build();
    return new ClusterConnectionProvider(this.nodes, config, this.cache, this.poolConfig,
        this.topologyRefreshPeriod);
  }

  @Override
  protected void validateSpecificConfiguration() {
    validateCommonConfiguration();

    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one cluster node must be specified for cluster mode");
    }

    if (maxAttempts <= 0) {
      throw new IllegalArgumentException("Max attempts must be positive for cluster mode");
    }

    if (maxTotalRetriesDuration != null && maxTotalRetriesDuration.isNegative()) {
      throw new IllegalArgumentException(
          "Max total retries duration cannot be negative for cluster mode");
    }

    if (topologyRefreshPeriod != null && topologyRefreshPeriod.isNegative()) {
      throw new IllegalArgumentException(
          "Topology refresh period cannot be negative for cluster mode");
    }
  }

}
