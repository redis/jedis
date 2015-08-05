package redis.clients.jedis.params.sortedset;

import redis.clients.jedis.params.Params;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;

public class ZIncrByParams extends Params {

  private static final String XX = "xx";
  private static final String NX = "nx";
  private static final String INCR = "incr";

  private ZIncrByParams() {
  }

  public static ZIncrByParams zIncrByParams() {
    return new ZIncrByParams();
  }

  /**
   * Only set the key if it does not already exist.
   * @return ZIncrByParams
   */
  public ZIncrByParams nx() {
    addParam(NX);
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return ZIncrByParams
   */
  public ZIncrByParams xx() {
    addParam(XX);
    return this;
  }

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
    byteParams.add(key);

    if (contains(NX)) {
      byteParams.add(SafeEncoder.encode(NX));
    }
    if (contains(XX)) {
      byteParams.add(SafeEncoder.encode(XX));
    }

    byteParams.add(SafeEncoder.encode(INCR));

    for (byte[] arg : args) {
      byteParams.add(arg);
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
