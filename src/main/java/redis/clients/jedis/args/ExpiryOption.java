package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Enumeration of setting expiration.
 */
public enum ExpiryOption implements Rawable {

  /**
   * Set expiry only when the key has no expiry.
   */
  NX,

  /**
   * Set expiry only when the key has an existing expiry.
   */
  XX,

  /**
   * Set expiry only when the new expiry is greater than the existing one.
   */
  GT,

  /**
   * Set expiry only when the new expiry is less than the existing one.
   */
  LT;

  private final byte[] raw;

  private ExpiryOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
