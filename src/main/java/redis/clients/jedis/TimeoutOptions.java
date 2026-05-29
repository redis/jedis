package redis.clients.jedis;

import redis.clients.jedis.util.JedisAsserts;

import java.time.Duration;

public class TimeoutOptions {

  public static final int UNSET_TIMEOUT_MS = -1;

  public static final Duration UNSET_TIMEOUT = Duration.ofMillis(UNSET_TIMEOUT_MS);

  public static final Duration DEFAULT_RELAXED_TIMEOUT = Duration.ofSeconds(10);

  public static final Duration DEFAULT_RELAXED_BLOCKING_TIMEOUT = UNSET_TIMEOUT;

  private final Duration relaxedTimeout;

  private final Duration relaxedBlockingTimeout;

  private TimeoutOptions(Duration relaxedTimeout, Duration relaxedBlockingTimeout) {
    this.relaxedTimeout = relaxedTimeout;
    this.relaxedBlockingTimeout = relaxedBlockingTimeout;
  }

  /**
   * Returns whether an explicit relaxed timeout value was configured, as opposed to being unset (in
   * which case the base socket timeout is inherited).
   * @param relaxedTimeoutMs the relaxed timeout in milliseconds
   * @return {@code true} if a value was explicitly configured, {@code false} if unset
   */
  public static boolean isSet(int relaxedTimeoutMs) {
    return relaxedTimeoutMs != UNSET_TIMEOUT_MS;
  }

  /**
   * @return the {@link Duration} to relax timeouts proactively, {@link #UNSET_TIMEOUT} if not set.
   */
  public Duration getRelaxedTimeout() {
    return relaxedTimeout;
  }

  /**
   * @return the {@link Duration} to relax timeouts proactively for blocking commands,
   *         {@link #UNSET_TIMEOUT} if not set.
   */
  public Duration getRelaxedBlockingTimeout() {
    return relaxedBlockingTimeout;
  }

  /**
   * Returns a new {@link TimeoutOptions.Builder} to construct {@link TimeoutOptions}.
   * @return a new {@link TimeoutOptions.Builder} to construct {@link TimeoutOptions}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Create a new instance of {@link TimeoutOptions} with default settings.
   * @return a new instance of {@link TimeoutOptions} with default settings.
   */
  public static TimeoutOptions create() {
    return builder().build();
  }

  public static class Builder {
    private Duration relaxedTimeout = DEFAULT_RELAXED_TIMEOUT;
    private Duration relaxedBlockingTimeout = DEFAULT_RELAXED_BLOCKING_TIMEOUT;

    /**
     * Enable proactive timeout relaxing. Disabled by default, see {@link #DEFAULT_RELAXED_TIMEOUT}.
     * <p>
     * If the Redis server supports this, and the client is set up to use it , the client would
     * listen to notifications that the current endpoint is about to go down (as part of some
     * maintenance activity, for example). In such cases, the driver could extend the existing
     * timeout settings for newly issued commands, or such that are in flight, to make sure they do
     * not time out during this process.
     * </p>
     * @param duration {@link Duration} to relax timeouts proactively, must not be {@code null}.
     * @return {@code this}
     */
    public Builder proactiveTimeoutsRelaxing(Duration duration) {
      JedisAsserts.notNull(duration, "Duration must not be null");

      this.relaxedTimeout = duration;
      return this;
    }

    /**
     * Enable proactive timeout relaxing for blocking commands. Disabled by default, see
     * {@link #DEFAULT_RELAXED_BLOCKING_TIMEOUT}.
     * <p>
     * If the Redis server supports this, and the client is set up to use it, the client would
     * listen to notifications that the current endpoint is about to go down (as part of some
     * maintenance activity, for example). In such cases, the driver could extend the existing
     * timeout settings for blocking commands that are in flight, to make sure they do not time out
     * during this process. If not configured, the infinite timeout for blocking commands will be
     * preserved.
     * </p>
     * @param duration {@link Duration} to relax timeouts proactively for blocking commands, must
     *          not be {@code null}.
     * @return {@code this}
     */
    public Builder proactiveBlockingTimeoutsRelaxing(Duration duration) {
      JedisAsserts.notNull(duration, "Duration must not be null");

      this.relaxedBlockingTimeout = duration;
      return this;
    }

    public TimeoutOptions build() {
      return new TimeoutOptions(relaxedTimeout, relaxedBlockingTimeout);
    }
  }

}
