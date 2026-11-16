package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.BitCountOption;

/**
 * Parameters for the {@code BITRANGEINTARRAY} command.
 * <p>
 * The options are sent in a fixed order, each at most once: the unit ({@code BYTE} or {@code BIT}),
 * then {@code LIMIT count}, then {@code ZEROS}. Options that are not set are not sent.
 * <p>
 * Usage examples:
 *
 * <pre>
 * BitRangeIntArrayParams.bitRangeIntArrayParams().unit(BitCountOption.BIT).limit(100)
 * BitRangeIntArrayParams.bitRangeIntArrayParams().zeros()
 * </pre>
 *
 * @since 8.1
 */
public class BitRangeIntArrayParams implements IParams {

  private BitCountOption unit;
  private Long limit;
  private boolean zeros;

  /**
   * @return a new, empty BitRangeIntArrayParams
   * @since 8.1
   */
  public static BitRangeIntArrayParams bitRangeIntArrayParams() {
    return new BitRangeIntArrayParams();
  }

  /**
   * The unit for start and end: {@link BitCountOption#BYTE BYTE} (the default) or
   * {@link BitCountOption#BIT BIT}.
   * @param unit the unit, or {@code null} to send none
   * @return BitRangeIntArrayParams
   * @since 8.1
   */
  public BitRangeIntArrayParams unit(BitCountOption unit) {
    this.unit = unit;
    return this;
  }

  /**
   * Return the lowest matching offsets in the range. With {@link BitCountOption#BIT BIT}, the reply
   * contains at most {@code count} offsets. With {@link BitCountOption#BYTE BYTE}, the command also
   * returns the remaining matching offsets in the byte containing the {@code count}th match, so the
   * reply can contain up to {@code count + 7} offsets. A count of 0 returns an empty list. Without
   * a limit, every matching offset in the range is returned.
   * @param count the number of offsets to return, not negative
   * @return BitRangeIntArrayParams
   * @throws IllegalArgumentException if {@code count} is negative
   * @since 8.1
   */
  public BitRangeIntArrayParams limit(long count) {
    if (count < 0) {
      throw new IllegalArgumentException("LIMIT must be non-negative");
    }
    this.limit = count;
    return this;
  }

  /**
   * Return clear-bit offsets instead of set-bit offsets within the range.
   * @return BitRangeIntArrayParams
   * @since 8.1
   */
  public BitRangeIntArrayParams zeros() {
    this.zeros = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (unit != null) {
      args.add(unit);
    }
    if (limit != null) {
      args.add(Keyword.LIMIT).add(limit);
    }
    if (zeros) {
      args.add(Keyword.ZEROS);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BitRangeIntArrayParams that = (BitRangeIntArrayParams) o;
    return zeros == that.zeros && unit == that.unit && Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(unit, limit, zeros);
  }
}
