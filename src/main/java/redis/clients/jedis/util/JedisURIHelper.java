package redis.clients.jedis.util;

import java.net.URI;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;

/**
 * Utility class for handling Redis URIs.
 * This class provides methods to extract various components from a Redis URI,
 * such as host, port, user, password, database index, and protocol.
 * It also includes methods to validate the URI and check its scheme.
 *
 * <h2>URI syntax</h2>
 *
 * <blockquote>
 * <i>redis[s]</i><b>{@code ://}</b>[[<i>username</i>]<i>[{@code :}password</i>]@]
 * <i>host</i>[<b>{@code :}</b><i>port</i>][<b>{@code /}</b><i>database</i>]
 * </blockquote>
 *
 *
 * <h2>Authentication</h2>
 * <p>Authentication details can be provided in the URI in the form of a username and password.
 * Redis URIs may contain authentication details that effectively lead to usernames with passwords,
 * password-only, or no authentication.</p>
 * <h3>Examples:</h3>
 * <ul>
 *   <li><b>Username and Password:</b> redis://username:password@host:port</li>
 *   <li><b>Password-only:</b> redis://:password@host:port</li>
 *   <li><b>Empty password:</b> redis://username:@host:port</li>
 *   <li><b>No Authentication:</b> redis://host:port</li>
 * </ul>
 */
public final class JedisURIHelper {

  private static final String REDIS = "redis";
  private static final String REDISS = "rediss";

  private JedisURIHelper() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static HostAndPort getHostAndPort(URI uri) {
    return new HostAndPort(uri.getHost(), uri.getPort());
  }

  /**
   * Extracts the user from the given URI.
   * <p>
   * For details on the URI format and authentication examples, see {@link JedisURIHelper}.
   * </p>
   * @param uri the URI to extract the user from
   * @return the user as a String, or null if user is empty or {@link URI#getUserInfo()} info is missing
   */
  public static String getUser(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      String user = userInfo.split(":", 2)[0];
      if (user.isEmpty()) {
        user = null; // return null user is not specified
      }
      return user;
    }
    return null;
  }

  /**
   * Extracts the password from the given URI.
   * <p>
   * For details on the URI format and authentication examples, see {@link JedisURIHelper}.
   * </p>
   * @param uri the URI to extract the password from
   * @return the password as a String, or null if {@link URI#getUserInfo()} info is missing
   * @throws IllegalArgumentException if {@link URI#getUserInfo()} is provided but does not contain
   *           a password
   */
  public static String getPassword(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      String[] userAndPassword = userInfo.split(":", 2);
      if (userAndPassword.length < 2) {
        throw new IllegalArgumentException("Password not provided in uri.");
      }
      return userAndPassword[1];
    }
    return null;
  }

  /**
   * Checks if the given URI has a database index component.
   *
   * @param uri the URI to check
   * @return true if the URI has a database index component, false otherwise
   */
  public static boolean hasDbIndex(URI uri) {
    if (uri.getPath() == null || uri.getPath().isEmpty()) {
      return false;
    }

    String[] pathSplit = uri.getPath().split("/", 2);

    return pathSplit.length > 1;
  }

  /**
   * Returns the database index from the given URI.
   *
   * @param uri
   * @return database index, or default database (0) if not specified
   */
  public static int getDBIndex(URI uri) {
    String[] pathSplit = uri.getPath().split("/", 2);
    if (pathSplit.length > 1) {
      String dbIndexStr = pathSplit[1];
      if (dbIndexStr.isEmpty()) {
        return Protocol.DEFAULT_DATABASE;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return Protocol.DEFAULT_DATABASE;
    }
  }

  /**
   * Returns the Redis protocol from the given URI.
   *
   * @param uri
   * @return Redis protocol, or null if not specified
   */
  public static RedisProtocol getRedisProtocol(URI uri) {
    if (uri.getQuery() == null) return null;

    String[] params = uri.getQuery().split("&");
    for (String param : params) {
      int idx = param.indexOf("=");
      if (idx < 0) continue;
      if ("protocol".equals(param.substring(0, idx))) {
        String ver = param.substring(idx + 1);
        for (RedisProtocol proto : RedisProtocol.values()) {
          if (proto.version().equals(ver)) {
            return proto;
          }
        }
        throw new IllegalArgumentException("Unknown protocol " + ver);
      }
    }
    return null; // null (default) when not defined
  }

  public static boolean isValid(URI uri) {
    if (isEmpty(uri.getScheme()) || isEmpty(uri.getHost()) || uri.getPort() == -1) {
      return false;
    }

    return true;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().length() == 0;
  }

  public static boolean isRedisScheme(URI uri) {
    return REDIS.equals(uri.getScheme());
  }

  public static boolean isRedisSSLScheme(URI uri) {
    return REDISS.equals(uri.getScheme());
  }

}
