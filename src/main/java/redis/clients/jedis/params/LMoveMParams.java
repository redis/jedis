package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.ListMoveOrder;

/**
 * Optional arguments for the {@code LMOVEM} and {@code BLMOVEM} commands: the
 * {@code [<COUNT | EXACTLY> count <OBO | BULK>]} block that turns a single-element move into a
 * multi-element move.
 * <p>
 * A count selector ({@link #count(int, ListMoveOrder)} or {@link #exactly(int, ListMoveOrder)})
 * carries both the element count and the mandatory ordering. The wire arguments are emitted in the
 * order {@code <COUNT | EXACTLY> count <OBO | BULK>}.
 * @since 8.0
 */
public class LMoveMParams implements IParams {

  private Keyword selector;
  private Integer count;
  private ListMoveOrder order;

  public static LMoveMParams lMoveMParams() {
    return new LMoveMParams();
  }

  /**
   * Move up to {@code count} elements (same semantics as the {@code count} argument of
   * {@code LPOP}), ordered by {@code order}. Mutually exclusive with
   * {@link #exactly(int, ListMoveOrder)} (last call wins).
   */
  public LMoveMParams count(int count, ListMoveOrder order) {
    if (order == null) {
      throw new IllegalArgumentException("Ordering (OBO or BULK) must not be null.");
    }
    this.selector = Keyword.COUNT;
    this.count = count;
    this.order = order;
    return this;
  }

  /**
   * Move exactly {@code count} elements, or nothing (a {@code null} reply) if the source list does
   * not hold that many, ordered by {@code order}. Mutually exclusive with
   * {@link #count(int, ListMoveOrder)} (last call wins).
   */
  public LMoveMParams exactly(int count, ListMoveOrder order) {
    if (order == null) {
      throw new IllegalArgumentException("Ordering (OBO or BULK) must not be null.");
    }
    this.selector = Keyword.EXACTLY;
    this.count = count;
    this.order = order;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (selector == null || count == null) {
      throw new IllegalArgumentException("COUNT or EXACTLY must be specified.");
    }
    args.add(selector).add(count).add(order);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LMoveMParams that = (LMoveMParams) o;
    return selector == that.selector && Objects.equals(count, that.count) && order == that.order;
  }

  @Override
  public int hashCode() {
    return Objects.hash(selector, count, order);
  }
}
