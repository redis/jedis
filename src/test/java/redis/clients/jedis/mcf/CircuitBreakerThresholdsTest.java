package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;

/**
 * Tests for circuit breaker thresholds: both failure-rate threshold and minimum number of failures
 * must be exceeded to trigger failover. Uses a real CircuitBreaker and real Retry, but mocks the
 * provider and cluster wiring to avoid network I/O.
 */
public class CircuitBreakerThresholdsTest {

  private final EndpointConfig endpoint0 = HostAndPorts.getRedisEndpoint("standalone0");

  private MultiClusterPooledConnectionProvider provider;
  private Cluster cluster;
  private CircuitBreaker circuitBreaker;
  private Retry retry;
  private CircuitBreakerCommandExecutor executor;
  private CommandObject<String> dummyCommand;

  @BeforeEach
  public void setup() {
    provider = mock(MultiClusterPooledConnectionProvider.class);
    cluster = mock(Cluster.class);

    // Real CircuitBreaker with small window and tracking JedisConnectionException as failures
    circuitBreaker = CircuitBreaker.of("cb-thresholds-test",
      CircuitBreakerConfig.custom().failureRateThreshold(50.0f).slidingWindowSize(10)
          .recordExceptions(JedisConnectionException.class).build());

    // Real Retry (single attempt per execute to make sequencing explicit)
    retry = Retry.of("retry-thresholds-test",
      RetryConfig.custom().maxAttempts(1).waitDuration(Duration.ZERO)
          .retryExceptions(JedisConnectionException.class).failAfterMaxAttempts(false).build());

    when(cluster.getCircuitBreaker()).thenReturn(circuitBreaker);
    when(cluster.getRetry()).thenReturn(retry);
    when(provider.getCluster()).thenReturn(cluster);
    when(provider.getFallbackExceptionList()).thenReturn(Arrays
        .asList(CallNotPermittedException.class, JedisFailoverThresholdsExceededException.class));
    when(provider.canIterateOnceMore()).thenReturn(false); // make failover throw on 2nd attempt

    // Provide threshold via cluster
    when(cluster.getThresholdMinNumOfFailures()).thenReturn(3);

    executor = new CircuitBreakerCommandExecutor(provider);

    // Construct a minimal CommandObject without hitting a real Redis
    dummyCommand = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
        BuilderFactory.STRING);
  }

  /**
   * Below minimum failures: even if all calls are failures, failover should NOT trigger.
   */
  @Test
  public void belowMinFailures_doesNotFailover() {
    when(cluster.getThresholdMinNumOfFailures()).thenReturn(3);

    // Always failing connection
    Connection failing = mock(Connection.class);
    when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    // Ensure close() is safe
    doNothing().when(failing).close();
    when(cluster.getConnection()).thenReturn(failing);

    // Two failing calls (< minFailures)
    for (int i = 0; i < 2; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }

    // Verify CB recorded failures but no failover attempted
    assertEquals(2, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    verify(provider, never()).iterateActiveCluster(any());
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  /**
   * Reaching minFailures and exceeding failure rate threshold should trigger failover.
   */
  @Test
  public void minFailuresAndRateExceeded_triggersFailover() {
    when(cluster.getThresholdMinNumOfFailures()).thenReturn(3);

    // Always failing connection
    Connection failing = mock(Connection.class);
    when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    doNothing().when(failing).close();
    when(cluster.getConnection()).thenReturn(failing);

    // Three calls to reach min failures; threshold check happens on the NEXT call
    for (int i = 0; i < 3; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }

    // Fourth call should see minFailures satisfied and failure rate >= 50%, triggering failover
    Exception ex = assertThrows(Exception.class, () -> executor.executeCommand(dummyCommand));

    // After threshold exceeded, the fallback attempts failover once and then throws
    verify(provider, times(1)).iterateActiveCluster(eq(SwitchReason.CIRCUIT_BREAKER));
    assertEquals(CircuitBreaker.State.FORCED_OPEN, circuitBreaker.getState());
    // The final surfaced exception should be JedisConnectionException from failover path
    assertTrue(
      ex instanceof JedisConnectionException || ex.getCause() instanceof JedisConnectionException);
  }

  /**
   * Even after reaching minFailures, if failure rate is below threshold, do not failover.
   */
  @Test
  public void rateBelowThreshold_doesNotFailover() {
    when(cluster.getThresholdMinNumOfFailures()).thenReturn(3);

    // Success connection
    Connection success = mock(Connection.class);
    when(success.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenAnswer(inv -> "OK");
    doNothing().when(success).close();

    // Failing connection
    Connection failing = mock(Connection.class);
    when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    doNothing().when(failing).close();

    // Rebuild a fresh environment with 80% threshold and record successes first
    CircuitBreaker highRateCB = CircuitBreaker.of("cb-high-rate",
      CircuitBreakerConfig.custom().failureRateThreshold(80.0f).slidingWindowSize(10)
          .recordExceptions(JedisConnectionException.class).build());
    when(cluster.getCircuitBreaker()).thenReturn(highRateCB);
    executor = new CircuitBreakerCommandExecutor(provider);

    // First record 3 successes on the same CB
    when(cluster.getConnection()).thenReturn(success);
    for (int i = 0; i < 3; i++) {
      executor.executeCommand(dummyCommand);
    }

    // Then record 3 failures -> failure rate = 50% which is below 80%
    when(cluster.getConnection()).thenReturn(failing);
    for (int i = 0; i < 3; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }
    verify(provider, never()).iterateActiveCluster(any());
    assertEquals(CircuitBreaker.State.CLOSED, highRateCB.getState());
  }

  @Test
  public void providerBuilder_zeroRate_mapsToHundredAndHugeMinCalls() {
    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(Arrays.asList(MultiClusterClientConfig.ClusterConfig
            .builder(new HostAndPort("localhost", 6379), DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).build()));
    cfgBuilder.circuitBreakerFailureRateThreshold(0.0f).thresholdMinNumOfFailures(3)
        .circuitBreakerSlidingWindowSize(10);
    MultiClusterClientConfig mcc = cfgBuilder.build();

    MultiClusterPooledConnectionProvider realProvider = new MultiClusterPooledConnectionProvider(
        mcc);
    try {
      CircuitBreaker cb = realProvider.getClusterCircuitBreaker();
      CircuitBreakerConfig cbc = cb.getCircuitBreakerConfig();

      assertEquals(100.0f, cbc.getFailureRateThreshold(), 0.0001f);
      // For rate=0.0 we expect extremely large min-calls so CB won't open on its own
      assertTrue(cbc.getMinimumNumberOfCalls() >= 1_000_000);
    } finally {
      realProvider.close();
    }
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

    // Build config via MultiClusterClientConfig builder and a real provider consistently for all
    // rows
    int window = Math.max(10, successes + failures + 2);
    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(Arrays.asList(MultiClusterClientConfig.ClusterConfig
            .builder(endpoint0.getHostAndPort(), endpoint0.getClientConfigBuilder().build())
            .healthCheckEnabled(false).build()));

    cfgBuilder.thresholdMinNumOfFailures(minFailures)
        .circuitBreakerFailureRateThreshold(ratePercent).circuitBreakerSlidingWindowSize(window);

    MultiClusterClientConfig mcc = cfgBuilder.build();

    MultiClusterPooledConnectionProvider realProvider = new MultiClusterPooledConnectionProvider(
        mcc);
    CircuitBreaker realCb = realProvider.getClusterCircuitBreaker();

    // Spy the provider so we can supply mocked connections without network I/O
    MultiClusterPooledConnectionProvider spyProvider = spy(realProvider);

    // Build a mock cluster that uses the real CB and a real 1-attempt Retry
    Retry oneAttemptRetry = Retry.of("retry-matrix",
      RetryConfig.custom().maxAttempts(1).waitDuration(Duration.ZERO)
          .retryExceptions(JedisConnectionException.class).failAfterMaxAttempts(false).build());

    MultiClusterPooledConnectionProvider.Cluster mockCluster = mock(
      MultiClusterPooledConnectionProvider.Cluster.class);
    when(mockCluster.getCircuitBreaker()).thenReturn(realCb);
    when(mockCluster.getRetry()).thenReturn(oneAttemptRetry);
    when(mockCluster.getThresholdMinNumOfFailures()).thenReturn(minFailures);
    when(mockCluster.retryOnFailover()).thenReturn(false);

    // Provide fallback list from the real provider
    doReturn(realProvider.getFallbackExceptionList()).when(spyProvider).getFallbackExceptionList();
    // Prevent actual iteration logic, but allow verify()
    doReturn(false).when(spyProvider).canIterateOnceMore();
    doReturn(null).when(spyProvider).iterateActiveCluster(any());
    // Always return our mock cluster to avoid opening sockets
    doReturn(mockCluster).when(spyProvider).getCluster();

    // Use executor with the spy provider (real CB, mocked connections)
    executor = new CircuitBreakerCommandExecutor(spyProvider);

    // Success path
    Connection successConn = mock(Connection.class);
    when(successConn.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenAnswer(inv -> "OK");
    doNothing().when(successConn).close();

    // Failure path
    Connection failConn = mock(Connection.class);
    when(failConn.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    doNothing().when(failConn).close();

    // First, record successes
    when(mockCluster.getConnection()).thenReturn(successConn);
    for (int i = 0; i < successes; i++) {
      executor.executeCommand(dummyCommand);
    }

    // Then, record failures
    when(mockCluster.getConnection()).thenReturn(failConn);
    for (int i = 0; i < failures; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }

    verify(spyProvider, never()).iterateActiveCluster(any());

    // Now, one more failing call — this is where thresholds are checked
    Exception ex = assertThrows(Exception.class, () -> executor.executeCommand(dummyCommand));

    if (expectFailoverOnNext) {
      verify(spyProvider, times(1)).iterateActiveCluster(eq(SwitchReason.CIRCUIT_BREAKER));
      assertEquals(CircuitBreaker.State.FORCED_OPEN, realCb.getState());
      assertTrue(ex instanceof JedisConnectionException
          || ex.getCause() instanceof JedisConnectionException);
    } else {
      verify(spyProvider, never()).iterateActiveCluster(any());
      assertEquals(CircuitBreaker.State.CLOSED, realCb.getState());
      assertTrue(ex instanceof JedisConnectionException);
    }

    // Cleanup
    realProvider.close();
  }

}
