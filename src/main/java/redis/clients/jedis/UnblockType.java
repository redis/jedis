package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Unblock type for {@code CLIENT UNBLOCK} command.
 */
public enum UnblockType {
  TIMEOUT, ERROR;

  public final byte[] raw;

  UnblockType() {
    raw = SafeEncoder.encode(this.name().toLowerCase());
  }
}
