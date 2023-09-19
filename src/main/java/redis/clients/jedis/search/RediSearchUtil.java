package redis.clients.jedis.search;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.util.SafeEncoder;

public class RediSearchUtil {

  /**
   * Jedis' {@code hset} methods do not support {@link Object}s as values. This method eases process
   * of converting a {@link Map} with Objects as values so that the returning Map can be set to a
   * {@code hset} method.
   * @param input map with object value
   * @return map with string value
   */
  public static Map<String, String> toStringMap(Map<String, Object> input) {
    Map<String, String> output = new HashMap<>(input.size());
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      if (key == null || obj == null) {
        throw new NullPointerException("A null argument cannot be sent to Redis.");
      }
      String str;
      if (obj instanceof byte[]) {
        str = SafeEncoder.encode((byte[]) obj);
      } else if (obj instanceof redis.clients.jedis.GeoCoordinate) {
        redis.clients.jedis.GeoCoordinate geo = (redis.clients.jedis.GeoCoordinate) obj;
        str = geo.getLongitude() + "," + geo.getLatitude();
      } else if (obj instanceof String) {
        str = (String) obj;
      } else {
        str = obj.toString();
      }
      output.put(key, str);
    }
    return output;
  }

  /**
   * x86 systems are little-endian and Java defaults to big-endian. This causes mismatching query
   * results when RediSearch is running in a x86 system. This method helps to convert concerned
   * arrays.
   * @param input float array
   * @return byte array
   */
  public static byte[] ToByteArray(float[] input) {
    byte[] bytes = new byte[Float.BYTES * input.length];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(input);
    return bytes;
  }

  public static final Set<Character> TAG_ESCAPE_CHARS = new HashSet<>(Arrays.asList(//
      ',', '.', '<', '>', '{', '}', '[', //
      ']', '"', '\'', ':', ';', '!', '@', //
      '#', '$', '%', '^', '&', '*', '(', //
      ')', '-', '+', '=', '~', '|' //
  ));

  public static String escape(String text) {
    return escape(text, false);
  }

  public static String escapeQuery(String query) {
    return escape(query, true);
  }

  public static String escape(String text, boolean querying) {
    char[] chars = text.toCharArray();

    StringBuilder sb = new StringBuilder();
    for (char ch : chars) {
      if (TAG_ESCAPE_CHARS.contains(ch)
          || (querying && ch == ' ')) {
        sb.append("\\");
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  public static String unescape(String text) {
    return text.replace("\\", "");
  }

  private RediSearchUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
