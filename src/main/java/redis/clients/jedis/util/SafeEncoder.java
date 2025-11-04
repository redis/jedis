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

    if (dataToEncode instanceof KeyValue) {
      KeyValue keyValue = (KeyValue) dataToEncode;
      return new KeyValue<>(encodeObject(keyValue.getKey()), encodeObject(keyValue.getValue()));
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

  /**
   * Converts a byte array to uppercase by converting lowercase ASCII letters (a-z) to uppercase (A-Z).
   * This method is optimized for ASCII text and performs direct byte manipulation, which is significantly
   * faster than converting to String and calling String.toUpperCase().
   * <p>
   * This method only works correctly for ASCII text. Non-ASCII characters are left unchanged.
   * For Redis command names (which are always ASCII), this is safe and provides ~47% performance
   * improvement over the String-based approach.
   *
   * @param data the byte array to convert to uppercase
   * @return a new byte array with lowercase ASCII letters converted to uppercase
   */
  public static byte[] toUpperCase(final byte[] data) {
    if (data == null) {
      return null;
    }

    byte[] uppercaseBytes = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      if (data[i] >= 'a' && data[i] <= 'z') {
        uppercaseBytes[i] = (byte) (data[i] - 32);
      } else {
        uppercaseBytes[i] = data[i];
      }
    }
    return uppercaseBytes;
  }
}
