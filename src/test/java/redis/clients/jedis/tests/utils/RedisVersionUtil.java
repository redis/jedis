package redis.clients.jedis.tests.utils;

import redis.clients.jedis.Jedis;

public class RedisVersionUtil {

  public static int getRedisMajorVersionNumber() {
    String completeVersion = null;

    try (Jedis jedis = new Jedis()) {
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
      return 0;
    }
    return Integer.parseInt(completeVersion.substring(0, completeVersion.indexOf(".")));
  }

  public static boolean checkRedisMajorVersionNumber(int minVersion) {
    return getRedisMajorVersionNumber() >= minVersion;
  }
}
