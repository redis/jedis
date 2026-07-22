package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Optional arguments for the {@code LMOVEM} and {@code BLMOVEM} commands: the
 * {@code [<COUNT | EXACTLY> count <OBO | BULK>]} block that turns a single-element move into a
 * multi-element move.
 * <p>
 * A count selector ({@link #count(int)} or {@link #exactly(int)}) is required. The ordering
 * ({@link #obo()} or {@link #bulk()}) is optional and defaults to {@code BULK}. The wire arguments
 * are emitted in the order {@code <COUNT | EXACTLY> count <OBO | BULK>}.
 * @since 8.0
 */
public class LMoveMParams implements IParams {

  private Keyword selector;
  private Integer count;
  private Keyword ordering;

  public static LMoveMParams lMoveMParams() {
    return new LMoveMParams();
  }

  /**
   * Move up to {@code count} elements (same semantics as the {@code count} argument of
   * {@code LPOP}). Mutually exclusive with {@link #exactly(int)} (last call wins).
   */
  public LMoveMParams count(int count) {
    this.selector = Keyword.COUNT;
    this.count = count;
    return this;
  }

  /**
   * Move exactly {@code count} elements, or nothing (a {@code null} reply) if the source list does
   * not hold that many. Mutually exclusive with {@link #count(int)} (last call wins).
   */
  public LMoveMParams exactly(int count) {
    this.selector = Keyword.EXACTLY;
    this.count = count;
    return this;
  }

  /**
   * Move elements one by one, so they arrive at the destination in pop order. Mutually exclusive
   * with {@link #bulk()} (last call wins). When neither is set, ordering defaults to {@code BULK}.
   */
  public LMoveMParams obo() {
    this.ordering = Keyword.OBO;
    return this;
  }

  /**
   * Move all elements in bulk, preserving the source sub-list order at the destination. Mutually
   * exclusive with {@link #obo()} (last call wins). This is the default ordering.
   */
  public LMoveMParams bulk() {
    this.ordering = Keyword.BULK;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (selector == null || count == null) {
      throw new IllegalArgumentException("COUNT or EXACTLY must be specified.");
    }
    args.add(selector).add(count).add(ordering != null ? ordering : Keyword.BULK);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LMoveMParams that = (LMoveMParams) o;
    return selector == that.selector && Objects.equals(count, that.count)
        && ordering == that.ordering;
  }

  @Override
  public int hashCode() {
    return Objects.hash(selector, count, ordering);
  }
}
