package redis.clients.util;

import java.io.Closeable;
import java.io.IOException;

import redis.clients.jedis.exceptions.JedisException;

public class IOUtils {
  private IOUtils() {
  }

  public static void closeQuietly(Closeable closeable) {
    // It's same thing as Apache Commons - IOUtils.closeQuietly()
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException | JedisException e) {
        // ignored
      }
    }
  }
}
