package redis.clients.util;

import java.io.IOException;
import java.net.Socket;

public class IOUtils {
  public static final int DEFAULT_BUFFER_SIZE = 8192;

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
