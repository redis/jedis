package redis.clients.jedis;

public interface CommandKeyArgumentPreProcessor {

  /**
   * @param paramKey key name in application
   * @return key name in Redis server
   */
  Object actualKey(Object paramKey);
}
