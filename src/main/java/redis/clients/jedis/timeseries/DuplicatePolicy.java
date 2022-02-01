package redis.clients.jedis.timeseries;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Policy that will define handling of duplicate samples.
 */
public enum DuplicatePolicy implements Rawable {

  /**
   * An error will occur for any out of order sample
   */
  BLOCK,
  /**
   * Ignore the new value
   */
  FIRST,
  /**
   * Override with latest value
   */
  LAST,
  /**
   * Only override if the value is lower than the existing value
   */
  MIN,
  /**
   * Only override if the value is higher than the existing value
   */
  MAX,
  /**
   * If a previous sample exists, add the new sample to it so that the updated value is
   */
  SUM;

  private final byte[] raw;

  private DuplicatePolicy() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
