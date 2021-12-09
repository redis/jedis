package redis.clients.jedis.tairhash;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.params.Params;

public class ExhBaseParams extends Params implements IParams {
  private static final String PX = "px";
  private static final String EX = "ex";
  private static final String EXAT = "exat";
  private static final String PXAT = "pxat";

  private static final String VER = "ver";
  private static final String ABS = "abs";

  private static final String KEEPTTL = "keepttl";

  public ExhBaseParams() {
  }

  public static ExhBaseParams ExhBaseParams() {
    return new ExhBaseParams();
  }

  /**
   * Set the specified expire time, in seconds.
   * @param secondsToExpire
   */
  public ExhBaseParams ex(long secondsToExpire) {
    addParam(EX, secondsToExpire);
    return this;
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @param millisecondsToExpire
   */
  public ExhBaseParams px(long millisecondsToExpire) {
    addParam(PX, millisecondsToExpire);
    return this;
  }

  /**
   * Set the specified absolute expire time, in seconds.
   * @param secondsToExpire
   */
  public ExhBaseParams exat(long secondsToExpire) {
    addParam(EXAT, secondsToExpire);
    return this;
  }

  /**
   * Set the specified absolute expire time, in milliseconds.
   * @param millisecondsToExpire
   */
  public ExhBaseParams pxat(long millisecondsToExpire) {
    addParam(PXAT, millisecondsToExpire);
    return this;
  }

  /**
   * Set if version equal or not exist
   * @param version
   */
  public ExhBaseParams ver(long version) {
    if (contains(ABS)) {
      throw new IllegalStateException("Only one of ABS or VER can be specified");
    }
    addParam(VER, version);
    return this;
  }

  /**
   * Set version to absoluteVersion
   * @param absoluteVersion
   */
  public ExhBaseParams abs(long absoluteVersion) {
    if (contains(VER)) {
      throw new IllegalStateException("Only one of ABS or VER can be specified");
    }
    addParam(ABS, absoluteVersion);
    return this;
  }

  /**
   * Do not update ttl
   */
  public ExhBaseParams keepttl() {
    addParam(KEEPTTL);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (contains(EX)) {
      args.add(EX);
      args.add(Protocol.toByteArray((long) getParam(EX)));
    }
    if (contains(PX)) {
      args.add(PX);
      args.add(Protocol.toByteArray((long) getParam(PX)));
    }
    if (contains(EXAT)) {
      args.add(EXAT);
      args.add(Protocol.toByteArray((long) getParam(EXAT)));
    }
    if (contains(PXAT)) {
      args.add(PXAT);
      args.add(Protocol.toByteArray((long) getParam(PXAT)));
    }

    if (contains(VER)) {
      args.add(VER);
      args.add(Protocol.toByteArray((long) getParam(VER)));
    }
    if (contains(ABS)) {
      args.add(ABS);
      args.add(Protocol.toByteArray((long) getParam(ABS)));
    }

    if (contains(KEEPTTL)) {
      args.add(KEEPTTL);
    }
  }
}
