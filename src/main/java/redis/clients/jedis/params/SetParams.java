package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;

public class SetParams extends Params implements IParams {

  private static final String XX = "xx";
  private static final String NX = "nx";
  private static final String PX = "px";
  private static final String EX = "ex";
  private static final String EXAT = "exat";
  private static final String PXAT = "pxat";
  private static final String KEEPTTL = "keepttl";
  private static final String GET = "get";

  public SetParams() {
  }

  public static SetParams setParams() {
    return new SetParams();
  }

  /**
   * Set the specified expire time, in seconds.
   * @param secondsToExpire
   * @return SetParams
   * @deprecated Use {@link #ex(long)}.
   */
  @Deprecated
  public SetParams ex(int secondsToExpire) {
    return ex((long) secondsToExpire);
  }

  /**
   * Set the specified expire time, in seconds.
   * @param secondsToExpire
   * @return SetParams
   */
  public SetParams ex(long secondsToExpire) {
    addParam(EX, secondsToExpire);
    return this;
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @param millisecondsToExpire
   * @return SetParams
   */
  public SetParams px(long millisecondsToExpire) {
    addParam(PX, millisecondsToExpire);
    return this;
  }

  /**
   * Only set the key if it does not already exist.
   * @return SetParams
   */
  public SetParams nx() {
    addParam(NX);
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return SetParams
   */
  public SetParams xx() {
    addParam(XX);
    return this;
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param seconds
   * @return SetParams
   */
  public SetParams exAt(long seconds) {
    addParam(EXAT, seconds);
    return this;
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param milliseconds
   * @return SetParams
   */
  public SetParams pxAt(long milliseconds) {
    addParam(PXAT, milliseconds);
    return this;
  }

  /**
   * Retain the time to live associated with the key.
   * @return SetParams
   */
  public SetParams keepttl() {
    addParam(KEEPTTL);
    return this;
  }

  /**
   * Return the old value stored at key, or nil when key did not exist.
   * @return SetParams
   */
  public SetParams get() {
    addParam(GET);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (contains(NX)) {
      args.addObject(Keyword.NX);
    }
    if (contains(XX)) {
      args.addObject(Keyword.XX);
    }

    if (contains(EX)) {
      args.addObject(Keyword.EX);
      args.addObject(Protocol.toByteArray((long) getParam(EX)));
    }
    if (contains(PX)) {
      args.addObject(Keyword.PX);
      args.addObject(Protocol.toByteArray((long) getParam(PX)));
    }
    if (contains(EXAT)) {
      args.addObject(Keyword.EXAT);
      args.addObject(Protocol.toByteArray((long) getParam(EXAT)));
    }
    if (contains(PXAT)) {
      args.addObject(Keyword.PXAT);
      args.addObject(Protocol.toByteArray((long) getParam(PXAT)));
    }
    if (contains(KEEPTTL)) {
      args.addObject(Keyword.KEEPTTL);
    }

    if (contains(GET)) {
      args.addObject(Keyword.GET);
    }
  }

}
