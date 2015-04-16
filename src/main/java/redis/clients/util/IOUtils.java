package redis.clients.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class IOUtils {
  private IOUtils() {}

  public static void closeQuietly(Closeable resource) {
    // It's same thing as Apache Commons - IOUtils.closeQuietly()
    if (resource != null) {
      try {
        resource.close();
      } catch (IOException e) {
        // pass
      }
    }
  }

  public static void closeQuietly(Socket resource) {
    // It's same thing as Apache Commons - IOUtils.closeQuietly()
    if (resource != null) {
      try {
        resource.close();
      } catch (IOException e) {
        // pass
      }
    }
  }

}
