package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Aggregate operators for {@code AROP}. Each constant maps directly to the corresponding Redis
 * subcommand keyword.
 */
public enum ArrayAggregate implements Rawable {

  /** Sum of the numeric values in the range. */
  SUM,
  /** Minimum numeric value in the range. */
  MIN,
  /** Maximum numeric value in the range. */
  MAX;

  private final byte[] raw;

  ArrayAggregate() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
