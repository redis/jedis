package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider.Cluster;

/**
 * Tests for circuit breaker thresholds: both failure-rate threshold and minimum number of failures
 * must be exceeded to trigger failover. Uses a real CircuitBreaker and real Retry, but mocks the
 * provider and cluster wiring to avoid network I/O.
 */
public class CircuitBreakerThresholdsTest {

  private MultiClusterPooledConnectionProvider realProvider;
  private MultiClusterPooledConnectionProvider spyProvider;
  private Cluster cluster;
  private CircuitBreakerCommandExecutor executor;
  private CommandObject<String> dummyCommand;
  private TrackingConnectionPool poolMock;
  private HostAndPort fakeEndpoint = new HostAndPort("fake", 6379);
  private HostAndPort fakeEndpoint2 = new HostAndPort("fake2", 6379);
  private ClusterConfig[] fakeClusterConfigs;

  @BeforeEach
  public void setup() throws Exception {

    ClusterConfig[] clusterConfigs = new ClusterConfig[] {
        ClusterConfig.builder(fakeEndpoint, DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).weight(1.0f).build(),
        ClusterConfig.builder(fakeEndpoint2, DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).weight(0.5f).build() };
    fakeClusterConfigs = clusterConfigs;

    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig.builder(clusterConfigs)
        .circuitBreakerFailureRateThreshold(50.0f).circuitBreakerMinNumOfFailures(3)
        .circuitBreakerSlidingWindowSize(10).retryMaxAttempts(1).retryOnFailover(false);

    MultiClusterClientConfig mcc = cfgBuilder.build();

    realProvider = new MultiClusterPooledConnectionProvider(mcc);
    spyProvider = spy(realProvider);

    cluster = spyProvider.getCluster();

    executor = new CircuitBreakerCommandExecutor(spyProvider);

    dummyCommand = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
        BuilderFactory.STRING);

