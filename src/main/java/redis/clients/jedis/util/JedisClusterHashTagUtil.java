package redis.clients.jedis.util;

/**
 * Holds various methods/utilities to manipulate and parse redis hash-tags. See <a
 * href="http://redis.io/topics/cluster-spec">Cluster-Spec : Keys hash tags</a>
 *
 * @deprecated Use {@link JedisClusterHashTag}.
 */
@Deprecated
public final class JedisClusterHashTagUtil {

  private JedisClusterHashTagUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * @deprecated Use {@link JedisClusterHashTag#getHashTag(java.lang.String)}.
   */
  @Deprecated
  public static String getHashTag(String key) {
    return JedisClusterHashTag.getHashTag(key);
  }

  /**
   * @deprecated Use {@link JedisClusterHashTag#isClusterCompliantMatchPattern(byte[])}.
   */
  @Deprecated
  public static boolean isClusterCompliantMatchPattern(byte[] matchPattern) {
    return JedisClusterHashTag.isClusterCompliantMatchPattern(matchPattern);
  }

  /**
   * @deprecated Use {@link JedisClusterHashTag#isClusterCompliantMatchPattern(java.lang.String)}.
   */
  @Deprecated
  public static boolean isClusterCompliantMatchPattern(String matchPattern) {
    return JedisClusterHashTag.isClusterCompliantMatchPattern(matchPattern);
  }
}
