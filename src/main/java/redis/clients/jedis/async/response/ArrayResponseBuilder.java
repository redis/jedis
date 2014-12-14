package redis.clients.jedis.async.response;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ArrayResponseBuilder extends BasicResponseBuilder<List<Object>> {
  private boolean carrigeReturn = false;
  private Integer count = null;

  private BasicResponseBuilder currentChildResponse;

  // for length
  private boolean isFirstChar = true;
  private boolean isNeg = false;
  private int value = 0;

  public void appendPartialResponse(final ByteBuffer buffer) {
    if (count == null) {
      handleCount(buffer);
    }

    if (count != null) {
      handleContent(buffer);
    }
  }

  private void handleCount(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete && count == null) {
      byte b = buffer.get();

      if (isFirstChar && b == '-') {
        isNeg = true;
        continue;
      }

      if (carrigeReturn && b == '\n') {
        count = (isNeg ? -value : value);

        if (count == -1) {
          response = null;
          complete = true;
          return;
        } else if (count == 0) {
          response = new ArrayList<Object>();
          complete = true;
          return;
        }

        response = new ArrayList<Object>();
        currentChildResponse = new BasicResponseBuilder();
      } else if (b == '\r') {
        carrigeReturn = true;
      } else {
        value = value * 10 + (b - '0');
      }
    }
  }

  private void handleContent(final ByteBuffer buffer) {
    while (buffer.hasRemaining() && !complete) {
      currentChildResponse.appendPartialResponse(buffer);
      if (currentChildResponse.isComplete()) {
        if (currentChildResponse.getResponse() != null) {
          response.add(currentChildResponse.getResponse());
        } else {
          response.add(currentChildResponse.getException());
        }

        if (response.size() == count) {
          currentChildResponse = null;
          complete = true;
        } else {
          currentChildResponse = new BasicResponseBuilder();
        }
      }
    }
  }

}
