package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import redis.clients.jedis.MultiDbConfig;

/**
 * Adapter that disables Resilience4j's built-in circuit breaker evaluation and help delegate
 * threshold decisions to Jedis's custom dual-threshold logic.
 * <p>
 * This adapter sets maximum values for failure rate (100%) and minimum calls (Integer.MAX_VALUE) to
 * effectively disable Resilience4j's automatic circuit breaker transitions, allowing
 * {@link MultiDatabaseConnectionProvider.Database#evaluateThresholds(boolean)} to control when the
 * circuit breaker opens based on both minimum failure count AND failure rate.
 * </p>
 * @see MultiDatabaseConnectionProvider.Database#evaluateThresholds(boolean)
 */
class CircuitBreakerThresholdsAdapter {
  /** Maximum failure rate threshold (100%) to disable Resilience4j evaluation */
  private static final float FAILURE_RATE_THRESHOLD_MAX = 100.0f;

  /** Always set to 100% to disable Resilience4j's rate-based evaluation */
  private float failureRateThreshold;

  /** Always set to Integer.MAX_VALUE to disable Resilience4j's call-count evaluation */
  private int minimumNumberOfCalls;

  /** Sliding window size from configuration for metrics collection */
  private int slidingWindowSize;

  /**
   * Returns Integer.MAX_VALUE to disable Resilience4j's minimum call evaluation.
   * @return Integer.MAX_VALUE to prevent automatic circuit breaker evaluation
   */
  int getMinimumNumberOfCalls() {
    return minimumNumberOfCalls;
  }

  /**
   * Returns 100% to disable Resilience4j's failure rate evaluation.
   * @return 100.0f to prevent automatic circuit breaker evaluation
   */
  float getFailureRateThreshold() {
    return failureRateThreshold;
  }

  /**
   * Returns TIME_BASED sliding window type for metrics collection.
   * @return SlidingWindowType.TIME_BASED
   */
  SlidingWindowType getSlidingWindowType() {
    return SlidingWindowType.TIME_BASED;
  }

  /**
   * Returns the sliding window size for metrics collection.
   * @return sliding window size in seconds
   */
  int getSlidingWindowSize() {
    return slidingWindowSize;
  }

  /**
   * Creates an adapter that disables Resilience4j's circuit breaker evaluation.
   * <p>
   * Sets failure rate to 100% and minimum calls to Integer.MAX_VALUE to ensure Resilience4j never
   * automatically opens the circuit breaker. Instead, Jedis's custom {@code evaluateThresholds()}
   * method controls circuit breaker state based on the original configuration's dual-threshold
   * logic.
   * </p>
   * @param multiDbConfig configuration containing sliding window size
   */
  CircuitBreakerThresholdsAdapter(MultiDbConfig multiDbConfig) {

    // IMPORTANT: failureRateThreshold is set to max theoretically disable Resilience4j's evaluation
    // and rely on our custom evaluateThresholds() logic.
    failureRateThreshold = FAILURE_RATE_THRESHOLD_MAX;

    // IMPORTANT: minimumNumberOfCalls is set to max theoretically disable Resilience4j's evaluation
    // and rely on our custom evaluateThresholds() logic.
    minimumNumberOfCalls = Integer.MAX_VALUE;

    slidingWindowSize = multiDbConfig.getCircuitBreakerSlidingWindowSize();
  }
}
