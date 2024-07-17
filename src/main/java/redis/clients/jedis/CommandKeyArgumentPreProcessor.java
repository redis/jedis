package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

@Experimental
public interface CommandKeyArgumentPreProcessor {

  /**
   * @param paramKey key name in application
   * @return key name in Redis server
   */
  Object actualKey(Object paramKey);
}
