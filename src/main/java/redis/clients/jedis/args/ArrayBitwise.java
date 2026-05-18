package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Bitwise operators for {@code AROP}. Each constant maps directly to the corresponding Redis
 * subcommand keyword.
 */
public enum ArrayBitwise implements Rawable {

  /** Bitwise AND across all non-empty elements in the range. */
  AND,
  /** Bitwise OR across all non-empty elements in the range. */
  OR,
  /** Bitwise XOR across all non-empty elements in the range. */
  XOR;

  private final byte[] raw;

  ArrayBitwise() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
