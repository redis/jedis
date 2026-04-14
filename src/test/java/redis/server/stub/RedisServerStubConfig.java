package redis.server.stub;

/**
 * Configuration for RedisServerStub.
 * <p>
 * Allows customization of server behavior for testing different scenarios.
 * </p>
 */
public class RedisServerStubConfig {

  private final String redisVersion;

  private RedisServerStubConfig(Builder builder) {
    this.redisVersion = builder.redisVersion;
  }

  public String getRedisVersion() {
    return redisVersion;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String redisVersion = "7.4.0";

    public Builder redisVersion(String version) {
      this.redisVersion = version;
      return this;
    }

    public RedisServerStubConfig build() {
      return new RedisServerStubConfig(this);
    }
  }
}

