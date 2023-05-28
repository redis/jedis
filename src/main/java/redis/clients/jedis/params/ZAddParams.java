package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class ZAddParams implements IParams {

  private Keyword existance;
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
    this.existance = Keyword.NX;
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return ZAddParams
   */
  public ZAddParams xx() {
    this.existance = Keyword.XX;
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
    if (existance != null) {
      args.add(existance);
    }
    if (comparison != null) {
      args.add(comparison);
    }
    if (change) {
      args.add(Keyword.CH);
    }
  }

}
