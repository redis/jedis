package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

/**
 * Parameters for ZINCRBY commands. In fact, Redis doesn't have parameters for ZINCRBY. Instead
 * Redis has INCR parameter for ZADD.
 * <p>
 * When users call ZADD with INCR option, its restriction (only one member) and return type is same
 * to ZINCRBY. Document page for ZADD also describes INCR option to act like ZINCRBY. So we decided
 * to wrap "ZADD with INCR option" to ZINCRBY.
 * <p>
 * Works with Redis 3.0.2 and onwards.
 */
public class ZIncrByParams implements IParams {

  private Keyword existance;

  public ZIncrByParams() {
  }

  public static ZIncrByParams zIncrByParams() {
    return new ZIncrByParams();
  }

  /**
   * Only set the key if it does not already exist.
   * @return ZIncrByParams
   */
  public ZIncrByParams nx() {
    this.existance = Keyword.NX;
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return ZIncrByParams
   */
  public ZIncrByParams xx() {
    this.existance = Keyword.XX;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (existance != null) {
      args.add(existance);
    }

    args.add(Keyword.INCR);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZIncrByParams that = (ZIncrByParams) o;
    return existance == that.existance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(existance);
  }
}
