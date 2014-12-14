package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.nio.ByteBuffer;

public class BulkStringResponseBuilder extends BasicResponseBuilder<byte[]> {
  private boolean carrigeReturn = false;
  private Integer length = null;

  private byte[] responseBuffer;

  // for length
  private boolean isFirstChar = true;
  private boolean isNeg = false;
  private int value = 0;
  private int lengthWithCrLf = 0;

  public void appendPartialResponse(final ByteBuffer buffer) {
    if (length == null) {
      handleLength(buffer);
    }

    if (length != null && length != -1) {
      handleContent(buffer);
    }
  }

  private void handleLength(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete && length == null) {
      byte b = buffer.get();

      if (isFirstChar && b == '-') {
        isNeg = true;
        continue;
      }

      if (carrigeReturn && b == '\n') {
        length = (isNeg ? -value : value);

        if (length == -1) {
          response = null;
          complete = true;
          return;
        }

        lengthWithCrLf = length + 2;
        responseBuffer = new byte[lengthWithCrLf];
      } else if (b == '\r') {
        carrigeReturn = true;
      } else {
        value = value * 10 + (b - '0');
      }
    }
  }

  private void handleContent(final ByteBuffer buffer) {
    int copyLen = Math.min(lengthWithCrLf, buffer.remaining());
    buffer.get(responseBuffer, 0, copyLen);
    lengthWithCrLf -= copyLen;

    if (lengthWithCrLf < 0) {
      throw new JedisConnectionException("Broken response");
    } else if (lengthWithCrLf == 0) {
      complete = true;
      response = new byte[length];
      System.arraycopy(responseBuffer, 0, response, 0, length);
      responseBuffer = null;
    }
  }

}
