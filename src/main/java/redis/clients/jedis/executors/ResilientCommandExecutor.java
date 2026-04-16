package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Abstract base class for {@link CommandExecutor} implementations that support automatic retry
 * logic for transient connection failures.
 * <p>
 * This class provides the shared constructor contract, fields, and utility methods for resilient
 * executors, ensuring consistent validation, backoff, and sleep behaviour across all executor
 * implementations that support retries.
 * </p>
 */
public abstract class ResilientCommandExecutor implements CommandExecutor {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;

  /**
   * Constructs a resilient command executor with the specified retry parameters.
   *
   * @param maxAttempts the maximum number of attempts (must be &gt; 0)
   * @param maxTotalRetriesDuration the maximum total duration for all retry attempts (must not be null)
   * @throws IllegalArgumentException if maxAttempts is not positive or maxTotalRetriesDuration is null
   */
  protected ResilientCommandExecutor(int maxAttempts, Duration maxTotalRetriesDuration) {
    JedisAsserts.isTrue(maxAttempts > 0, "maxAttempts must be greater than 0");
    JedisAsserts.notNull(maxTotalRetriesDuration, "maxTotalRetriesDuration must not be null");

    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
  }

  /**
   * Returns the maximum number of attempts for executing a command.
   * @return the maximum number of attempts (always &gt; 0)
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * Returns the maximum total duration allowed for all retry attempts.
   * @return the maximum total retries duration
   */
  public Duration getMaxTotalRetriesDuration() {
    return maxTotalRetriesDuration;
  }

  /**
   * Computes the remaining time in milliseconds until the given deadline.
   *
   * @param deadline the retry deadline
   * @return remaining milliseconds, or 0 if the deadline has already passed
   */
  protected static long millisUntil(Instant deadline) {
    long millis = Duration.between(Instant.now(), deadline).toMillis();
    return Math.max(millis, 0);
  }

  /**
   * Computes a jittered backoff duration based on the remaining attempts and time budget.
   * <p>
   * The maximum backoff is {@code millisLeft / (attemptsLeft²)}, which distributes the remaining
   * time budget across outstanding attempts. A random value in {@code [0, maxBackoff]} is returned
   * to add jitter, preventing retry storms when multiple clients fail simultaneously.
   *
   * @param attemptsLeft the number of attempts remaining (must be &gt; 0)
   * @param deadline the retry deadline
   * @return backoff duration in milliseconds ({@code 0} when no time is left or no attempts remain)
   */
  protected static long computeBackoffMillis(int attemptsLeft, Instant deadline) {
    if (attemptsLeft <= 0) {
      return 0;
    }

    long millisLeft = millisUntil(deadline);
    if (millisLeft <= 0) {
      return 0;
    }

    long maxBackoff = millisLeft / ((long) attemptsLeft * attemptsLeft);
    return ThreadLocalRandom.current().nextLong(maxBackoff + 1);
  }

  /**
   * Sleeps for the specified duration, preserving the thread's interrupt status.
   *
   * @param sleepMillis milliseconds to sleep; values &le; 0 are no-ops
   * @throws JedisException wrapping {@link InterruptedException} if the thread is interrupted
   */
  protected void sleep(long sleepMillis) {
    if (sleepMillis <= 0) {
      return;
    }
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JedisException(e);
    }
  }
}
