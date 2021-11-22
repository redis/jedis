package redis.clients.jedis.tests.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;

public class RedisVersionUtil {

  public static Integer getRedisMajorVersionNumber() {
    String completeVersion = null;

    try (Jedis jedis = new Jedis(HostAndPortUtil.getRedisServers().get(0))) {
      jedis.auth("foobared");
      String info = jedis.info("server");
      String[] splitted = info.split("\\s+|:");
      for (int i = 0; i < splitted.length; i++) {
        if (splitted[i].equalsIgnoreCase("redis_version")) {
          completeVersion = splitted[i + 1];
          break;
        }
      }
    }

    if (completeVersion == null) {
      return null;
    }
    return Integer.parseInt(completeVersion.substring(0, completeVersion.indexOf(".")));
  }

  public static boolean checkRedisMajorVersionNumber(int minVersion) {
    return getRedisMajorVersionNumber() >= minVersion;
  }
}
