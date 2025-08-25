package redis.clients.jedis.mcf;

import java.io.Closeable;

import redis.clients.jedis.Endpoint;

public interface HealthCheckStrategy extends Closeable {

  /**
   * Get the interval (in milliseconds) between health checks.
   * @return the interval in milliseconds
   */
  int getInterval();

  /**
   * Get the timeout (in milliseconds) for a health check.
   * @return the timeout in milliseconds
   */
  int getTimeout();

  /**
   * Perform the health check for the given endpoint.
   * @param endpoint the endpoint to check
   * @return the health status
   */
  HealthStatus doHealthCheck(Endpoint endpoint);

  /**
   * Close any resources used by the health check strategy.
   */
  default void close() {
  }

  /**
   * Get the minimum number of consecutive successful health checks required to mark the endpoint as
   * healthy.
   * @return the minimum number of consecutive successful health checks
   */
  default int minConsecutiveSuccessCount() {
    return 1;
  }

  public static class Config {
    protected final int interval;
    protected final int timeout;
    protected final int minConsecutiveSuccessCount;

    public Config(int interval, int timeout, int minConsecutiveSuccessCount) {
      this.interval = interval;
      this.timeout = timeout;
      this.minConsecutiveSuccessCount = minConsecutiveSuccessCount;
    }

    public int getInterval() {
      return interval;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getMinConsecutiveSuccessCount() {
      return minConsecutiveSuccessCount;
    }

    /**
     * Create a new Config instance with default values.
     * @return a new Config instance
     */
    public static Config create() {
      return new Builder<>().build();
    }

    /**
     * Create a new builder for HealthCheckStrategy.Config.
     * @return a new Builder instance
     */
    public static Builder<?, Config> builder() {
      return new Builder<>();
    }

    /**
     * Base builder for HealthCheckStrategy.Config and its subclasses.
     * @param <T> the builder type (for fluent API)
     * @param <C> the config type being built
     */
    public static class Builder<T extends Builder<T, C>, C extends Config> {
      protected int interval = 1000;
      protected int timeout = 1000;
      protected int minConsecutiveSuccessCount = 3;

      /**
       * Set the interval between health checks in milliseconds.
       * @param interval the interval in milliseconds (default: 1000)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T interval(int interval) {
        this.interval = interval;
        return (T) this;
      }

      /**
       * Set the timeout for health checks in milliseconds.
       * @param timeout the timeout in milliseconds (default: 1000)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T timeout(int timeout) {
        this.timeout = timeout;
        return (T) this;
      }

      /**
       * Set the minimum number of consecutive successful health checks required.
       * @param minConsecutiveSuccessCount the minimum count (default: 3)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T minConsecutiveSuccessCount(int minConsecutiveSuccessCount) {
        this.minConsecutiveSuccessCount = minConsecutiveSuccessCount;
        return (T) this;
      }

      /**
       * Build the Config instance.
       * @return a new Config instance
       */
      public Config build() {
        return new Config(interval, timeout, minConsecutiveSuccessCount);
      }
    }
  }
}
