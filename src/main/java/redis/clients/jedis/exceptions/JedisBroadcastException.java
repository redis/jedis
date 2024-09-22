package redis.clients.jedis.exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.HostAndPort;

/**
 * Note: This exception extends {@link JedisDataException} just so existing applications catching
 * JedisDataException do not get broken.
 */
// TODO: extends JedisException
public class JedisBroadcastException extends JedisDataException {

  private static final String BROADCAST_ERROR_MESSAGE = "A failure occurred while broadcasting the command.";

  private final Map<HostAndPort, Object> replies = new HashMap<>();

  public JedisBroadcastException() {
    super(BROADCAST_ERROR_MESSAGE);
  }

  public void addReply(HostAndPort node, Object reply) {
    replies.put(node, reply);
  }

  public Map<HostAndPort, Object> getReplies() {
    return Collections.unmodifiableMap(replies);
  }
}
