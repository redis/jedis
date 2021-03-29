package redis.clients.jedis.params;

import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class ZAddParams extends Params {

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

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);

    if (contains(NX)) {
      byteParams.add(SafeEncoder.encode(NX));
    }
    if (contains(XX)) {
      byteParams.add(SafeEncoder.encode(XX));
    }
    if (contains(CH)) {
      byteParams.add(SafeEncoder.encode(CH));
    }
    if (contains(LT)) {
      byteParams.add(SafeEncoder.encode(LT));
    }
    if (contains(GT)) {
      byteParams.add(SafeEncoder.encode(GT));
    }

    Collections.addAll(byteParams, args);

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
