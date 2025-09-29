package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
public class CircuitBreakerFailoverBaseTests {

  private MultiClusterPooledConnectionProvider provider;
  private Cluster cluster;
  private CircuitBreaker circuitBreaker;
  private CircuitBreakerConfig cbConfig;
  private CircuitBreaker.Metrics metrics;
  private TestableCBBase base;

  // Minimal wrapper to access protected methods of the base class
  private static class TestableCBBase extends CircuitBreakerFailoverBase {
    TestableCBBase(MultiClusterPooledConnectionProvider provider) {
      super(provider);
    }

    static void eval(Cluster c) {
      evaluateThresholds(c);
    }

    void doFailover(Cluster c) {
      clusterFailover(c);
    }
  }

  @BeforeEach
  public void setup() {
    provider = mock(MultiClusterPooledConnectionProvider.class);
    cluster = mock(Cluster.class);

    circuitBreaker = mock(CircuitBreaker.class);
    cbConfig = mock(CircuitBreakerConfig.class);
    metrics = mock(CircuitBreaker.Metrics.class);

    when(cluster.getCircuitBreaker()).thenReturn(circuitBreaker);
    when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(cbConfig);
    when(circuitBreaker.getMetrics()).thenReturn(metrics);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    base = new TestableCBBase(provider);
  }

  /**
   * Below minimum failures: even if all calls are failures, failover should NOT trigger.
   */
  @Test
  public void belowMinFailures_doesNotFailover() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(2);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
    when(cbConfig.getFailureRateThreshold()).thenReturn(50.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    TestableCBBase.eval(cluster);

    verify(circuitBreaker, never()).transitionToOpenState();
    verify(provider, never()).switchToHealthyCluster(any(), any());
  }

  /**
   * Reaching minFailures and exceeding failure rate threshold should trigger failover.
   */
  @Test
  public void minFailuresAndRateExceeded_triggersFailover() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(3);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
    when(cbConfig.getFailureRateThreshold()).thenReturn(50.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
    when(provider.getCluster()).thenReturn(cluster);

    TestableCBBase.eval(cluster);
    verify(circuitBreaker, times(1)).transitionToOpenState();

    base.doFailover(cluster);
    verify(cluster, times(1)).setGracePeriod();
    verify(circuitBreaker, times(1)).transitionToForcedOpenState();
    verify(provider, times(1)).switchToHealthyCluster(eq(SwitchReason.CIRCUIT_BREAKER),
      eq(cluster));
  }

  /**
   * Even after reaching minFailures, if failure rate is below threshold, do not failover.
   */
  @Test
  public void rateBelowThreshold_doesNotFailover() {
    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(3);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(3);
    when(metrics.getNumberOfFailedCalls()).thenReturn(3);
    when(cbConfig.getFailureRateThreshold()).thenReturn(80.0f);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    TestableCBBase.eval(cluster);

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
      // minFailures, ratePercent, successes, failures, expectFailoverOnNext
      "0, 1.0, 0, 1, true", //
      "1, 1.0, 0, 1, true", //
      "3, 50.0, 0, 3, true", //
      "1, 100.0, 0, 1, true", //
      "0, 100.0, 99, 1, false", //
      "0, 1.0, 99, 1, true", //
      // additional edge cases
      "1, 0.0, 0, 1, true", //
      "3, 50.0, 3, 2, false", //
      "1000, 1.0, 198, 2, false", })
  public void thresholdMatrix(int minFailures, float ratePercent, int successes, int failures,
      boolean expectFailoverOnNext) {

    when(cluster.getCircuitBreakerMinNumOfFailures()).thenReturn(minFailures);
    when(metrics.getNumberOfSuccessfulCalls()).thenReturn(successes);
    when(metrics.getNumberOfFailedCalls()).thenReturn(failures);
    when(cbConfig.getFailureRateThreshold()).thenReturn(ratePercent);
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    TestableCBBase.eval(cluster);

    if (expectFailoverOnNext) {
      verify(circuitBreaker, times(1)).transitionToOpenState();
      when(provider.getCluster()).thenReturn(cluster);
      base.doFailover(cluster);
      verify(circuitBreaker, times(1)).transitionToForcedOpenState();
      verify(provider, times(1)).switchToHealthyCluster(eq(SwitchReason.CIRCUIT_BREAKER),
        eq(cluster));
    } else {
      verify(circuitBreaker, never()).transitionToOpenState();
      verify(provider, never()).switchToHealthyCluster(any(), any());
    }
  }

}
