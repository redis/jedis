package redis.clients.jedis.util;

import java.io.IOException;
import java.net.Socket;

public class IOUtils {

  public static void closeQuietly(Socket sock) {
    // It's same thing as Apache Commons - IOUtils.closeQuietly()
    if (sock != null) {
      try {
        sock.close();
      } catch (IOException e) {
        // ignored
      }
    }
  }

  private IOUtils() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
