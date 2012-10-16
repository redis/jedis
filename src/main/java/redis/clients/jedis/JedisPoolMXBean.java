package redis.clients.jedis;

public interface JedisPoolMXBean {

    public enum WhenExhaustedAction {FAIL, BLOCK, GROW};

    // Attributes
    String getHost();
    int getPort();
    int getTimeout();
    int getNumActive();
    int getNumIdle();
    
    boolean getLifo();
    int getMaxActive();
    int getMaxIdle();
    long getMaxWait();
    long getMinEvictableIdleTimeMillis();
    int getMinIdle();
    int getNumTestsPerEvictionRun();
    long getSoftMinEvictableIdleTimeMillis();
    boolean getTestOnBorrow();
    boolean getTestOnReturn();
    boolean getTestWhileIdle();
    long getTimeBetweenEvictionRunsMillis();
    WhenExhaustedAction getWhenExhaustedAction();

    // Operations
    void updateHostAndPort(String host, int port);
    void setMaxActive(int maxActive);
    void setMaxIdle(int maxIdle);
    void setMaxWait(long maxWait);
    void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis);
    void setMinIdle(int minIdle);
    void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);
    void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis);
    void setTestOnBorrow(boolean testOnBorrow);
    void setTestOnReturn(boolean testOnReturn);
    void setTestWhileIdle(boolean testWhileIdle);
    void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);
    void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction);
}
