package redis.clients.jedis.util;

import java.net.URI;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;

public final class JedisURIHelper {

  private static final String REDIS = "redis";
  private static final String REDISS = "rediss";

  private JedisURIHelper() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static HostAndPort getHostAndPort(URI uri) {
    return new HostAndPort(uri.getHost(), uri.getPort());
  }

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
        return Protocol.DEFAULT_DATABASE;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return Protocol.DEFAULT_DATABASE;
    }
  }

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
