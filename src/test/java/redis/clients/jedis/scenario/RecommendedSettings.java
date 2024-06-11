package redis.clients.jedis.scenario;

import redis.clients.jedis.ConnectionPoolConfig;

import java.time.Duration;

public class RecommendedSettings {

  public static ConnectionPoolConfig poolConfig;

  static {
    poolConfig = new ConnectionPoolConfig();
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(8);
    poolConfig.setMaxIdle(8);
    poolConfig.setMinIdle(0);
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setMaxWait(Duration.ofSeconds(1));
    poolConfig.setTestWhileIdle(true);
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));
  }

  public static int MAX_RETRIES = 5;

  public static Duration MAX_TOTAL_RETRIES_DURATION = Duration.ofSeconds(10);

  public static int DEFAULT_TIMEOUT_MS = 5000;



}
