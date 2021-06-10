package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Enum object describing flushing mode.
 */
public enum FlushMode implements Rawable {

  /**
   * flushes synchronously
   */
  SYNC,

  /**
   * flushes asynchronously
   */
  ASYNC;

  private final byte[] raw;

  FlushMode() {
    raw = SafeEncoder.encode(this.name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
