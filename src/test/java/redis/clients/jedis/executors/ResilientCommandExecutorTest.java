package redis.clients.jedis.executors;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisException;

public class ResilientCommandExecutorTest {

  // --- Constructor validation ---

  @Test
  public void constructorRejectsZeroMaxAttempts() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new TestableExecutor(0, Duration.ofSeconds(1)));
    assertTrue(ex.getMessage().contains("maxAttempts"));
  }

  @Test
  public void constructorRejectsNegativeMaxAttempts() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new TestableExecutor(-1, Duration.ofSeconds(1)));
    assertTrue(ex.getMessage().contains("maxAttempts"));
  }

  @Test
  public void constructorRejectsNullDuration() {
    assertThrows(IllegalArgumentException.class,
        () -> new TestableExecutor(3, null));
  }

  @Test
  public void constructorAcceptsValidParameters() {
    assertDoesNotThrow(() -> new TestableExecutor(1, Duration.ZERO));
    assertDoesNotThrow(() -> new TestableExecutor(5, Duration.ofSeconds(10)));
  }

  // --- Getters ---

  @Test
  public void gettersReturnConstructorValues() {
    TestableExecutor executor = new TestableExecutor(7, Duration.ofSeconds(42));
    assertEquals(7, executor.getMaxAttempts());
    assertEquals(Duration.ofSeconds(42), executor.getMaxTotalRetriesDuration());
  }

  // --- millisUntil ---

  @Test
  public void millisUntilReturnPositiveForFutureDeadline() {
    Instant future = Instant.now().plusSeconds(5);
    long result = ResilientCommandExecutor.millisUntil(future);
    assertTrue(result > 0, "Should return positive millis for future deadline");
    assertTrue(result <= 5000, "Should not exceed 5000ms");
  }

  @Test
  public void millisUntilReturnsZeroForPastDeadline() {
    Instant past = Instant.now().minusSeconds(5);
    assertEquals(0, ResilientCommandExecutor.millisUntil(past));
  }

  @Test
  public void millisUntilReturnsZeroForNow() {
    // Instant.now() might already be past by the time millisUntil runs
    long result = ResilientCommandExecutor.millisUntil(Instant.now());
    assertTrue(result >= 0, "Should return 0 or very small positive value");
  }

  // --- computeBackoffMillis ---

  @Test
  public void computeBackoffReturnsZeroWhenNoAttemptsLeft() {
    Instant future = Instant.now().plusSeconds(10);
    assertEquals(0, ResilientCommandExecutor.computeBackoffMillis(0, future));
  }

  @Test
  public void computeBackoffReturnsZeroWhenDeadlinePassed() {
    Instant past = Instant.now().minusSeconds(5);
    assertEquals(0, ResilientCommandExecutor.computeBackoffMillis(3, past));
  }

  @Test
  public void computeBackoffReturnsBoundedValue() {
    Instant deadline = Instant.now().plusMillis(1000);
    int attemptsLeft = 2;
    // maxBackoff = 1000 / (2*2) = 250, so result should be in [0, 250]
    for (int i = 0; i < 100; i++) {
      long backoff = ResilientCommandExecutor.computeBackoffMillis(attemptsLeft, deadline);
      assertTrue(backoff >= 0, "Backoff must be non-negative");
      assertTrue(backoff <= 250, "Backoff must not exceed millisLeft / attemptsLeft²");
    }
  }

  @Test
  public void computeBackoffDecreasesWithMoreAttemptsLeft() {
    Instant deadline = Instant.now().plusMillis(10_000);
    // With more attempts left, max backoff is smaller (time is spread out)
    // maxBackoff for 10 attempts = 10000 / 100 = 100
    // maxBackoff for 2 attempts  = 10000 / 4   = 2500
    // Run many iterations to verify trend holds on average
    long sumMany = 0, sumFew = 0;
    int iterations = 1000;
    for (int i = 0; i < iterations; i++) {
      sumMany += ResilientCommandExecutor.computeBackoffMillis(10, deadline);
      sumFew += ResilientCommandExecutor.computeBackoffMillis(2, deadline);
    }
    assertTrue(sumFew / iterations > sumMany / iterations,
        "Fewer attempts left should produce larger average backoff");
  }

  // --- sleep ---

  @Test
  public void sleepDoesNothingForZeroMillis() {
    TestableExecutor executor = new TestableExecutor(1, Duration.ZERO);
    long start = System.nanoTime();
    executor.sleep(0);
    long elapsed = (System.nanoTime() - start) / 1_000_000;
    assertTrue(elapsed < 50, "sleep(0) should be a no-op");
  }

  @Test
  public void sleepDoesNothingForNegativeMillis() {
    TestableExecutor executor = new TestableExecutor(1, Duration.ZERO);
    assertDoesNotThrow(() -> executor.sleep(-10));
  }

  @Test
  public void sleepWrapsInterruptionAndPreservesInterruptStatus() {
    TestableExecutor executor = new TestableExecutor(1, Duration.ZERO);
    Thread.currentThread().interrupt();
    JedisException ex = assertThrows(JedisException.class, () -> executor.sleep(10_000));
    assertInstanceOf(InterruptedException.class, ex.getCause());
    // interrupt flag should be restored
    assertTrue(Thread.currentThread().isInterrupted());
    // Clear for subsequent tests
    Thread.interrupted();
  }

  // --- Concrete stub for testing abstract class ---

  private static class TestableExecutor extends ResilientCommandExecutor {
    TestableExecutor(int maxAttempts, Duration maxTotalRetriesDuration) {
      super(maxAttempts, maxTotalRetriesDuration);
    }

    @Override public <T> T executeCommand(redis.clients.jedis.CommandObject<T> commandObject) {
      throw new UnsupportedOperationException("stub");
    }

    @Override public void close() { }
  }
}
