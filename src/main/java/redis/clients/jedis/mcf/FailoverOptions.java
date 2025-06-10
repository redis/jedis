package redis.clients.jedis.mcf;

import redis.clients.jedis.annots.Experimental;

/**
 * Configuration options for CircuitBreakerCommandExecutor
 */
@Experimental
public class FailoverOptions {
  private final boolean retryFailedInflightCommands;

  private FailoverOptions(Builder builder) {
    this.retryFailedInflightCommands = builder.retryFailedInflightCommands;
  }

  /**
   * Gets whether to retry failed in-flight commands during failover
   * @return true if retry is enabled, false otherwise
   */
  public boolean isRetryFailedInflightCommands() {
    return retryFailedInflightCommands;
  }

  /**
   * Creates a new builder with default options
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for FailoverOptions
   */
  public static class Builder {
    private boolean retryFailedInflightCommands = false;

    private Builder() {
    }

    /**
     * Sets whether to retry failed in-flight commands during failover
     * @param retry true to retry, false otherwise
     * @return this builder for method chaining
     */
    public Builder retryFailedInflightCommands(boolean retry) {
      this.retryFailedInflightCommands = retry;
      return this;
    }

    /**
     * Builds a new FailoverOptions instance with the configured options
     * @return a new FailoverOptions instance
     */
    public FailoverOptions build() {
      return new FailoverOptions(this);
    }
  }
}