package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;

/**
 * Regression tests for connection-lifecycle handling in {@link MultiDbConnectionSupplier}.
 */
public class MultiDbConnectionSupplierTest {

  /**
   * Reproduces a connection-pool leak in {@link MultiDbConnectionSupplier#getConnection()}.
   * <p>
   * The supplier borrows a {@link Connection} from the database pool and then validates it with
   * {@code connection.ping()}. The call sits inside a Resilience4j retry / circuit-breaker /
   * fallback chain, so every failed attempt re-runs the supplier and borrows a fresh connection.
   * Because the {@code ping()} call is not guarded by a try/finally, a {@code ping()} failure
   * leaves the borrowed connection without a {@code close()} — it never returns to the pool, and
   * each retry attempt (and each failover hop) leaks one more connection.
   * <p>
   * Expectation: every connection borrowed from the pool must be {@code close()}d before the
   * exception escapes. With the bug in place none are closed and this test fails on the final
   * verification block.
   */
  @Test
  void pingFailureDuringConnectionAcquisitionLeaksPooledConnections() {
    int maxAttempts = 3;

    Retry retry = RetryRegistry
        .of(RetryConfig.custom().maxAttempts(maxAttempts).waitDuration(Duration.ofMillis(1))
            .failAfterMaxAttempts(false).retryExceptions(JedisConnectionException.class).build())
        .retry("leak-test");

    // Keep the circuit breaker CLOSED for the duration of the test so retries fully exhaust
    // and the original JedisConnectionException propagates (rather than a CallNotPermitted).
    CircuitBreaker circuitBreaker = CircuitBreakerRegistry
        .of(CircuitBreakerConfig.custom().minimumNumberOfCalls(1000).build())
        .circuitBreaker("leak-test");

    Database database = mock(Database.class);
    when(database.getRetry()).thenReturn(retry);
    when(database.getCircuitBreaker()).thenReturn(circuitBreaker);

    List<Connection> borrowed = new ArrayList<>();
    when(database.getConnection()).thenAnswer(inv -> {
      Connection conn = mock(Connection.class);
      when(conn.ping()).thenThrow(new JedisConnectionException("simulated ping failure"));
      borrowed.add(conn);
      return conn;
    });

    MultiDbConnectionProvider provider = mock(MultiDbConnectionProvider.class);
    when(provider.getDatabase()).thenReturn(database);
    when(provider.getFallbackExceptionList())
        .thenReturn(Collections.<Class<? extends Throwable>> emptyList());

    MultiDbConnectionSupplier supplier = new MultiDbConnectionSupplier(provider);

    assertThrows(JedisConnectionException.class, supplier::getConnection);

    assertEquals(maxAttempts, borrowed.size(), "Expected one connection borrow per retry attempt");

    for (Connection connection : borrowed) {
      verify(connection, times(1)).close();
    }
  }
}
