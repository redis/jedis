package redis.clients.jedis.util;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Delay {

  protected Delay() {
  }

  /**
   * Calculate a specific delay based on the attempt.
   * @param attempt the attempt to calculate the delay from.
   * @return the calculated delay.
   */
  public abstract Duration delay(long attempt);

  /**
   * Creates a constant delay.
   * @param delay the constant delay duration
   * @return a Delay that always returns the same duration
   */
  public static Delay constant(Duration delay) {
    return new ConstantDelay(delay);
  }

  /**
   * Creates an exponential delay with equal jitter. Based on AWS exponential backoff strategy:
   * https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/ Formula: temp =
   * min(upper, base * 2^attempt) sleep = temp/2 + random_between(0, temp/2) result = max(lower,
   * sleep)
   * @param lower the minimum delay duration (lower bound)
   * @param upper the maximum delay duration (upper bound)
   * @param base the base delay duration
   * @return a Delay with exponential backoff and equal jitter
   */
  public static Delay exponentialWithJitter(Duration lower, Duration upper, Duration base) {
    return new EqualJitterDelay(lower, upper, base);
  }

  static class ConstantDelay extends Delay {

    private final Duration delay;

    ConstantDelay(Duration delay) {
      this.delay = delay;
    }

    @Override
    public Duration delay(long attempt) {
      return delay;
    }
  }

  static class EqualJitterDelay extends Delay {

    private final long lowerMillis;
    private final long upperMillis;
    private final long baseMillis;

    EqualJitterDelay(Duration lower, Duration upper, Duration base) {
      this.lowerMillis = lower.toMillis();
      this.upperMillis = upper.toMillis();
      this.baseMillis = base.toMillis();
    }

    @Override
    public Duration delay(long attempt) {
      // temp = min(upper, base * 2^attempt)
      long exponential = baseMillis * (1L << Math.min(attempt, 62));
      long temp = Math.min(upperMillis, exponential);

      // sleep = temp/2 + random_between(0, temp/2)
      long half = temp / 2;
      long jitter = ThreadLocalRandom.current().nextLong(half + 1);
      long delayMillis = half + jitter;

      // Apply lower bound
      delayMillis = Math.max(lowerMillis, delayMillis);

      return Duration.ofMillis(delayMillis);
    }
  }
}
