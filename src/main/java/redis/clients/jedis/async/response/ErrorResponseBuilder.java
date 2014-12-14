package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.ByteBuffer;

public class ErrorResponseBuilder extends BasicResponseBuilder<String> {
  private boolean carrigeReturn = false;
  private StringBuilder responseBuffer = new StringBuilder();

  public void appendPartialResponse(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete) {
      byte b = buffer.get();
      if (carrigeReturn && b == '\n') {
        exception = handleException(this.responseBuffer.toString());
        complete = true;
      } else if (b == '\r') {
        carrigeReturn = true;
      } else {
        this.responseBuffer.append((char) b);
      }
    }

  }

  private JedisException handleException(String message) {
    return new JedisDataException(message);
  }

}
