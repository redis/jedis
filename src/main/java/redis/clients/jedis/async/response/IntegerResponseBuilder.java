package redis.clients.jedis.async.response;

import java.nio.ByteBuffer;

public class IntegerResponseBuilder extends BasicResponseBuilder<Long> {
  private boolean carrigeReturn = false;
  private long value = 0;
  private boolean isNeg = false;
  private boolean isFirstChar = true;

  public void appendPartialResponse(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete) {
      byte b = buffer.get();

      if (isFirstChar && b == '-') {
        isNeg = true;
        continue;
      }

      if (carrigeReturn && b == '\n') {
        response = (isNeg ? -value : value);
        complete = true;
      } else if (b == '\r') {
        carrigeReturn = true;
      } else {
        value = value * 10 + (b - '0');
      }
    }
  }
}
