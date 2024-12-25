package redis.clients.jedis.scenario;

import redis.clients.jedis.ConnectionPoolConfig;

import java.time.Duration;

public class RecommendedSettings {

  public static ConnectionPoolConfig poolConfig;

  static {
    poolConfig = new ConnectionPoolConfig();
    var poolConfig = ConnectionPoolConfig.builder();
    poolConfig.maxPoolSize(8);
    poolConfig.minPoolSize(0);
    // poolConfig.setBlockWhenExhausted(true);
    poolConfig.waitingForObjectTimeout(Duration.ofSeconds(1));
    poolConfig.testWhileIdle(true);
    poolConfig.objEvictionTimeout(Duration.ofSeconds(60));
    poolConfig.durationBetweenEvictionsRuns(Duration.ofSeconds(1));
    // TODO
  }

  public static int MAX_RETRIES = 5;

  public static Duration MAX_TOTAL_RETRIES_DURATION = Duration.ofSeconds(10);

  public static int DEFAULT_TIMEOUT_MS = 5000;

}
