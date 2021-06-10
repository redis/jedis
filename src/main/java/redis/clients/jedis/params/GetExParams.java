package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class GetExParams extends Params {

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

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);

    if (contains(EX)) {
      byteParams.add(SafeEncoder.encode(EX));
      byteParams.add(Protocol.toByteArray((long) getParam(EX)));
    } else if (contains(PX)) {
      byteParams.add(SafeEncoder.encode(PX));
      byteParams.add(Protocol.toByteArray((long) getParam(PX)));
    } else if (contains(EXAT)) {
      byteParams.add(SafeEncoder.encode(EXAT));
      byteParams.add(Protocol.toByteArray((long) getParam(EXAT)));
    } else if (contains(PXAT)) {
      byteParams.add(SafeEncoder.encode(PXAT));
      byteParams.add(Protocol.toByteArray((long) getParam(PXAT)));
    } else if (contains(PERSIST)) {
      byteParams.add(SafeEncoder.encode(PERSIST));
    }

    Collections.addAll(byteParams, args);

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
