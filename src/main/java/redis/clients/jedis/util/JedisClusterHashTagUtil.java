package redis.clients.jedis.util;

/**
 * Holds various methods/utilities to manipualte and parse redis hash-tags. See <a
 * href="http://redis.io/topics/cluster-spec">Cluster-Spec : Keys hash tags</a>
 */
public final class JedisClusterHashTagUtil {

  private JedisClusterHashTagUtil() {
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
    return tag != null && !tag.isEmpty();
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
