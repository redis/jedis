package redis.clients.jedis.util;

import java.net.URI;

public final class JedisURIHelper {

  private static final int DEFAULT_DB = 0;

  private static final String REDIS = "redis";
  private static final String REDISS = "rediss";

  private JedisURIHelper(){
    throw new InstantiationError( "Must not instantiate this class" );
  }

  public static String getUser(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      String user = userInfo.split(":", 2)[0];
      if (user.isEmpty()) {
        user = null; //return null user is not specified
      }
      return user;
    }
    return null;
  }

  public static String getPassword(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      return userInfo.split(":", 2)[1];
    }
    return null;
  }

  public static int getDBIndex(URI uri) {
    String[] pathSplit = uri.getPath().split("/", 2);
    if (pathSplit.length > 1) {
      String dbIndexStr = pathSplit[1];
      if (dbIndexStr.isEmpty()) {
        return DEFAULT_DB;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return DEFAULT_DB;
    }
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
