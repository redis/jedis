package redis.clients.jedis.csc.hash;

import redis.clients.jedis.CommandObject;

/**
 * The interface for hashing a command object to support client-side caching.
 */
public interface CommandLongHasher {

  /**
   * Produce a 64-bit signed hash value from a command object.
   * @param command the command object
   * @return 64-bit signed hash value
   */
  long hash(CommandObject command);
}
