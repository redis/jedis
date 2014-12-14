package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

public class ErrorResponseBuilder extends BasicResponseBuilder<String> {
  private boolean carrigeReturn = false;
  private StringBuilder buffer = new StringBuilder();

  public void appendPartialResponse(byte b) {
    if (carrigeReturn && b == '\n') {
      exception = handleException(buffer.toString());
      complete = true;
    } else if (b == '\r') {
      carrigeReturn = true;
    } else {
      buffer.append((char) b);
    }
  }

  private JedisException handleException(String message) {
    return new JedisDataException(message);
  }

}
