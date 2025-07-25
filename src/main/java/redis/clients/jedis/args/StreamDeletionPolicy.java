package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Deletion policy for stream commands that handle consumer group references. Used with XDELEX,
 * XACKDEL, and enhanced XADD/XTRIM commands.
 */
public enum StreamDeletionPolicy implements Rawable {

  /**
   * Preserves existing references to entries in all consumer groups' PEL. This is the default
   * behavior similar to XDEL.
   */
  KEEP_REFERENCES("KEEPREF"),

  /**
   * Removes all references to entries from all consumer groups' pending entry lists, effectively
   * cleaning up all traces of the messages.
   */
  DELETE_REFERENCES("DELREF"),

  /**
   * Only operates on entries that were read and acknowledged by all consumer groups.
   */
  ACKNOWLEDGED("ACKED");

  private final byte[] raw;

  StreamDeletionPolicy(String redisParamName) {
    raw = SafeEncoder.encode(redisParamName);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
