package redis.clients.jedis.mcf;

import java.util.Map;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.mcf.InitializationPolicy.Decision;

/**
 * Context for tracking connection initialization status across multiple database endpoints.
 * <p>
 * This class evaluates the current state of database connections and their health checks to
 * determine how many connections are available, failed, or still pending. It is used in conjunction
 * with {@link InitializationPolicy} to decide when the multi-database connection provider is ready
 * to be used.
 * </p>
 * @author Ali Takavci
 * @since 7.3
 */
class ConnectionInitializationContext implements InitializationPolicy.InitializationContext {

  private int available = 0;

  private int failed = 0;

  private int pending = 0;

  /**
   * Creates a new ConnectionInitializationContext by evaluating the current state of database
   * connections and their health statuses.
   * @param databases map of database endpoints to their Database instances
   * @param healthStatusManager manager for tracking health status of endpoints
   */
  public ConnectionInitializationContext(
      Map<Endpoint, MultiDbConnectionProvider.Database> databases,
      HealthStatusManager healthStatusManager) {

    for (Map.Entry<Endpoint, MultiDbConnectionProvider.Database> entry : databases.entrySet()) {
      Endpoint endpoint = entry.getKey();

      // Check if health checks are enabled for this endpoint
      if (healthStatusManager.hasHealthCheck(endpoint)) {
        HealthStatus status = healthStatusManager.getHealthStatus(endpoint);

        if (status == HealthStatus.HEALTHY) {
          // Health check completed successfully
          available++;
        } else if (status == HealthStatus.UNHEALTHY) {
          // Health check completed with failure
          failed++;
        } else {
          // Health check not completed yet (UNKNOWN)
          pending++;
        }
      } else {
        // No health check configured - assume available
        available++;
      }
    }
  }

  @Override
  public int getAvailableConnections() {
    return available;
  }

  @Override
  public int getFailedConnections() {
    return failed;
  }

  @Override
  public int getPendingConnections() {
    return pending;
  }

  /**
   * Evaluates whether the current connection state conforms to the given initialization policy.
   * @param policy the initialization policy to evaluate against
   * @return the decision (CONTINUE, SUCCESS, or FAIL) based on the policy evaluation
   */
  public Decision conformsTo(InitializationPolicy policy) {
    return policy.evaluate(this);
  }

  @Override
  public String toString() {
    return "ConnectionInitializationContext{" + "available=" + available + ", failed=" + failed
        + ", pending=" + pending + '}';
  }

}
