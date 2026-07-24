package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters for the {@code SDIFFCARD} command.
 * <p>
 * Usage examples:
 *
 * <pre>
 *   new SDiffCardParams().limit(100)
 *   SDiffCardParams.sDiffCardParams().limit(1)
 * </pre>
 *
 * @since 8.0
 */
public class SDiffCardParams implements IParams {

  private Long limit;

  public static SDiffCardParams sDiffCardParams() {
    return new SDiffCardParams();
  }

  /**
   * Cap the returned cardinality at {@code limit}. {@code LIMIT 0} means no limit.
   * @param limit non-negative cap
   * @return SDiffCardParams
   */
  public SDiffCardParams limit(long limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("LIMIT must be non-negative");
    }
    this.limit = limit;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (limit != null) {
      args.add(Keyword.LIMIT);
      args.add(limit);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SDiffCardParams)) return false;
    SDiffCardParams that = (SDiffCardParams) o;
    return Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(limit);
  }
}
