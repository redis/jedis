package redis.clients.jedis.csc.hash;

import redis.clients.jedis.CommandObject;

/**
 * The interface for hashing a command object for client-side caching.
 */
public interface CommandLongHashing {

  /**
   * Produce a 64-bit signed hash from a command object.
   * @param command the command object
   * @return 64-bit signed hash
   */
  long hash(CommandObject command);
}
