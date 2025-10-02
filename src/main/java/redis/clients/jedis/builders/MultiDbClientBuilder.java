package redis.clients.jedis.builders;

import java.util.function.Consumer;

import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.mcf.CircuitBreakerCommandExecutor;
import redis.clients.jedis.mcf.ClusterSwitchEventArgs;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Builder for creating multi-db Redis clients with multi-endpoint support.
 * <p>
 * This builder provides methods specific to multi-db Redis deployments, including multiple
 * weighted endpoints, circuit breaker configuration, health checks, and automatic failover/failback
 * capabilities.
 * </p>
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li><strong>Multi-Endpoint Configuration:</strong> Add multiple Redis endpoints with individual
 * weights</li>
 * <li><strong>Circuit Breaker Integration:</strong> Built-in circuit breaker with configurable
 * thresholds</li>
 * <li><strong>Health Monitoring:</strong> Automatic health checks with configurable strategies</li>
 * <li><strong>Event Handling:</strong> Listen to cluster switch events for monitoring and
 * alerting</li>
 * <li><strong>Flexible Configuration:</strong> Support for both simple and advanced multi-cluster
 * configurations</li>
 * </ul>
 * <p>
 * <strong>Usage Examples:</strong>
 * </p>
 *
 * <pre>
 * // Simple configuration with default settings
 * MultiDbClient client = MultiDbClient.builder()
 *     .endpoint("primary:6379", 100)
 *     .endpoint("backup:6379", 50)
 *     .build();
 *
 * // Advanced configuration with custom settings
 * MultiDbClient client = MultiDbClient.builder()
 *     .endpoint("primary:6379", 100)
 *     .endpoint("backup:6379", 50)
 *     .endpoint("dr:6379", 25)
 *     .multiClusterConfig(
 *         MultiClusterClientConfig.builder()
 *             .circuitBreakerSlidingWindowSize(20)
 *             .circuitBreakerFailureRateThreshold(60.0f)
 *             .retryMaxAttempts(5)
 *             .enableFailback(true)
 *             .failbackCheckInterval(Duration.ofSeconds(30))
 *             .build()
 *     )
 *     .onClusterSwitch(event -&gt;
 *         log.info("Switched to cluster: {} due to: {}", 
 *             event.getEndpoint(), event.getReason()))
 *     .build();
 * </pre>
 * 
 * @param <C> the client type that this builder creates
 * @author Ivo Gaydazhiev
 * @since 5.2.0
 */
@Experimental
public abstract class MultiDbClientBuilder<C>
    extends AbstractClientBuilder<MultiDbClientBuilder<C>, C> {

  // Multi-db specific configuration fields
  private MultiClusterClientConfig multiClusterConfig = null;
  private Consumer<ClusterSwitchEventArgs> databaseSwitchListener = null;

  /**
   * Sets the multi-cluster configuration.
   * <p>
   * This configuration controls circuit breaker behavior, retry logic, health checks, failback
   * settings, and other resilience features. If not provided, default configuration will be used.
   * </p>
   * @param config the multi-cluster configuration
   * @return this builder
   */
  public MultiDbClientBuilder<C> multiClusterConfig(MultiClusterClientConfig config) {
    this.multiClusterConfig = config;
    return this;
  }

  /**
   * Sets a listener for database switch events.
   * <p>
   * The listener will be called whenever the client switches from one endpoint to another,
   * providing information about the switch reason and the new active endpoint. This is useful for
   * monitoring, alerting, and logging purposes.
   * </p>
   * @param listener the database switch event listener
   * @return this builder
   */
  public MultiDbClientBuilder<C> databaseSwitchListener(
      Consumer<ClusterSwitchEventArgs> listener) {
    this.databaseSwitchListener = listener;
    return this;
  }

  @Override
  protected MultiDbClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {

    if (this.multiClusterConfig == null || this.multiClusterConfig.getClusterConfigs() == null
        || this.multiClusterConfig.getClusterConfigs().length < 1) {
      throw new IllegalArgumentException("At least one endpoint must be specified");
    }

    // Create the multi-cluster connection provider
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        multiClusterConfig);

    // Set database switch listener if provided
    if (this.databaseSwitchListener != null) {
      provider.setClusterSwitchListener(this.databaseSwitchListener);
    }

    return provider;
  }

  @Override
  protected CommandExecutor createDefaultCommandExecutor() {
    // For multi-db clients, we always use CircuitBreakerCommandExecutor
    return new CircuitBreakerCommandExecutor(
        (MultiClusterPooledConnectionProvider) this.connectionProvider);
  }

  @Override
  protected void validateSpecificConfiguration() {

  }

}
