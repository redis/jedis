package redis.clients.jedis;

public class JedisPoolConfig {
  private int     maxTotal                      = 8;
  private int     maxIdle                       = 8;
  private int     minIdle                       = 0;
  private long    maxWaitMillis                 = 5000L;
  private long    minEvictableIdleTimeMillis    = 60000L;
  private long    timeBetweenEvictionRunsMillis = 30000L;
  private int     numTestsPerEvictionRun        = 3;
  private boolean testOnBorrow                  = true;
  private boolean testOnReturn                  = false;
  private boolean testWhileIdle                 = true;

  public JedisPoolConfig() {
    this(builder());
  }

  private JedisPoolConfig(Builder builder) {
    this.maxTotal                      = builder.maxTotal;
    this.maxIdle                       = builder.maxIdle;
    this.minIdle                       = builder.minIdle;
    this.maxWaitMillis                 = builder.maxWaitMillis;
    this.minEvictableIdleTimeMillis    = builder.minEvictableIdleTimeMillis;
    this.timeBetweenEvictionRunsMillis = builder.timeBetweenEvictionRunsMillis;
    this.numTestsPerEvictionRun        = builder.numTestsPerEvictionRun;
    this.testOnBorrow                  = builder.testOnBorrow;
    this.testOnReturn                  = builder.testOnReturn;
    this.testWhileIdle                 = builder.testWhileIdle;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getMaxTotal() {
    return maxTotal;
  }

  public void setMaxTotal(int maxTotal) {
    this.maxTotal = maxTotal;
  }

  public int getMaxIdle() {
    return maxIdle;
  }

  public void setMaxIdle(int maxIdle) {
    this.maxIdle = maxIdle;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public void setMinIdle(int minIdle) {
    this.minIdle = minIdle;
  }

  public long getMaxWaitMillis() {
    return maxWaitMillis;
  }

  public void setMaxWaitMillis(long maxWaitMillis) {
    this.maxWaitMillis = maxWaitMillis;
  }

  public long getMinEvictableIdleTimeMillis() {
    return minEvictableIdleTimeMillis;
  }

  public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
    this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
  }

  public long getTimeBetweenEvictionRunsMillis() {
    return timeBetweenEvictionRunsMillis;
  }

  public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
    this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
  }

  public int getNumTestsPerEvictionRun() {
    return numTestsPerEvictionRun;
  }

  public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
    this.numTestsPerEvictionRun = numTestsPerEvictionRun;
  }

  public boolean isTestOnBorrow() {
    return testOnBorrow;
  }

  public void setTestOnBorrow(boolean testOnBorrow) {
    this.testOnBorrow = testOnBorrow;
  }

  public boolean isTestOnReturn() {
    return testOnReturn;
  }

  public void setTestOnReturn(boolean testOnReturn) {
    this.testOnReturn = testOnReturn;
  }

  public boolean isTestWhileIdle() {
    return testWhileIdle;
  }

  public void setTestWhileIdle(boolean testWhileIdle) {
    this.testWhileIdle = testWhileIdle;
  }

  public static class Builder {
    private int     maxTotal                      = 8;
    private int     maxIdle                       = 8;
    private int     minIdle                       = 0;
    private long    maxWaitMillis                 = 5000L;
    private long    minEvictableIdleTimeMillis    = 60000L;
    private long    timeBetweenEvictionRunsMillis = 30000L;
    private int     numTestsPerEvictionRun        = 3;
    private boolean testOnBorrow                  = true;
    private boolean testOnReturn                  = false;
    private boolean testWhileIdle                 = true;

    public Builder maxTotal(int maxTotal) {
      this.maxTotal = maxTotal;
      return this;
    }

    public Builder maxIdle(int maxIdle) {
      this.maxIdle = maxIdle;
      return this;
    }

    public Builder minIdle(int minIdle) {
      this.minIdle = minIdle;
      return this;
    }

    public Builder maxWaitMillis(long maxWaitMillis) {
      this.maxWaitMillis = maxWaitMillis;
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

    public Builder testOnBorrow(boolean testOnBorrow) {
      this.testOnBorrow = testOnBorrow;
      return this;
    }

    public Builder testOnReturn(boolean testOnReturn) {
      this.testOnReturn = testOnReturn;
      return this;
    }

    public Builder testWhileIdle(boolean testWhileIdle) {
      this.testWhileIdle = testWhileIdle;
      return this;
    }

    public JedisPoolConfig build() {
      return new JedisPoolConfig(this);
    }
  }
}
