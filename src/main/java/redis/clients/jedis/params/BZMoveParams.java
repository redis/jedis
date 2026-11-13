package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.SortedSetOption;

/**
 * Arguments for {@code BZMOVE}: the end to move from and the timeout, created with
 * {@link #min(double)} or {@link #max(double)}, and optionally how many members to move.
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
   * Move the members with the lowest scores.
   * @param timeout seconds, fractions allowed; 0 blocks indefinitely
   * @return BZMoveParams
   */
  public static BZMoveParams min(double timeout) {
    return new BZMoveParams(SortedSetOption.MIN, timeout);
  }

  /**
   * Move the members with the highest scores.
   * @param timeout seconds, fractions allowed; 0 blocks indefinitely
   * @return BZMoveParams
   */
  public static BZMoveParams max(double timeout) {
    return new BZMoveParams(SortedSetOption.MAX, timeout);
  }

  /**
   * Move up to {@code count} members.
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
