package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum FunctionRestorePolicy implements Rawable {
  /**
   * Delete all existing libraries before restoring the payload
   */
  FLUSH,

  /**
   * Append the restored libraries to the existing libraries and
   * aborts on collision. This is the default policy.
   */
  APPEND,

  /**
   * Append the restored libraries to the existing libraries, replacing
   * any existing ones in case of name collisions. Note that this policy
   * doesn't prevent function name collisions, only libraries.
   */
  REPLACE;

  private final byte[] raw;

  private FunctionRestorePolicy() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
