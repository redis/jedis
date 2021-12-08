package redis.clients.jedis.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The only reason to have this is to be able to compatible with java 1.5 :(
 */
public final class SafeEncoder {

  public static volatile Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private SafeEncoder() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static byte[][] encodeMany(final String... strs) {
    byte[][] many = new byte[strs.length][];
    for (int i = 0; i < strs.length; i++) {
      many[i] = encode(strs[i]);
    }
    return many;
  }

  public static byte[] encode(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("null value cannot be sent to redis");
    }
    return str.getBytes(DEFAULT_CHARSET);
  }

  public static String encode(final byte[] data) {
    return new String(data, DEFAULT_CHARSET);
  }

  /**
   * This method takes an object and will convert all bytes[] and list of byte[] and will encode the
   * object in a recursive way.
   * @param dataToEncode
   * @return the object fully encoded
   */
  public static Object encodeObject(Object dataToEncode) {
    if (dataToEncode instanceof byte[]) {
      return SafeEncoder.encode((byte[]) dataToEncode);
    }

    if (dataToEncode instanceof List) {
      List arrayToDecode = (List) dataToEncode;
      List returnValueArray = new ArrayList(arrayToDecode.size());
      for (Object arrayEntry : arrayToDecode) {
        // recursive call and add to list
        returnValueArray.add(encodeObject(arrayEntry));
      }
      return returnValueArray;
    }

    return dataToEncode;
  }
}
