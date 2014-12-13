package redis.clients.jedis.async.response;

import java.util.ArrayList;
import java.util.List;

public class ArrayResponseBuilder extends BasicResponseBuilder<List<Object>> {
  private boolean carrigeReturn = false;
  private Integer count = null;

  private StringBuffer countBuffer = new StringBuffer();
  private BasicResponseBuilder currentChildResponse;

  public void appendPartialResponse(byte b) {
    if (count == null) {
      handleCount(b);
    } else {
      handleContent(b);
    }
  }

  private void handleCount(byte b) {
    if (carrigeReturn && b == '\n') {
      carrigeReturn = false;
      count = Integer.valueOf(countBuffer.toString());

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
      countBuffer.append((char) b);
    }
  }

  private void handleContent(byte b) {
    currentChildResponse.appendPartialResponse(b);
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
