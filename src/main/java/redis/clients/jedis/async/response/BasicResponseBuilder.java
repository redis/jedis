package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.ByteBuffer;

public class BasicResponseBuilder<T> implements AsyncResponseBuilder<T> {
  protected boolean complete = false;
  protected T response = null;
  protected JedisException exception = null;
  private AsyncResponseBuilder childResponse = null;

  public void appendPartialResponse(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete) {
      if (childResponse == null) {
        byte b = buffer.get();
        if (b == '*') {
          childResponse = new ArrayResponseBuilder();
        } else if (b == '$') {
          childResponse = new BulkStringResponseBuilder();
        } else if (b == ':') {
          childResponse = new IntegerResponseBuilder();
        } else if (b == '-') {
          childResponse = new ErrorResponseBuilder();
        } else if (b == '+') {
          childResponse = new SimpleStringResponseBuilder();
        } else {
          exception = new JedisConnectionException("Unknown reply: " + (char) b);
          complete = true;
        }
      } else {
        childResponse.appendPartialResponse(buffer);
        if (childResponse.isComplete()) {
          exception = childResponse.getException();
          response = (T) childResponse.getResponse();
          complete = true;
        }
      }
    }
  }

  public boolean isComplete() {
    return complete;
  }

  public T getResponse() {
    return response;
  }

  public JedisException getException() {
    return exception;
  }
}
