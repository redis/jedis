package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.SortedSetOption;

/**
 * Arguments for the {@code BZMOVE} command: which end of the source sorted set to take members
 * from, how long to block when it is empty, and, optionally, how many members to move.
 * <p>
 * An instance is created by {@link #min(double)} or {@link #max(double)}, each carrying the
 * mandatory end and timeout. The wire arguments are emitted in the order
 * {@code <MIN | MAX> timeout [COUNT count]}; {@code COUNT} is sent only when {@link #count(long)}
 * was called.
 * @since 8.1
 */
public class BZMoveParams implements IParams {

  private final SortedSetOption end;
  private final double timeout;
  private Long count;

  private BZMoveParams(SortedSetOption end, double timeout) {
    this.end = end;
    this.timeout = timeout;
  }

  /**
   * Take the members with the lowest scores.
   * @param timeout the longest time to wait, in seconds, a fraction allowed; {@code 0} waits
   *          without limit. The server rejects an infinite, NaN or negative timeout, so
   *          {@link Double#POSITIVE_INFINITY} is not a way to wait without limit.
   * @return BZMoveParams
   */
  public static BZMoveParams min(double timeout) {
    return new BZMoveParams(SortedSetOption.MIN, timeout);
  }

  /**
   * Take the members with the highest scores.
   * @param timeout the longest time to wait, in seconds, a fraction allowed; {@code 0} waits
   *          without limit. The server rejects an infinite, NaN or negative timeout, so
   *          {@link Double#POSITIVE_INFINITY} is not a way to wait without limit.
   * @return BZMoveParams
   */
  public static BZMoveParams max(double timeout) {
    return new BZMoveParams(SortedSetOption.MAX, timeout);
  }

  /**
   * Move up to {@code count} members. Without a count, one member is moved.
   * @param count the largest number of members to move
   * @return BZMoveParams
   */
  public BZMoveParams count(long count) {
    this.count = count;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(end).add(timeout);
    if (count != null) {
      args.add(Keyword.COUNT).add(count);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BZMoveParams that = (BZMoveParams) o;
    return end == that.end && Double.compare(timeout, that.timeout) == 0
        && Objects.equals(count, that.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(end, timeout, count);
  }
}
