package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters for the {@code SUNIONCARD} command. Options are emitted in the canonical order:
 * {@code APPROX} before {@code LIMIT}.
 * <p>
 * Usage examples:
 *
 * <pre>
 *   new SUnionCardParams().approx().limit(1000)
 *   SUnionCardParams.sUnionCardParams().limit(3)
 * </pre>
 *
 * @since 8.0
 */
public class SUnionCardParams implements IParams {

  private boolean approx;
  private Long limit;

  public static SUnionCardParams sUnionCardParams() {
    return new SUnionCardParams();
  }

  /**
   * Return an approximate cardinality using HyperLogLog (standard error ~0.81%).
   * @return SUnionCardParams
   */
  public SUnionCardParams approx() {
    this.approx = true;
    return this;
  }

  /**
   * Cap the returned cardinality at {@code limit}. {@code LIMIT 0} means no limit.
   * @param limit non-negative cap
   * @return SUnionCardParams
   */
  public SUnionCardParams limit(long limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("LIMIT must be non-negative");
    }
    this.limit = limit;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (approx) {
      args.add(Keyword.APPROX);
    }
    if (limit != null) {
      args.add(Keyword.LIMIT);
      args.add(limit);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SUnionCardParams)) return false;
    SUnionCardParams that = (SUnionCardParams) o;
    return approx == that.approx && Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(approx, limit);
  }
}
