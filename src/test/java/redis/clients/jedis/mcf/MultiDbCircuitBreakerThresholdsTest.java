package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

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
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Tests for circuit breaker thresholds: both failure-rate threshold and minimum number of failures
 * must be exceeded to trigger failover. Uses a real CircuitBreaker and real Retry, but mocks the
 * provider and cluster wiring to avoid network I/O.
 */
public class MultiDbCircuitBreakerThresholdsTest {

  private MultiDbConnectionProvider realProvider;
  private MultiDbConnectionProvider spyProvider;
  private Database cluster;
  private MultiDbCommandExecutor executor;
  private CommandObject<String> dummyCommand;
  private TrackingConnectionPool poolMock;
  private HostAndPort fakeEndpoint = new HostAndPort("fake", 6379);
  private HostAndPort fakeEndpoint2 = new HostAndPort("fake2", 6379);
  private DatabaseConfig[] fakeDatabaseConfigs;

  @BeforeEach
  public void setup() throws Exception {

    DatabaseConfig[] databaseConfigs = new DatabaseConfig[] {
        DatabaseConfig.builder(fakeEndpoint, DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).weight(1.0f).build(),
        DatabaseConfig.builder(fakeEndpoint2, DefaultJedisClientConfig.builder().build())
            .healthCheckEnabled(false).weight(0.5f).build() };
    fakeDatabaseConfigs = databaseConfigs;

    MultiDbConfig.Builder cfgBuilder = MultiDbConfig.builder(databaseConfigs)
        .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().failureRateThreshold(50.0f)
            .minNumOfFailures(3).slidingWindowSize(10).build())
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
        .retryOnFailover(false);

    MultiDbConfig mcc = cfgBuilder.build();

    realProvider = new MultiDbConnectionProvider(mcc);
    spyProvider = spy(realProvider);

    cluster = spyProvider.getDatabase();

    executor = new MultiDbCommandExecutor(spyProvider);

    dummyCommand = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
        BuilderFactory.STRING);

    // Replace the cluster's pool with a mock to avoid real network I/O
    poolMock = mock(TrackingConnectionPool.class);
    ReflectionTestUtil.setField(cluster, "connectionPool", poolMock);
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
    assertEquals(CircuitBreaker.State.CLOSED, spyProvider.getDatabaseCircuitBreaker().getState());
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

    verify(spyProvider, atLeastOnce()).switchToHealthyDatabase(eq(SwitchReason.CIRCUIT_BREAKER),
      any());
    assertEquals(CircuitBreaker.State.FORCED_OPEN,
      spyProvider.getDatabase(fakeEndpoint).getCircuitBreaker().getState());
  }

  /**
   * Even after reaching minFailures, if failure rate is below threshold, do not failover.
   */
  @Test
  public void rateBelowThreshold_doesNotFailover() throws Exception {
    // Use local provider with higher threshold (80%) and no retries
    MultiDbConfig.Builder cfgBuilder = MultiDbConfig.builder(fakeDatabaseConfigs)
        .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().failureRateThreshold(80.0f)
            .minNumOfFailures(3).slidingWindowSize(10).build())
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
        .retryOnFailover(false);
    MultiDbConnectionProvider rp = new MultiDbConnectionProvider(cfgBuilder.build());
    MultiDbConnectionProvider sp = spy(rp);
    Database c = sp.getDatabase();
    try (MultiDbCommandExecutor ex = new MultiDbCommandExecutor(sp)) {
      CommandObject<String> cmd = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
          BuilderFactory.STRING);

      TrackingConnectionPool pool = mock(TrackingConnectionPool.class);
      ReflectionTestUtil.setField(c, "connectionPool", pool);

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

      assertEquals(CircuitBreaker.State.CLOSED, sp.getDatabaseCircuitBreaker().getState());
    }
  }

  @Test
  public void providerBuilder_zeroRate_mapsToHundredAndHugeMinCalls() {
    MultiDbConfig.Builder cfgBuilder = MultiDbConfig.builder(fakeDatabaseConfigs);
    cfgBuilder.failureDetector(MultiDbConfig.CircuitBreakerConfig.builder()
        .failureRateThreshold(0.0f).minNumOfFailures(3).slidingWindowSize(10).build());
    MultiDbConfig mcc = cfgBuilder.build();

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

    MultiDbConfig.Builder cfgBuilder = MultiDbConfig.builder(fakeDatabaseConfigs)
        .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder()
            .failureRateThreshold(ratePercent).minNumOfFailures(minFailures)
            .slidingWindowSize(Math.max(10, successes + failures + 2)).build())
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
        .retryOnFailover(false);

    MultiDbConnectionProvider real = new MultiDbConnectionProvider(cfgBuilder.build());
    MultiDbConnectionProvider spy = spy(real);
    Database c = spy.getDatabase();
    try (MultiDbCommandExecutor ex = new MultiDbCommandExecutor(spy)) {

      CommandObject<String> cmd = new CommandObject<>(new CommandArguments(Protocol.Command.PING),
          BuilderFactory.STRING);

      TrackingConnectionPool pool = mock(TrackingConnectionPool.class);
      ReflectionTestUtil.setField(c, "connectionPool", pool);

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
        verify(spy, atLeastOnce()).switchToHealthyDatabase(eq(SwitchReason.CIRCUIT_BREAKER), any());
        assertEquals(CircuitBreaker.State.FORCED_OPEN, c.getCircuitBreaker().getState());
      } else {
        CircuitBreaker.State st = c.getCircuitBreaker().getState();
        assertTrue(st == CircuitBreaker.State.CLOSED || st == CircuitBreaker.State.HALF_OPEN);
      }
    }
  }

}
