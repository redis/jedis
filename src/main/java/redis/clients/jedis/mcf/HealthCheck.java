
package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public interface HealthCheck {

  Endpoint getEndpoint();

  HealthStatus getStatus();

  void stop();

  void start();

  /**
   * Get the maximum wait duration (in milliseconds) to wait for health check HealthStatus reach
   * stable state.
   * <p>
   * Transition to stable state means either HEALTHY or UNHEALTHY. UNKNOWN is not considered stable.
   * This is calculated based on the health check strategy's timeout, retry delay and retry count.
   * </p>
   * @return the maximum wait duration in milliseconds
   */
  long getMaxWaitFor();

}
