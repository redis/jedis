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
 * This builder provides methods specific to multi-db Redis deployments, including multiple weighted
 * endpoints, circuit breaker configuration, health checks, and automatic failover/failback
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
 * MultiDbClient client = MultiDbClient.builder()
 *                 .multiDbConfig(
 *                         MultiClusterClientConfig.builder()
 *                                 .endpoint(
 *                                         ClusterConfig.builder(
 *                                                         east,
 *                                                         DefaultJedisClientConfig.builder().credentials(credentialsEast).build())
 *                                                 .weight(100.0f)
 *                                                 .build())
 *                                 .endpoint(ClusterConfig.builder(
 *                                                 west,
 *                                                 DefaultJedisClientConfig.builder().credentials(credentialsWest).build())
 *                                         .weight(50.0f).build())
 *                                 .circuitBreakerFailureRateThreshold(50.0f)
 *                                 .retryMaxAttempts(3)
 *                                 .build()
 *                 )
 *                 .databaseSwitchListener(event -&gt;
 *                     System.out.println("Switched to: " + event.getEndpoint()))
 *                 .build();
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
  private MultiClusterClientConfig multiDbConfig = null;
  private Consumer<ClusterSwitchEventArgs> databaseSwitchListener = null;

  /**
   * Sets the multi-database configuration.
   * <p>
   * This configuration controls circuit breaker behavior, retry logic, health checks, failback
   * settings, and other resilience features. If not provided, default configuration will be used.
   * </p>
   * @param config the multi-database configuration
   * @return this builder
   */
  public MultiDbClientBuilder<C> multiDbConfig(MultiClusterClientConfig config) {
    this.multiDbConfig = config;
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
  public MultiDbClientBuilder<C> databaseSwitchListener(Consumer<ClusterSwitchEventArgs> listener) {
    this.databaseSwitchListener = listener;
    return this;
  }

  @Override
  protected MultiDbClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {

    if (this.multiDbConfig == null || this.multiDbConfig.getClusterConfigs() == null
        || this.multiDbConfig.getClusterConfigs().length < 1) {
      throw new IllegalArgumentException("At least one endpoint must be specified");
    }

    // Create the multi-cluster connection provider
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        multiDbConfig);

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
