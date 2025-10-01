package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider.Cluster;

/**
 * Tests for circuit breaker thresholds: both failure-rate threshold and minimum number of failures
 * must be exceeded to trigger failover. Uses a real CircuitBreaker and real Retry, but mocks the
 * provider and cluster wiring to avoid network I/O.
 */
public class ClusterEvaluateThresholdsTest {

  private MultiClusterPooledConnectionProvider provider;
  private Cluster cluster;
  private CircuitBreaker circuitBreaker;
  private CircuitBreaker.Metrics metrics;

  @BeforeEach
  public void setup() {
    provider = mock(MultiClusterPooledConnectionProvider.class);
    cluster = mock(Cluster.class);

    circuitBreaker = mock(CircuitBreaker.class);
    metrics = mock(CircuitBreaker.Metrics.class);

    when(cluster.getCircuitBreaker()).thenReturn(circuitBreaker);
    when(circuitBreaker.getMetrics()).thenReturn(metrics);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    // Configure the mock to call the real evaluateThresholds method
    doCallRealMethod().when(cluster).evaluateThresholds(anyBoolean());

  }

  /**
   * Below minimum failures; even if all calls are failures, failover should NOT trigger. Note: The
   * isThresholdsExceeded method adds +1 to account for the current failing call, so we set
   * failures=1 which becomes 2 with +1, still below minFailures=3.
   */
  @Test
  public void belowMinFailures_doesNotFailover() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(1); // +1 becomes 2, still < 3
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
    when(cluster.getCircuitBreakerFailureRateThreshold()).thenReturn(50.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    cluster.evaluateThresholds(false);
    verify(circuitBreaker, never()).transitionToOpenState();
    verify(provider, never()).switchToHealthyCluster(any(), any());
  }

  /**
   * Reaching minFailures and exceeding failure rate threshold should trigger circuit breaker to
   * OPEN state. Note: The isThresholdsExceeded method adds +1 to account for the current failing
   * call, so we set failures=2 which becomes 3 with +1, reaching minFailures=3.
   */
  @Test
  public void minFailuresAndRateExceeded_triggersOpenState() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(2); // +1 becomes 3, reaching minFailures
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
    when(cluster.getCircuitBreakerFailureRateThreshold()).thenReturn(50.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    cluster.evaluateThresholds(false);
    verify(circuitBreaker, times(1)).transitionToOpenState();
  }

  /**
   * Even after reaching minFailures, if failure rate is below threshold, do not failover. Note: The
   * isThresholdsExceeded method adds +1 to account for the current failing call, so we set
   * failures=2 which becomes 3 with +1, reaching minFailures=3. Rate calculation: (3 failures) / (3
   * failures + 3 successes) = 50% < 80% threshold.
   */
  @Test
  public void rateBelowThreshold_doesNotFailover() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(2); // +1 becomes 3, rate = 3/(3+3) = 50%
    when(cluster.getCircuitBreakerFailureRateThreshold()).thenReturn(80.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    cluster.evaluateThresholds(false);

    verify(circuitBreaker, never()).transitionToOpenState();
    verify(provider, never()).switchToHealthyCluster(any(), any());
  }

  @Test
  public void providerBuilder_zeroRate_mapsToHundredAndHugeMinCalls() {
    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(java.util.Arrays.asList(MultiClusterClientConfig.ClusterConfig
            .builder(new HostAndPort("localhost", 6379), DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).build()));
    cfgBuilder.circuitBreakerFailureRateThreshold(0.0f).circuitBreakerMinNumOfFailures(3)
        .circuitBreakerSlidingWindowSize(10);
    MultiClusterClientConfig mcc = cfgBuilder.build();

    CircuitBreakerThresholdsAdapter adapter = new CircuitBreakerThresholdsAdapter(mcc);

    assertEquals(100.0f, adapter.getFailureRateThreshold(), 0.0001f);
    assertEquals(Integer.MAX_VALUE, adapter.getMinimumNumberOfCalls());
  }

