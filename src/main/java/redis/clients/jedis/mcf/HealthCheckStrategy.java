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
   * Get the number of probes for health checks to repeat.
   * @return the number of probes
   */
  int getNumProbes();

  /**
   * Get the policy for health checks.
   * @return the policy
   */
  ProbingPolicy getPolicy();

  /**
   * Get the delay (in milliseconds) between retries for failed health checks.
   * @return the delay in milliseconds
   */
  int getDelayInBetweenProbes();

  public static class Config {
    protected final int interval;
    protected final int timeout;
    protected final int numProbes;
    protected final int delayInBetweenProbes;
    protected final ProbingPolicy policy;

    public Config(int interval, int timeout, int numProbes, int delayInBetweenProbes,
        ProbingPolicy policy) {
      this.interval = interval;
      this.timeout = timeout;
      this.numProbes = numProbes;
      this.delayInBetweenProbes = delayInBetweenProbes;
      this.policy = policy;
    }

    Config(Builder<?, ?> builder) {
      this.interval = builder.interval;
      this.timeout = builder.timeout;
      this.numProbes = builder.numProbes;
      this.delayInBetweenProbes = builder.delayInBetweenProbes;
      this.policy = builder.policy;
    }

    public int getInterval() {
      return interval;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getNumProbes() {
      return numProbes;
    }

    public int getDelayInBetweenProbes() {
      return delayInBetweenProbes;
    }

    public ProbingPolicy getPolicy() {
      return policy;
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
      protected int numProbes = 3;
      protected ProbingPolicy policy = ProbingPolicy.BuiltIn.ALL_SUCCESS;
      protected int delayInBetweenProbes = 100;

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
       * Set the number of probes for health check.
       * @param numProbes the number of repeats (default: 3)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T numProbes(int numProbes) {
        this.numProbes = numProbes;
        return (T) this;
      }

      /**
       * Set the policy for health checks.
       * @param policy the policy (default: ProbingPolicy.BuiltIn.ALL_SUCCESS)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T policy(ProbingPolicy policy) {
        this.policy = policy;
        return (T) this;
      }

      /**
       * Set the delay between retries for failed health checks in milliseconds.
       * @param delayInBetweenProbes the delay in milliseconds (default: 100)
       * @return this builder
       */
      @SuppressWarnings("unchecked")
      public T delayInBetweenProbes(int delayInBetweenProbes) {
        this.delayInBetweenProbes = delayInBetweenProbes;
        return (T) this;
      }

      /**
       * Build the Config instance.
       * @return a new Config instance
       */
      @SuppressWarnings("unchecked")
      public C build() {
        return (C) new Config(this);
      }
    }
  }
}
