package redis.clients.jedis.async.response;

public class IntegerResponseBuilder extends BasicResponseBuilder<Long> {
    private boolean carrigeReturn = false;
    private StringBuffer buffer = new StringBuffer();

    public void appendPartialResponse(byte b) {
	if (carrigeReturn && b == '\n') {
	    response = Long.valueOf(buffer.toString());
	    complete = true;
	} else if (b == '\r') {
	    carrigeReturn = true;
	} else {
	    buffer.append((char) b);
	}
    }
}
