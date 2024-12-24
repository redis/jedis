package redis.clients.jedis;

import today.bonfire.oss.sop.SimpleObjectPoolConfig;

import java.time.Duration;

/**
 * Configuration class for Connection pool that extends SimpleObjectPoolConfig.
 * Provides pool-specific default values and configuration options while leveraging
 * the base pool configuration capabilities.
 */
public class ConnectionPoolConfig extends SimpleObjectPoolConfig {

  public ConnectionPoolConfig() {
    super(builder().defaultConfig().prepareAndCheck());
  }

  public ConnectionPoolConfig(SimpleObjectPoolConfig.Builder config) {
    super(config);
  }

  /**
   * Creates a new builder instance with pool-specific default settings.
   *
   * @return A new Builder instance with pool-specific default settings
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for ConnectionPoolConfig that extends SimpleObjectPoolConfig.Builder
   * to provide pool-specific configuration options.
   */
  public static class Builder extends SimpleObjectPoolConfig.Builder {

    /**
     * Sets default pool configuration values.
     *
     * @return this Builder instance
     */
    public Builder defaultConfig() {
      this.maxPoolSize(8)
          .minPoolSize(0)
          .fairness(false)
          .testOnCreate(false)
          .testOnBorrow(true)
          .testWhileIdle(true)
          .testOnReturn(false)
          .durationBetweenEvictionsRuns(Duration.ofSeconds(30))
          .objEvictionTimeout(Duration.ofSeconds(60));
      return this;
    }

    @Override protected Builder prepareAndCheck() {
      super.prepareAndCheck();
      return this;
    }

    @Override
    public ConnectionPoolConfig build() {
      var config = super.prepareAndCheck();
      return new ConnectionPoolConfig(config);
    }
  }
}
