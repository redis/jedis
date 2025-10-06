package redis.clients.jedis;

import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.builders.MultiDbClientBuilder;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.mcf.CircuitBreakerCommandExecutor;
import redis.clients.jedis.mcf.MultiClusterPipeline;
import redis.clients.jedis.mcf.MultiClusterTransaction;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.mcf.MultiDatabaseConnectionProvider;

import java.util.Set;

/**
 * MultiDbClient provides high-availability Redis connectivity with automatic failover and failback
 * capabilities across multiple weighted endpoints.
 * <p>
 * This client extends UnifiedJedis to support resilient operations with:
 * <ul>
 * <li><strong>Multi-Endpoint Support:</strong> Configure multiple Redis endpoints with individual
 * weights</li>
 * <li><strong>Automatic Failover:</strong> Seamless switching to backup endpoints when primary
 * becomes unavailable</li>
 * <li><strong>Circuit Breaker Pattern:</strong> Built-in circuit breaker to prevent cascading
 * failures</li>
 * <li><strong>Weight-Based Selection:</strong> Intelligent endpoint selection based on configured
 * weights</li>
 * <li><strong>Health Monitoring:</strong> Continuous health checks with automatic failback to
 * recovered endpoints</li>
 * <li><strong>Retry Logic:</strong> Configurable retry mechanisms with exponential backoff</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * 
 * <pre>
 * // Create multi-db client with multiple endpoints
 * HostAndPort primary = new HostAndPort("localhost", 29379);
 * HostAndPort secondary = new HostAndPort("localhost", 29380);
 *
 *
 * MultiDbClient client = MultiDbClient.builder()
 *                 .multiDbConfig(
 *                         MultiDbConfig.builder()
 *                                 .endpoint(
 *                                         DatabaseConfig.builder(
 *                                                         primary,
 *                                                         DefaultJedisClientConfig.builder().build())
 *                                                 .weight(100.0f)
 *                                                 .build())
 *                                 .endpoint(DatabaseConfig.builder(
 *                                                 secondary,
 *                                                 DefaultJedisClientConfig.builder().build())
 *                                         .weight(50.0f).build())
 *                                 .circuitBreakerFailureRateThreshold(50.0f)
 *                                 .retryMaxAttempts(3)
 *                                 .build()
 *                 )
 *                 .databaseSwitchListener(event -&gt;
 *                    System.out.println("Switched to: " + event.getEndpoint()))
 *                 .build();
 * 
 * // Use like any other Jedis client
 * client.set("key", "value");
 * String value = client.get("key");
 * 
 * // Automatic failover happens transparently
 * client.close();
 * </pre>
 * <p>
 * The client automatically handles endpoint failures and recoveries, providing transparent high
 * availability for Redis operations. All standard Jedis operations are supported with the added
 * resilience features.
 * </p>
 * @author Ivo Gaydazhiev
 * @since 5.2.0
 * @see MultiDatabaseConnectionProvider
 * @see CircuitBreakerCommandExecutor
 * @see MultiDbConfig
 */
@Experimental
public class MultiDbClient extends UnifiedJedis {

