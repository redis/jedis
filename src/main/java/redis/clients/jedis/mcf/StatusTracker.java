package redis.clients.jedis.mcf;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.InitializationPolicy.Decision;

/**
 * StatusTracker is responsible for tracking and waiting for health status changes for specific
 * endpoints. It provides an event-driven approach to wait for health status transitions from
 * UNKNOWN to either HEALTHY or UNHEALTHY.
 */
public class StatusTracker {

  private static final Logger log = LoggerFactory.getLogger(StatusTracker.class);

  private final HealthStatusManager healthStatusManager;

  public StatusTracker(HealthStatusManager healthStatusManager) {
    this.healthStatusManager = healthStatusManager;
  }

  /**
   * Waits for a specific endpoint's health status to be determined (not UNKNOWN). Uses event-driven
   * approach with CountDownLatch to avoid polling.
   * @param endpoint the endpoint to wait for
   * @return the determined health status (HEALTHY or UNHEALTHY)
   * @throws JedisConnectionException if interrupted while waiting
   */
  public HealthStatus waitForHealthStatus(Endpoint endpoint) {
    // First check if status is already determined
    HealthStatus currentStatus = healthStatusManager.getHealthStatus(endpoint);
    if (currentStatus != HealthStatus.UNKNOWN) {
      return currentStatus;
    }

    // Set up event-driven waiting
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<HealthStatus> resultStatus = new AtomicReference<>();

    // Create a temporary listener for this specific endpoint
    HealthStatusListener tempListener = new HealthStatusListener() {
      @Override
      public void onStatusChange(HealthStatusChangeEvent event) {
        if (event.getEndpoint().equals(endpoint) && event.getNewStatus() != HealthStatus.UNKNOWN) {
          resultStatus.set(event.getNewStatus());
          latch.countDown();
        }
      }
    };

    // Register the temporary listener
    healthStatusManager.registerListener(endpoint, tempListener);

    try {
      // Double-check status after registering listener (race condition protection)
      currentStatus = healthStatusManager.getHealthStatus(endpoint);
      if (currentStatus != HealthStatus.UNKNOWN) {
        return currentStatus;
      }

      // Wait for the health status change event
      // just for safety to not block indefinitely
      boolean completed = latch.await(healthStatusManager.getMaxWaitFor(endpoint),
        TimeUnit.MILLISECONDS);
      if (!completed) {
        throw new JedisValidationException("Timeout while waiting for health check result");
      }
      return resultStatus.get();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JedisConnectionException("Interrupted while waiting for health check result", e);
    } finally {
      // Clean up: unregister the temporary listener
      healthStatusManager.unregisterListener(endpoint, tempListener);
    }
  }

  /**
   * Waits for health checks to complete based on the initialization policy. This method blocks
   * until the policy evaluates to SUCCESS or FAIL.
   * @param databases map of database endpoints to their Database instances
   * @param healthStatusManager manager for tracking health status of endpoints
   * @param policy the initialization policy to evaluate
   * @throws JedisConnectionException if the policy evaluation fails or if interrupted while waiting
   */
  public void waitForPolicy(Map<Endpoint, MultiDbConnectionProvider.Database> databases,
      HealthStatusManager healthStatusManager, InitializationPolicy policy) {

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Decision> finalDecision = new AtomicReference<>(Decision.CONTINUE);

    // Create a listener that evaluates the policy on every health status change
    HealthStatusListener policyListener = new HealthStatusListener() {
      @Override
      public void onStatusChange(HealthStatusChangeEvent event) {
        // Evaluate the current state against the policy
        ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
            healthStatusManager);
        Decision decision = ctx.conformsTo(policy);

        log.info("Policy evaluation: {} with context: {}", decision, ctx);

        if (decision == Decision.SUCCESS || decision == Decision.FAIL) {
          finalDecision.set(decision);
          latch.countDown();
        }
        // If CONTINUE, keep waiting for more health status changes
      }
    };

    // Register the listener globally to receive all health status changes
    healthStatusManager.registerListener(policyListener);

    try {
      // Evaluate the policy immediately in case some health checks are already complete
      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);
      Decision decision = ctx.conformsTo(policy);

      log.info("Initial policy evaluation: {} with context: {}", decision, ctx);

      if (decision == Decision.SUCCESS) {
        log.info("Initialization policy satisfied immediately");
        return;
      } else if (decision == Decision.FAIL) {
        throw new JedisConnectionException(
            "Initialization failed due to initialization policy: " + ctx);
      }

      // Wait for the policy to be satisfied
      // Calculate maximum wait time based on all endpoints
      long maxWaitTime = databases.keySet().stream().filter(healthStatusManager::hasHealthCheck)
          .mapToLong(healthStatusManager::getMaxWaitFor).max().orElse(30000); // Default to 30
                                                                              // seconds if no
                                                                              // health checks

      boolean completed = latch.await(maxWaitTime, TimeUnit.MILLISECONDS);

      if (!completed) {
        throw new JedisConnectionException(
            "Timeout while waiting for initialization policy to be satisfied");
      }

      // Check the final decision
      if (finalDecision.get() == Decision.FAIL) {
        ConnectionInitializationContext finalCtx = new ConnectionInitializationContext(databases,
            healthStatusManager);
        throw new JedisConnectionException(
            "Initialization failed due to initialization policy: " + finalCtx);
      }

      log.info("Initialization policy satisfied");

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JedisConnectionException("Interrupted while waiting for initialization policy", e);
    } finally {
      // Clean up: unregister the listener
      healthStatusManager.unregisterListener(policyListener);
    }
  }
}