    // Replace the cluster's pool with a mock to avoid real network I/O
    poolMock = mock(TrackingConnectionPool.class);
    Field f = MultiClusterPooledConnectionProvider.Cluster.class.getDeclaredField("connectionPool");
    f.setAccessible(true);
    f.set(cluster, poolMock);
  }

  /**
   * Below minimum failures; even if all calls are failures, failover should NOT trigger.
   */
  @Test
  public void belowMinFailures_doesNotFailover() {
    // Always failing connections
    Connection failing = mock(Connection.class);
    when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    doNothing().when(failing).close();
    when(poolMock.getResource()).thenReturn(failing);

    for (int i = 0; i < 2; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }

    // Below min failures; CB remains CLOSED
    assertEquals(CircuitBreaker.State.CLOSED, spyProvider.getClusterCircuitBreaker().getState());
  }

  /**
   * Reaching minFailures and exceeding failure rate threshold should trigger failover.
   */
  @Test
  public void minFailuresAndRateExceeded_triggersFailover() {
    // Always failing connections
    Connection failing = mock(Connection.class);
    when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
        .thenThrow(new JedisConnectionException("fail"));
    doNothing().when(failing).close();
    when(poolMock.getResource()).thenReturn(failing);

    // Reach min failures and exceed rate threshold
    for (int i = 0; i < 3; i++) {
      assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));
    }

    // Next call should hit open CB (CallNotPermitted) and trigger failover
    assertThrows(JedisConnectionException.class, () -> executor.executeCommand(dummyCommand));

    verify(spyProvider, atLeastOnce()).switchToHealthyCluster(eq(SwitchReason.CIRCUIT_BREAKER),
      any());
    assertEquals(CircuitBreaker.State.FORCED_OPEN,
      spyProvider.getCluster(fakeEndpoint).getCircuitBreaker().getState());
  }

  /**
   * Even after reaching minFailures, if failure rate is below threshold, do not failover.
   */
  @Test
  public void rateBelowThreshold_doesNotFailover() throws Exception {
    // Use local provider with higher threshold (80%) and no retries
    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(fakeClusterConfigs).circuitBreakerFailureRateThreshold(80.0f)
        .circuitBreakerMinNumOfFailures(3).circuitBreakerSlidingWindowSize(10).retryMaxAttempts(1)
        .retryOnFailover(false);
    MultiClusterPooledConnectionProvider rp = new MultiClusterPooledConnectionProvider(
        cfgBuilder.build());
    MultiClusterPooledConnectionProvider sp = spy(rp);
    Cluster c = sp.getCluster();
    try (CircuitBreakerCommandExecutor ex = new CircuitBreakerCommandExecutor(sp)) {
      CommandObject<String> cmd = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
          BuilderFactory.STRING);

      TrackingConnectionPool pool = mock(TrackingConnectionPool.class);
      Field f = MultiClusterPooledConnectionProvider.Cluster.class
          .getDeclaredField("connectionPool");
      f.setAccessible(true);
      f.set(c, pool);

      // 3 successes
      Connection success = mock(Connection.class);
      when(success.executeCommand(org.mockito.Mockito.<CommandObject<String>> any()))
          .thenReturn("PONG");
      doNothing().when(success).close();
      when(pool.getResource()).thenReturn(success);
      for (int i = 0; i < 3; i++) {
        assertEquals("PONG", ex.executeCommand(cmd));
      }

      // 3 failures -> total 6 calls, 50% failure rate; threshold 80% means stay CLOSED
      Connection failing = mock(Connection.class);
      when(failing.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
          .thenThrow(new JedisConnectionException("fail"));
      doNothing().when(failing).close();
      when(pool.getResource()).thenReturn(failing);
      for (int i = 0; i < 3; i++) {
        assertThrows(JedisConnectionException.class, () -> ex.executeCommand(cmd));
      }

      assertEquals(CircuitBreaker.State.CLOSED, sp.getClusterCircuitBreaker().getState());
    }
  }

  @Test
  public void providerBuilder_zeroRate_mapsToHundredAndHugeMinCalls() {
    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(fakeClusterConfigs);
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
      boolean expectFailoverOnNext) throws Exception {

    MultiClusterClientConfig.Builder cfgBuilder = MultiClusterClientConfig
        .builder(fakeClusterConfigs).circuitBreakerFailureRateThreshold(ratePercent)
        .circuitBreakerMinNumOfFailures(minFailures)
        .circuitBreakerSlidingWindowSize(Math.max(10, successes + failures + 2)).retryMaxAttempts(1)
        .retryOnFailover(false);

    MultiClusterPooledConnectionProvider real = new MultiClusterPooledConnectionProvider(
        cfgBuilder.build());
    MultiClusterPooledConnectionProvider spy = spy(real);
    Cluster c = spy.getCluster();
    try (CircuitBreakerCommandExecutor ex = new CircuitBreakerCommandExecutor(spy)) {

      CommandObject<String> cmd = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
          BuilderFactory.STRING);

      TrackingConnectionPool pool = mock(TrackingConnectionPool.class);
      Field f = MultiClusterPooledConnectionProvider.Cluster.class
          .getDeclaredField("connectionPool");
      f.setAccessible(true);
      f.set(c, pool);

      if (successes > 0) {
        Connection ok = mock(Connection.class);
        when(ok.executeCommand(org.mockito.Mockito.<CommandObject<String>> any()))
            .thenReturn("PONG");
        doNothing().when(ok).close();
        when(pool.getResource()).thenReturn(ok);
        for (int i = 0; i < successes; i++) {
          ex.executeCommand(cmd);
        }
      }

      if (failures > 0) {
        Connection bad = mock(Connection.class);
        when(bad.executeCommand(org.mockito.Mockito.<CommandObject<?>> any()))
            .thenThrow(new JedisConnectionException("fail"));
        doNothing().when(bad).close();
        when(pool.getResource()).thenReturn(bad);
        for (int i = 0; i < failures; i++) {
          try {
            ex.executeCommand(cmd);
          } catch (Exception ignore) {
          }
        }
      }

      if (expectFailoverOnNext) {
        assertThrows(Exception.class, () -> ex.executeCommand(cmd));
        verify(spy, atLeastOnce()).switchToHealthyCluster(eq(SwitchReason.CIRCUIT_BREAKER), any());
        assertEquals(CircuitBreaker.State.FORCED_OPEN, c.getCircuitBreaker().getState());
      } else {
        CircuitBreaker.State st = c.getCircuitBreaker().getState();
        assertTrue(st == CircuitBreaker.State.CLOSED || st == CircuitBreaker.State.HALF_OPEN);
      }
    }
  }

}
