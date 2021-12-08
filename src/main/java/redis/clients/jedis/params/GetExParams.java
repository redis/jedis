package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class GetExParams extends Params implements IParams {

  private static final String PX = "px";
  private static final String EX = "ex";
  private static final String EXAT = "exat";
  private static final String PXAT = "pxat";
  private static final String PERSIST = "persist";

  public GetExParams() {
  }

  public static GetExParams getExParams() {
    return new GetExParams();
  }

  /**
   * Set the specified expire time, in seconds.
   * @return GetExParams
   */
  public GetExParams ex(long secondsToExpire) {
    addParam(EX, secondsToExpire);
    return this;
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @return GetExParams
   */
  public GetExParams px(long millisecondsToExpire) {
    addParam(PX, millisecondsToExpire);
    return this;
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param seconds
   * @return GetExParams
   */
  public GetExParams exAt(long seconds) {
    addParam(EXAT, seconds);
    return this;
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param milliseconds
   * @return GetExParams
   */
  public GetExParams pxAt(long milliseconds) {
    addParam(PXAT, milliseconds);
    return this;
  }

  /**
   * Remove the time to live associated with the key.
   * @return GetExParams
   */
  public GetExParams persist() {
    addParam(PERSIST);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (contains(EX)) {
      args.add(SafeEncoder.encode(EX));
      args.add(Protocol.toByteArray((long) getParam(EX)));
    } else if (contains(PX)) {
      args.add(SafeEncoder.encode(PX));
      args.add(Protocol.toByteArray((long) getParam(PX)));
    } else if (contains(EXAT)) {
      args.add(SafeEncoder.encode(EXAT));
      args.add(Protocol.toByteArray((long) getParam(EXAT)));
    } else if (contains(PXAT)) {
      args.add(SafeEncoder.encode(PXAT));
      args.add(Protocol.toByteArray((long) getParam(PXAT)));
    } else if (contains(PERSIST)) {
      args.add(SafeEncoder.encode(PERSIST));
    }
  }

}
