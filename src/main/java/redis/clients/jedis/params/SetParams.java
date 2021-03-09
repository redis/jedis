package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class SetParams extends Params {

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

  public byte[][] getByteParams(byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    Collections.addAll(byteParams, args);

    if (contains(NX)) {
      byteParams.add(SafeEncoder.encode(NX));
    }
    if (contains(XX)) {
      byteParams.add(SafeEncoder.encode(XX));
    }

    if (contains(EX)) {
      byteParams.add(SafeEncoder.encode(EX));
      byteParams.add(Protocol.toByteArray((long) getParam(EX)));
    }
    if (contains(PX)) {
      byteParams.add(SafeEncoder.encode(PX));
      byteParams.add(Protocol.toByteArray((long) getParam(PX)));
    }
    if (contains(EXAT)) {
      byteParams.add(SafeEncoder.encode(EXAT));
      byteParams.add(Protocol.toByteArray((long) getParam(EXAT)));
    }
    if (contains(PXAT)) {
      byteParams.add(SafeEncoder.encode(PXAT));
      byteParams.add(Protocol.toByteArray((long) getParam(PXAT)));
    }
    if (contains(KEEPTTL)) {
      byteParams.add(SafeEncoder.encode(KEEPTTL));
    }

    if (contains(GET)) {
      byteParams.add(SafeEncoder.encode(GET));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
