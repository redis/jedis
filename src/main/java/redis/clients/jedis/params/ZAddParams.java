package redis.clients.jedis.params;

import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;

public class ZAddParams extends Params {

  private static final String XX = "xx";
  private static final String NX = "nx";
  private static final String CH = "ch";

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

    for (byte[] arg : args) {
      byteParams.add(arg);
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
