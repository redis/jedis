package redis.clients.jedis.async.response;

import java.util.ArrayList;

public class BulkStringResponseBuilder extends BasicResponseBuilder<byte[]> {
  private boolean carrigeReturn = false;
  private Integer length = null;

  private StringBuffer lengthBuffer = new StringBuffer();
  private byte[] buffer;
  private int currIndex = 0;

  public void appendPartialResponse(byte b) {
    if (length == null) {
      handleLength(b);
    } else {
      handleContent(b);
    }
  }

  private void handleContent(byte b) {
    if (carrigeReturn) {
      if (b == '\n') {
        carrigeReturn = false;
        complete = true;
        response = buffer;
        return;
      } else {
        carrigeReturn = false;
        buffer[currIndex++] = '\r';
      }
    }

    if (b == '\r') {
      carrigeReturn = true;
    } else {
      buffer[currIndex++] = b;
    }
  }

  private void handleLength(byte b) {
    if (carrigeReturn && b == '\n') {
      carrigeReturn = false;
      length = Integer.valueOf(lengthBuffer.toString());

      if (length == -1) {
        response = null;
        complete = true;
        return;
      }

      buffer = new byte[length];
    } else if (b == '\r') {
      carrigeReturn = true;
    } else {
      lengthBuffer.append((char) b);
    }
  }
}
