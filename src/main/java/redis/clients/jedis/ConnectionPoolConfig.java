package redis.clients.jedis;

public class ConnectionPoolConfig extends JedisPoolConfig {

  public ConnectionPoolConfig() {
    this(poolBbuilder());
  }

  private ConnectionPoolConfig(Builder builder) {
    super();
    setTestWhileIdle(builder.testWhileIdle);
    setMinEvictableIdleTimeMillis(builder.minEvictableIdleTimeMillis);
    setTimeBetweenEvictionRunsMillis(builder.timeBetweenEvictionRunsMillis);
    setNumTestsPerEvictionRun(builder.numTestsPerEvictionRun);
  }

  public static Builder poolBbuilder() {
    return new Builder();
  }

  public static class Builder {
    private boolean testWhileIdle                 = true;
    private long    minEvictableIdleTimeMillis    = 60000L;
    private long    timeBetweenEvictionRunsMillis = 30000L;
    private int     numTestsPerEvictionRun        = -1;

    public Builder testWhileIdle(boolean testWhileIdle) {
      this.testWhileIdle = testWhileIdle;
      return this;
    }

    public Builder minEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
      this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
      return this;
    }

    public Builder timeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
      this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
      return this;
    }

    public Builder numTestsPerEvictionRun(int numTestsPerEvictionRun) {
      this.numTestsPerEvictionRun = numTestsPerEvictionRun;
      return this;
    }

    public ConnectionPoolConfig build() {
      return new ConnectionPoolConfig(this);
    }
  }
}
