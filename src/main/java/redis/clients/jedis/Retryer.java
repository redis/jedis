package redis.clients.jedis;

import java.util.function.Function;

/**
 * Functionality for retrying Redis connections.
 * <p/>
 * This class contains scaffolding around the retry mechanism. The actual retrying will be done in
 * subclasses of this class, with {@link DefaultRetryer} being the default implementation.
 *
 * @see BinaryJedisCluster#BinaryJedisCluster(JedisClusterConnectionHandler, Retryer)
 * @see JedisCluster#JedisCluster(JedisClusterConnectionHandler, Retryer)
 * @see DefaultRetryer
 */
public interface Retryer {

  /**
   * Execute a Redis command on any random node.
   */
  <R> R runWithRetries(JedisClusterConnectionHandler connectionHandler, Function<Jedis, R> command);

  /**
   * Execute a Redis command with retries.
   *
   * @param slot From one of the {@code JedisClusterCRC16#get*())} methods
   * @return The result of running the given command with retries
   */
  <R> R runWithRetries(JedisClusterConnectionHandler connectionHandler, int slot,
      Function<Jedis, R> command);
}
