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

  public static boolean isClusterCompliantMatchPattern(byte[] matchPattern) {
    return isClusterCompliantMatchPattern(SafeEncoder.encode(matchPattern));
  }

  public static boolean isClusterCompliantMatchPattern(String matchPattern) {
    String tag = extractHashTag(matchPattern, false);
    return tag != null && !tag.isEmpty() && !containsGlobCharacter(tag);
  }

  /**
   * A glob character or an escape inside the hash tag lets the pattern match keys with other hash
   * tags, which live in other slots. The server's patternHashSlot() does not compute a slot for
   * such a pattern either.
   */
  private static boolean containsGlobCharacter(String tag) {
    for (int i = 0; i < tag.length(); i++) {
      char c = tag.charAt(i);
      if (c == '*' || c == '?' || c == '[' || c == '\\') {
        return true;
      }
    }
    return false;
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
