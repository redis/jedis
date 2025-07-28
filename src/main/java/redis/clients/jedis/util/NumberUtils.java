package redis.clients.jedis.util;

public final class NumberUtils {

  public static int safeToInt(long millis) {
    if (millis > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }

    return (int) millis;
  }

}
