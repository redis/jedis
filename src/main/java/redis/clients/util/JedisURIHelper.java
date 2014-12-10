package redis.clients.util;

import java.net.URI;

public class JedisURIHelper {
  public static String getPassword(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      return userInfo.split(":", 2)[1];
    }
    return null;
  }

  public static Integer getDBIndex(URI uri) {
    String[] pathSplit = uri.getPath().split("/", 2);
    if (pathSplit.length > 1) {
      String dbIndexStr = pathSplit[1];
      if (dbIndexStr.isEmpty()) {
        return 0;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return 0;
    }
  }
}
