package redis.clients.jedis;

public interface JedisBroadcastAndRoundRobinConfig {

  public enum RediSearchMode {
    DEFAULT, LIGHT;
  }

  RediSearchMode getRediSearchModeInCluster();
}