  /**
   * Creates a MultiDbClient with custom components.
   * <p>
   * This constructor allows full customization of the client components and is primarily used by
   * the builder pattern for advanced configurations. For most use cases, prefer using
   * {@link #builder()} to create instances.
   * </p>
   * @param commandExecutor the command executor (typically CircuitBreakerCommandExecutor)
   * @param connectionProvider the connection provider (typically MultiDatabaseConnectionProvider)
   * @param commandObjects the command objects
   * @param redisProtocol the Redis protocol version
   * @param cache the client-side cache (may be null)
   */
  MultiDbClient(CommandExecutor commandExecutor, ConnectionProvider connectionProvider,
      CommandObjects commandObjects, RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  /**
   * Returns the underlying MultiDatabaseConnectionProvider.
   * <p>
   * This provides access to multi-cluster specific operations like manual failover, health status
   * monitoring, and cluster switch event handling.
   * </p>
   * @return the multi-cluster connection provider
   * @throws ClassCastException if the provider is not a MultiDatabaseConnectionProvider
   */
  private MultiDatabaseConnectionProvider getMultiDatabaseConnectionProvider() {
    return (MultiDatabaseConnectionProvider) this.provider;
  }

  /**
   * Manually switches to the specified endpoint.
   * <p>
   * This method allows manual failover to a specific endpoint, bypassing the automatic weight-based
   * selection. The switch will only succeed if the target endpoint is healthy.
   * </p>
   * @param endpoint the endpoint to switch to
   */
  public void setActiveDatabase(Endpoint endpoint) {
    getMultiDatabaseConnectionProvider().setActiveDatabase(endpoint);
  }

  /**
   * Adds a pre-configured cluster configuration.
   * <p>
   * This method allows adding a fully configured DatabaseConfig instance, providing maximum
   * flexibility for advanced configurations including custom health check strategies, connection
   * pool settings, etc.
   * </p>
   * @param databaseConfig the pre-configured database configuration
   */
  public void addEndpoint(DatabaseConfig databaseConfig) {
    getMultiDatabaseConnectionProvider().add(databaseConfig);
  }

  /**
   * Dynamically adds a new cluster endpoint to the resilient client.
   * <p>
   * This allows adding new endpoints at runtime without recreating the client. The new endpoint
   * will be available for failover operations immediately after being added and passing health
   * checks (if configured).
   * </p>
   * @param endpoint the Redis server endpoint
   * @param weight the weight for this endpoint (higher values = higher priority)
   * @param clientConfig the client configuration for this endpoint
   * @throws redis.clients.jedis.exceptions.JedisValidationException if the endpoint already exists
   */
  public void addEndpoint(Endpoint endpoint, float weight, JedisClientConfig clientConfig) {
    DatabaseConfig databaseConfig = DatabaseConfig.builder(endpoint, clientConfig).weight(weight)
        .build();

    getMultiDatabaseConnectionProvider().add(databaseConfig);
  }

  /**
   * Returns the set of all configured endpoints.
   * <p>
   * This method provides a view of all endpoints currently configured in the resilient client.
   * </p>
   * @return the set of all configured endpoints
   */
  public Set<Endpoint> getEndpoints() {
    return getMultiDatabaseConnectionProvider().getEndpoints();
  }

  /**
   * Returns the health status of the specified endpoint.
   * <p>
   * This method provides the current health status of a specific endpoint.
   * </p>
   * @param endpoint the endpoint to check
   * @return the health status of the endpoint
   */
  public boolean isHealthy(Endpoint endpoint) {
    return getMultiDatabaseConnectionProvider().isHealthy(endpoint);
  }

  /**
   * Dynamically removes a cluster endpoint from the resilient client.
   * <p>
   * This allows removing endpoints at runtime. If the removed endpoint is currently active, the
   * client will automatically failover to the next available healthy endpoint based on weight
   * priority.
   * </p>
   * @param endpoint the endpoint to remove
   * @throws redis.clients.jedis.exceptions.JedisValidationException if the endpoint doesn't exist
   * @throws redis.clients.jedis.exceptions.JedisException if removing the endpoint would leave no
   *           healthy clusters available
   */
  public void removeEndpoint(Endpoint endpoint) {
    getMultiDatabaseConnectionProvider().remove(endpoint);
  }

  /**
   * Forces the client to switch to a specific endpoint for a duration.
   * <p>
   * This method forces the client to use the specified endpoint and puts all other endpoints in a
   * grace period, preventing automatic failover for the specified duration. This is useful for
   * maintenance scenarios or testing specific endpoints.
   * </p>
   * @param endpoint the endpoint to force as active
   * @param forcedActiveDurationMs the duration in milliseconds to keep this endpoint forced
   * @throws redis.clients.jedis.exceptions.JedisValidationException if the endpoint is not healthy
   *           or doesn't exist
   */
  public void forceActiveEndpoint(Endpoint endpoint, long forcedActiveDurationMs) {
    getMultiDatabaseConnectionProvider().forceActiveDatabase(endpoint, forcedActiveDurationMs);
  }

  /**
   * Creates a new pipeline for batch operations with multi-cluster support.
   * <p>
   * The returned pipeline supports the same resilience features as the main client, including
   * automatic failover during batch execution.
   * </p>
   * @return a new MultiClusterPipeline instance
   */
  @Override
  public MultiClusterPipeline pipelined() {
    return new MultiClusterPipeline(getMultiDatabaseConnectionProvider(), commandObjects);
  }

  /**
   * Creates a new transaction with multi-cluster support.
   * <p>
   * The returned transaction supports the same resilience features as the main client, including
   * automatic failover during transaction execution.
   * </p>
   * @return a new MultiClusterTransaction instance
   */
  @Override
  public MultiClusterTransaction multi() {
    return new MultiClusterTransaction((MultiDatabaseConnectionProvider) provider, true,
        commandObjects);
  }

  /**
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   * @return transaction object
   */
  @Override
  public MultiClusterTransaction transaction(boolean doMulti) {
    if (provider == null) {
      throw new IllegalStateException(
          "It is not allowed to create Transaction from this " + getClass());
    }

    return new MultiClusterTransaction(getMultiDatabaseConnectionProvider(), doMulti,
        commandObjects);
  }

  public Endpoint getActiveEndpoint() {
    return getMultiDatabaseConnectionProvider().getDatabase().getEndpoint();
  }

  /**
   * Fluent builder for {@link MultiDbClient}.
   * <p>
   * Obtain an instance via {@link #builder()}.
   * </p>
   */
  public static class Builder extends MultiDbClientBuilder<MultiDbClient> {

    @Override
    protected MultiDbClient createClient() {
      return new MultiDbClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache);
    }
  }

  /**
   * Create a new builder for configuring MultiDbClient instances.
   * @return a new {@link MultiDbClient.Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }
}
