package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class ZAddParams implements IParams {

  private Keyword existence;
  private Keyword comparison;
  private boolean change;

  public ZAddParams() {
  }

  public static ZAddParams zAddParams() {
    return new ZAddParams();
  }

  /**
   * Only set the key if it does not already exist.
   * @return ZAddParams
   */
  public ZAddParams nx() {
    this.existence = Keyword.NX;
    return this;
  }

  /**
   * Only set the key if it already exists.
   * @return ZAddParams
   */
  public ZAddParams xx() {
    this.existence = Keyword.XX;
    return this;
  }

  /**
   * Only update existing elements if the new score is greater than the current score.
   * @return ZAddParams
   */
  public ZAddParams gt() {
    this.comparison = Keyword.GT;
    return this;
  }

  /**
   * Only update existing elements if the new score is less than the current score.
   * @return ZAddParams
   */
  public ZAddParams lt() {
    this.comparison = Keyword.LT;
    return this;
  }

  /**
   * Modify the return value from the number of new elements added to the total number of elements
   * changed
   * @return ZAddParams
   */
  public ZAddParams ch() {
    this.change = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (existence != null) {
      args.add(existence);
    }
    if (comparison != null) {
      args.add(comparison);
    }
    if (change) {
      args.add(Keyword.CH);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZAddParams that = (ZAddParams) o;
    return change == that.change && existence == that.existence && comparison == that.comparison;
  }

  @Override
  public int hashCode() {
    return Objects.hash(existence, comparison, change);
  }
}
