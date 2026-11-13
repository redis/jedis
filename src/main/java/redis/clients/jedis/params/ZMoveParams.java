package redis.clients.jedis.params;

import static redis.clients.jedis.args.RawableFactory.from;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.SortedSetOption;

/**
 * Arguments for the {@code ZMOVE} command: which members to take from the source sorted set and,
 * optionally, how many.
 * <p>
 * An instance is created by one of the static factories, each carrying the mandatory selection:
 * {@link #min()}, {@link #max()} or {@link #byScore(String, String)} (with {@code double} and
 * {@code byte[]} overloads). The wire arguments are emitted in the order
 * {@code <MIN | MAX | BYSCORE min max> [COUNT count]}; {@code COUNT} is sent only when
 * {@link #count(long)} was called.
 * @since 8.1
 */
public class ZMoveParams implements IParams {

  private final SortedSetOption end;
  private final Rawable min;
  private final Rawable max;
  private Long count;

  private ZMoveParams(SortedSetOption end, Rawable min, Rawable max) {
    this.end = end;
    this.min = min;
    this.max = max;
  }

  /**
   * Take the members with the lowest scores.
   * @return ZMoveParams
   */
  public static ZMoveParams min() {
    return new ZMoveParams(SortedSetOption.MIN, null, null);
  }

  /**
   * Take the members with the highest scores.
   * @return ZMoveParams
   */
  public static ZMoveParams max() {
    return new ZMoveParams(SortedSetOption.MAX, null, null);
  }

  /**
   * Take the members whose score is between {@code min} and {@code max}, both inclusive. Infinities
   * are sent as {@code -inf} and {@code +inf}.
   * @param min the lowest score
   * @param max the highest score
   * @return ZMoveParams
   */
  public static ZMoveParams byScore(double min, double max) {
    return new ZMoveParams(null, from(min), from(max));
  }

  /**
   * Take the members whose score is between {@code min} and {@code max}. The bounds are sent as
   * given, so they accept the server's syntax: {@code (} for an exclusive bound, {@code -inf} and
   * {@code +inf}.
   * @param min the lowest score
   * @param max the highest score
   * @return ZMoveParams
   */
  public static ZMoveParams byScore(String min, String max) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max must not be null");
    }
    return new ZMoveParams(null, from(min), from(max));
  }

  /**
   * Binary variant of {@link #byScore(String, String)}.
   * @param min the lowest score
   * @param max the highest score
   * @return ZMoveParams
   */
  public static ZMoveParams byScore(byte[] min, byte[] max) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max must not be null");
    }
    return new ZMoveParams(null, from(min), from(max));
  }

  /**
   * Move up to {@code count} members. Without a count, {@code MIN} and {@code MAX} move one member
   * and a score range moves every member in the range.
   * @param count the largest number of members to move
   * @return ZMoveParams
   */
  public ZMoveParams count(long count) {
    this.count = count;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (end != null) {
      args.add(end);
    } else {
      args.add(Keyword.BYSCORE).add(min).add(max);
    }
    if (count != null) {
      args.add(Keyword.COUNT).add(count);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZMoveParams that = (ZMoveParams) o;
    return end == that.end && Objects.equals(min, that.min) && Objects.equals(max, that.max)
        && Objects.equals(count, that.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(end, min, max, count);
  }
}
