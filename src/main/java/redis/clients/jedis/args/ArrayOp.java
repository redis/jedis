package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Aggregate operations for the {@code AROP} command.
 * <p>
 * Each constant corresponds to a Redis aggregate operator that {@code AROP}
 * applies over a range of array elements. {@link #MATCH} is the only operator
 * that takes an additional value argument; all other operators are used
 * without trailing arguments.
 */
public enum ArrayOp implements Rawable {

  /** Sum of the numeric values. */
  SUM,
  /** Minimum numeric value. */
  MIN,
  /** Maximum numeric value. */
  MAX,
  /** Bitwise AND of all values. */
  AND,
  /** Bitwise OR of all values. */
  OR,
  /** Bitwise XOR of all values. */
  XOR,
  /** Count of elements whose value equals a supplied value. */
  MATCH,
  /** Count of non-empty elements in the range. */
  USED;

  private final byte[] raw;

  private ArrayOp() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
