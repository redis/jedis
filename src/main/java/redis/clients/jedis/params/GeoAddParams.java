package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.util.SafeEncoder;

public class GeoAddParams extends Params {

  private static final String NX = "nx";
  private static final String XX = "xx";
  private static final String CH = "ch";

  public GeoAddParams() {
  }

  public static GeoAddParams geoAddParams() {
    return new GeoAddParams();
  }

  /**
   * Don't update already existing elements. Always add new elements.
   * @return GetExParams
   */
  public GeoAddParams nx() {
    addParam(NX);
    return this;
  }

  /**
   * Only update elements that already exist. Never add elements.
   * @return GetExParams
   */
  public GeoAddParams xx() {
    addParam(XX);
    return this;
  }

  /**
   * Modify the return value from the number of new elements added, to the total number of elements
   * changed
   * @return GetExParams
   */
  public GeoAddParams ch() {
    addParam(CH);
    return this;
  }

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    List<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);

    if (contains(NX)) {
      byteParams.add(SafeEncoder.encode(NX));
    } else if (contains(XX)) {
      byteParams.add(SafeEncoder.encode(XX));
    }

    if (contains(CH)) {
      byteParams.add(SafeEncoder.encode(CH));
    }

    Collections.addAll(byteParams, args);

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
