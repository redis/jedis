package redis.clients.util;

import java.io.IOException;
import java.net.Socket;

public class IOUtils {
  private IOUtils() {
  }

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
}