  @ParameterizedTest
  @CsvSource({
      // Format: "minFails, rate%, success, fails, lastFailRecorded, expected"

      // === Basic threshold crossing cases ===
      "0, 1.0, 0, 1, false, true", // +1 = 2 fails, rate=100% >= 1%, min=0 -> trigger
      "0, 1.0, 0, 1, true, true",  // +0 = 1 fails, rate=100% >= 1%, min=0 -> trigger

      "1, 1.0, 0, 0, false, true", // +1 = 1 fails, rate=100% >= 1%, min=1 -> trigger
      "1, 1.0, 0, 0, true, false", // +0 = 0 fails, 0 < 1 min -> no trigger

      "3, 50.0, 0, 2, false, true", // +1 = 3 fails, rate=100% >= 50%, min=3 -> trigger
      "3, 50.0, 0, 2, true, false", // +0 = 2 fails, 2 < 3 min -> no trigger

      // === Rate threshold boundary cases ===
      "1, 100.0, 0, 0, false, true", // +1 = 1 fails, rate=100% >= 100%, min=1 -> trigger
      "1, 100.0, 0, 0, true, false", // +0 = 0 fails, 0 < 1 min -> no trigger

      "0, 100.0, 99, 1, false, false", // +1 = 2 fails, rate=1.98% < 100% -> no trigger
      "0, 100.0, 99, 1, true, false",  // +0 = 1 fails, rate=1.0% < 100% -> no trigger

      "0, 1.0, 99, 1, false, true", // +1 = 2 fails, rate=1.98% >= 1%, min=0 -> trigger
      "0, 1.0, 99, 1, true, true",  // +0 = 1 fails, rate=1.0% >= 1%, min=0 -> trigger

      // === Zero rate threshold (always trigger if min failures met) ===
      "1, 0.0, 0, 0, false, true", // +1 = 1 fails, rate=100% >= 0%, min=1 -> trigger
      "1, 0.0, 0, 0, true, false", // +0 = 0 fails, 0 < 1 min -> no trigger
      "1, 0.0, 100, 0, false, true", // +1 = 1 fails, rate=0.99% >= 0%, min=1 -> trigger
      "1, 0.0, 100, 0, true, false", // +0 = 0 fails, 0 < 1 min -> no trigger

      // === High minimum failures cases ===
      "3, 50.0, 3, 1, false, false", // +1 = 2 fails, 2 < 3 min -> no trigger
      "3, 50.0, 3, 1, true, false",  // +0 = 1 fails, 1 < 3 min -> no trigger
      "1000, 1.0, 198, 2, false, false", // +1 = 3 fails, 3 < 1000 min -> no trigger
      "1000, 1.0, 198, 2, true, false",  // +0 = 2 fails, 2 < 1000 min -> no trigger

      // === Corner cases ===
      "0, 50.0, 0, 0, false, true",  // +1 = 1 fails, rate=100% >= 50%, min=0 -> trigger
      "0, 50.0, 0, 0, true, false",  // +0 = 0 fails, no calls -> no trigger
      "1, 50.0, 1, 1, false, true",  // +1 = 2 fails, rate=66.7% >= 50%, min=1 -> trigger
      "1, 50.0, 1, 1, true, true",   // +0 = 1 fails, rate=50% >= 50%, min=1 -> trigger
      "2, 33.0, 2, 1, false, true",  // +1 = 2 fails, rate=50% >= 33%, min=2 -> trigger
      "2, 33.0, 2, 1, true, false",  // +0 = 1 fails, 1 < 2 min -> no trigger
      "5, 20.0, 20, 4, false, true", // +1 = 5 fails, rate=20% >= 20%, min=5 -> trigger
      "5, 20.0, 20, 4, true, false", // +0 = 4 fails, 4 < 5 min -> no trigger
      "3, 75.0, 1, 2, false, true",  // +1 = 3 fails, rate=75% >= 75%, min=3 -> trigger
      "3, 75.0, 1, 2, true, false",  // +0 = 2 fails, 2 < 3 min -> no trigger
      })
  public void thresholdMatrix(int minFailures, float ratePercent, int successes, int failures,
      boolean lastFailRecorded, boolean expectOpenState) {

    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(minFailures);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(successes);
    when(metrics.getNumberOfFailedCalls()).thenReturn(failures);
    when(cluster.getCircuitBreakerFailureRateThreshold()).thenReturn(ratePercent);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    cluster.evaluateThresholds(lastFailRecorded);

    if (expectOpenState) {
      verify(circuitBreaker, times(1)).transitionToOpenState();
    } else {
      verify(circuitBreaker, never()).transitionToOpenState();
    }
  }

}
