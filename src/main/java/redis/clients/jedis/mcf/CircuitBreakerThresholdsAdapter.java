package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import redis.clients.jedis.MultiClusterClientConfig;

public class CircuitBreakerThresholdsAdapter {

    private float failureRateThreshold;
    private int minimumNumberOfCalls;
    private int slidingWindowSize;

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public SlidingWindowType getSlidingWindowType() {
        return SlidingWindowType.TIME_BASED;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public CircuitBreakerThresholdsAdapter(MultiClusterClientConfig multiClusterClientConfig) {
        // IMPORTATNT: this is due to we can not set failureRateThreshold to 0.0f in Resilience4j
        failureRateThreshold = multiClusterClientConfig.getCircuitBreakerFailureRateThreshold();
        failureRateThreshold = failureRateThreshold == 0.0f ? 100.0f : failureRateThreshold;

        // IMPORTANT: minimumNumberOfCalls is calculated based on the configured thresholds.
        minimumNumberOfCalls = calculateMinTotalCalls(
            multiClusterClientConfig.getThresholdMinNumOfFailures(),
            multiClusterClientConfig.getCircuitBreakerFailureRateThreshold());
        // circuitBreakerConfigBuilder.minimumNumberOfCalls(minimumNumberOfCalls);

        slidingWindowSize = multiClusterClientConfig.getCircuitBreakerSlidingWindowSize();
    }

    private static int calculateMinTotalCalls(int failures, float rate) {
        int minCalls = (int) (failures * rate * 100);
        if (failures == 0) {
            minCalls = 1;
        }
        // This will prevent the CB from OPENing by itself in all non-0.0 rate configurations by
        // setting minimumNumberOfCalls to a value that practically guarantees it won't be reached
        // (Though it might get to MAX_VALUE, CB is going to receive exceptions and be %100 failure
        // rate from this adapter).
        // This keeps the CB CLOSED and lets our executor’s dual-threshold condition decide
        // failover.
        if (rate == 0) {
            minCalls = Integer.MAX_VALUE;
        }
        return minCalls;
    }
}
