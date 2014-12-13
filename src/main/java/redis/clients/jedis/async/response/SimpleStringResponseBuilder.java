package redis.clients.jedis.async.response;

public class SimpleStringResponseBuilder extends BasicResponseBuilder<String> {
  private boolean carrigeReturn = false;
  private StringBuffer buffer = new StringBuffer();

  public void appendPartialResponse(byte b) {
    if (carrigeReturn && b == '\n') {
      response = buffer.toString();
      complete = true;
    } else if (b == '\r') {
      carrigeReturn = true;
    } else {
      buffer.append((char) b);
    }
  }
}
