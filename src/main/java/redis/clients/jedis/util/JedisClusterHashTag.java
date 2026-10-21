package redis.clients.jedis.util;

/**
 * Holds various methods/utilities to manipulate and parse redis hash-tags. See <a
 * href="http://redis.io/topics/cluster-spec">Cluster-Spec : Keys hash tags</a>
 */
public final class JedisClusterHashTag {

  private JedisClusterHashTag() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static String getHashTag(String key) {
    return extractHashTag(key, true);
  }

  /**
   * The hash tag is located on the bytes, the same way as {@link JedisClusterCRC16#getSlot(byte[])}
   * and the server do. A glob character or an escape inside the tag lets the pattern match keys
   * with other hash tags, which live in other slots, so such a pattern is not compliant. The
   * server's patternHashSlot() does not compute a slot for it either.
   */
  public static boolean isClusterCompliantMatchPattern(byte[] matchPattern) {
    int s = -1;
    for (int i = 0; i < matchPattern.length; i++) {
      byte b = matchPattern[i];
      if (s == -1) {
        if (b == '{') {
          s = i;
        }
      } else if (b == '}') {
        return i > s + 1;
      } else if (b == '*' || b == '?' || b == '[' || b == '\\') {
        return false;
      }
    }
    return false;
  }

  public static boolean isClusterCompliantMatchPattern(String matchPattern) {
    // Check the encoded bytes: under a DBCS default charset (GBK, Shift_JIS, Big5) a trail byte can
    // be a brace or a glob character while the corresponding char is not.
    return isClusterCompliantMatchPattern(SafeEncoder.encode(matchPattern));
  }

  private static String extractHashTag(String key, boolean returnKeyOnAbsence) {
    int s = key.indexOf("{");
    if (s > -1) {
      int e = key.indexOf("}", s + 1);
      if (e > -1 && e != s + 1) {
        return key.substring(s + 1, e);
      }
    }
    return returnKeyOnAbsence ? key : null;
  }
}
