package redis.clients.jedis;

import java.util.function.Function;

public interface Retryer {
  /**
   * Execute a Redis command with retries.
   *
   * @param slot From one of the {@code JedisClusterCRC16#get*())} methods
   * @return The result of running the given command with retries
   */
  <R> R runWithRetries(int slot, Function<Jedis, R> command);
}
