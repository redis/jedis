package redis.clients.jedis.exceptions;

import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.HostAndPort;

/**
 * Note: This exception extends {@link JedisDataException} just so existing applications catching
 * JedisDataException do not get broken.
 */
public class JedisBroadcastException extends JedisDataException {

  private static final String BROADCAST_ERROR_MESSAGE = "A failure occurred while broadcasting the command.";

  private final Map<HostAndPort, SingleReply> replies = new HashMap<>();

  public JedisBroadcastException() {
    super(BROADCAST_ERROR_MESSAGE);
  }

  public void addReply(HostAndPort node, Object reply) {
    replies.put(node, new SingleReply(reply));
  }

  public void addError(HostAndPort node, JedisDataException error) {
    replies.put(node, new SingleReply(error));
  }

  public static class SingleReply {

    private final Object reply;
    private final JedisDataException error;

    public SingleReply(Object reply) {
      this.reply = reply;
      this.error = null;
    }

    public SingleReply(JedisDataException error) {
      this.reply = null;
      this.error = error;
    }

    public Object getReply() {
      return reply;
    }

    public JedisDataException getError() {
      return error;
    }
  }
}
