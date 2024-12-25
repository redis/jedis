package redis.clients.jedis;

import today.bonfire.oss.sop.SimpleObjectPoolConfig;

import java.time.Duration;

/**
 * Configuration class for Jedis connection pool that extends SimpleObjectPoolConfig. Provides
 * Jedis-specific default values and configuration options while leveraging the base pool
 * configuration capabilities.
 */
public class JedisPoolConfig extends SimpleObjectPoolConfig {

  public JedisPoolConfig() {
    super(builder().defaultConfig().prepareAndCheck());
  }

  public JedisPoolConfig(SimpleObjectPoolConfig.Builder config) {
    super(config);
  }

  /**
   * Creates a new builder instance with Jedis-specific default settings.
   * @return A new Builder instance with Jedis-specific default settings
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for JedisPoolConfig that extends SimpleObjectPoolConfig.Builder to provide
   * Jedis-specific configuration options.
   */
  public static class Builder extends SimpleObjectPoolConfig.Builder {

    /**
     * Sets default Jedis pool configuration values.
     * @return this Builder instance
     */
    public Builder defaultConfig() {
      this.maxPoolSize(8).minPoolSize(0).fairness(false).testOnCreate(false).testOnBorrow(true)
          .testWhileIdle(true).testOnReturn(false)
          .durationBetweenEvictionsRuns(Duration.ofSeconds(30))
          .objEvictionTimeout(Duration.ofSeconds(60));
      return this;
    }

    @Override
    protected Builder prepareAndCheck() {
      super.prepareAndCheck();
      return this;
    }

    @Override
    public JedisPoolConfig build() {
      var config = super.prepareAndCheck();
      return new JedisPoolConfig(config);
    }
  }
}
