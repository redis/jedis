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
   * Set the unit of start and end: {@link BitCountOption#BYTE BYTE} (the server default) or
   * {@link BitCountOption#BIT BIT}. The returned offsets are bit offsets in both units.
   * @param unit the unit, or {@code null} to send none
   * @return BitRangeIntArrayParams
   * @since 8.1
   */
  public BitRangeIntArrayParams unit(BitCountOption unit) {
    this.unit = unit;
    return this;
  }

  /**
   * Return the lowest offsets of the range: at most {@code count} in {@link BitCountOption#BIT BIT}
   * units, and up to {@code count + 7} in {@link BitCountOption#BYTE BYTE} units, the default.
   * <p>
   * Paging: in {@link BitCountOption#BIT BIT} units a page holds at most {@code count} offsets, and
   * the next page starts at the last returned offset + 1. In {@link BitCountOption#BYTE BYTE} units
   * the server completes the byte of the last offset, so a page can hold up to {@code count + 7}
   * offsets, and the next page starts at byte (last returned offset / 8) + 1. An empty page, or one
   * with fewer than {@code count} offsets, ends the scan. A count of 0 always returns an empty list
   * and can't be used to page.
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
   * Return the offsets of the bits set to 0 instead of 1. Only the bits inside the value are
   * reported: no clear bits are assumed past its end.
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
