package redis.clients.jedis.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class DelayTest {

  @Test
  public void testConstantDelay() {
    Delay delay = Delay.constant(Duration.ofMillis(100));

    // Constant delay should return the same value for all attempts
    assertEquals(100, delay.delay(0).toMillis());
    assertEquals(100, delay.delay(1).toMillis());
    assertEquals(100, delay.delay(5).toMillis());
    assertEquals(100, delay.delay(100).toMillis());
  }

  @Test
  public void testExponentialWithJitterBounds() {
    Duration lower = Duration.ofMillis(50);
    Duration upper = Duration.ofSeconds(10);
    Duration base = Duration.ofMillis(100);

    Delay delay = Delay.exponentialWithJitter(lower, upper, base);

    // Test multiple attempts to verify bounds
    for (int attempt = 0; attempt < 20; attempt++) {
      Duration result = delay.delay(attempt);
      long millis = result.toMillis();

      // Verify lower bound
      assertTrue(millis >= lower.toMillis(), String.format(
        "Attempt %d: delay %d should be >= lower bound %d", attempt, millis, lower.toMillis()));

      // Verify upper bound
      assertTrue(millis <= upper.toMillis(), String.format(
        "Attempt %d: delay %d should be <= upper bound %d", attempt, millis, upper.toMillis()));
    }
  }

  @Test
  public void testExponentialWithJitterGrowth() {
    Duration lower = Duration.ofMillis(10);
    Duration upper = Duration.ofSeconds(60);
    Duration base = Duration.ofMillis(100);

    Delay delay = Delay.exponentialWithJitter(lower, upper, base);

    // Collect multiple samples for each attempt to verify growth trend
    int samples = 100;

    // Attempt 0: base * 2^0 = 100ms, range [50-100]ms
    long sum0 = 0;
    for (int i = 0; i < samples; i++) {
      sum0 += delay.delay(0).toMillis();
    }
    long avg0 = sum0 / samples;

    // Attempt 1: base * 2^1 = 200ms, range [100-200]ms
    long sum1 = 0;
    for (int i = 0; i < samples; i++) {
      sum1 += delay.delay(1).toMillis();
    }
    long avg1 = sum1 / samples;

    // Attempt 2: base * 2^2 = 400ms, range [200-400]ms
    long sum2 = 0;
    for (int i = 0; i < samples; i++) {
      sum2 += delay.delay(2).toMillis();
    }
    long avg2 = sum2 / samples;

    // Verify exponential growth: avg1 should be roughly 2x avg0, avg2 should be roughly 2x avg1
    assertTrue(avg1 > avg0, "Average delay should increase with attempts");
    assertTrue(avg2 > avg1, "Average delay should continue to increase");
  }

  @Test
  public void testExponentialWithJitterEqualJitterFormula() {
    Duration lower = Duration.ofMillis(0);
    Duration upper = Duration.ofSeconds(10);
    Duration base = Duration.ofMillis(100);

    Delay delay = Delay.exponentialWithJitter(lower, upper, base);

    // For attempt 0: temp = min(10000, 100 * 2^0) = 100
    // Equal jitter: delay = temp/2 + random[0, temp/2] = 50 + random[0, 50]
    // Range should be [50, 100]
    for (int i = 0; i < 50; i++) {
      long millis = delay.delay(0).toMillis();
      assertTrue(millis >= 50 && millis <= 100,
        String.format("Attempt 0: delay %d should be in range [50, 100]", millis));
    }

    // For attempt 1: temp = min(10000, 100 * 2^1) = 200
    // Equal jitter: delay = 100 + random[0, 100]
    // Range should be [100, 200]
    for (int i = 0; i < 50; i++) {
      long millis = delay.delay(1).toMillis();
      assertTrue(millis >= 100 && millis <= 200,
        String.format("Attempt 1: delay %d should be in range [100, 200]", millis));
    }
  }

  @Test
  public void testExponentialWithJitterUpperBoundCapping() {
    Duration lower = Duration.ofMillis(10);
    Duration upper = Duration.ofMillis(500);
    Duration base = Duration.ofMillis(100);

    Delay delay = Delay.exponentialWithJitter(lower, upper, base);

    // For high attempts, exponential should be capped at upper bound
    // Attempt 10: base * 2^10 = 102400ms, but capped at 500ms
    // Equal jitter: delay = 250 + random[0, 250]
    // Range should be [250, 500]
    for (int i = 0; i < 50; i++) {
      long millis = delay.delay(10).toMillis();
      assertTrue(millis >= 250 && millis <= 500,
        String.format("Attempt 10: delay %d should be in range [250, 500] (capped)", millis));
    }
  }

  @Test
  public void testExponentialWithJitterLowerBoundEnforcement() {
    Duration lower = Duration.ofMillis(200);
    Duration upper = Duration.ofSeconds(10);
    Duration base = Duration.ofMillis(100);

    Delay delay = Delay.exponentialWithJitter(lower, upper, base);

    // For attempt 0: temp = 100, equal jitter would give [50, 100]
    // But lower bound is 200, so all values should be >= 200
    for (int i = 0; i < 50; i++) {
      long millis = delay.delay(0).toMillis();
      assertTrue(millis >= 200,
        String.format("Attempt 0: delay %d should be >= lower bound 200", millis));
    }
  }
}
