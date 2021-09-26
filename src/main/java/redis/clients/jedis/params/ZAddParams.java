package redis.clients.jedis.params;

import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import redis.clients.jedis.CommandArguments;

public class ZAddParams extends Params implements IParams {

  private static final String XX = "xx";
  private static final String NX = "nx";
  private static final String CH = "ch";
  private static final String LT = "lt";
  private static final String GT = "gt";

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
    addParam(NX);
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return ZAddParams
   */
  public ZAddParams xx() {
    addParam(XX);
    return this;
  }

  /**
   * Modify the return value from the number of new elements added to the total number of elements
   * changed
   * @return ZAddParams
   */
  public ZAddParams ch() {
    addParam(CH);
    return this;
  }

  /**
   * Only update existing elements if the new score is greater than the current score.
   * @return ZAddParams
   */
  public ZAddParams gt() {
    addParam(GT);
    return this;
  }

  /**
   * Only update existing elements if the new score is less than the current score.
   * @return ZAddParams
   */
  public ZAddParams lt() {
    addParam(LT);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (contains(NX)) {
      args.addObject(NX);
    }
    if (contains(XX)) {
      args.addObject(XX);
    }
    if (contains(CH)) {
      args.addObject(CH);
    }
    if (contains(LT)) {
      args.addObject(LT);
    }
    if (contains(GT)) {
      args.addObject(GT);
    }
  }

}
