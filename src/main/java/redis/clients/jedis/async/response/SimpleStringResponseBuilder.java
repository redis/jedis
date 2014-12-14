package redis.clients.jedis.async.response;

import java.nio.ByteBuffer;

public class SimpleStringResponseBuilder extends BasicResponseBuilder<String> {
  private boolean carrigeReturn = false;
  private StringBuilder responseBuffer = new StringBuilder();

  public void appendPartialResponse(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete) {
      byte b = buffer.get();
      if (carrigeReturn && b == '\n') {
        response = this.responseBuffer.toString();
        complete = true;
      } else if (b == '\r') {
        carrigeReturn = true;
      } else {
        this.responseBuffer.append((char) b);
      }
    }

  }
}
