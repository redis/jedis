package redis.clients.util;

/**
 * Holds various methods/utilities to manipualte and parse redis hash-tags.
 * See <a
 * href="http://redis.io/topics/cluster-spec">Cluster-Spec : Keys hash tags</a>
 */
public final class JedisClusterHashTagUtil {

  private JedisClusterHashTagUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static String getHashTag(String key) {
    return extractHashTag(key, false, false);
  }

  public static boolean isClusterCompliantMatchPattern(String matchPattern) {
    String tag = extractHashTag(matchPattern, true, true);
    return tag != null && !tag.isEmpty() ;
  }

  private static String extractHashTag(String key, boolean includeBrackets, boolean returnNullWhenTagNotFound) {
    int s = key.indexOf("{");
    if (s > -1) {
      int e = key.indexOf("}", s + 1);
      if (e > -1 && e != s + 1) {
        if (includeBrackets) {
          return key.substring(s, e + 1);
        } else {
          return key.substring(s + 1, e);
        }
      }
    }
    if (returnNullWhenTagNotFound) {
      return null;
    } else {
      return key;
    }
  }  
}
