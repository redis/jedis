package redis.clients.jedis.exceptions;

/**
 * {@code -NOPROTO} reply from Redis Enterprise and Cloud when the requested protocol version has
 * been explicitly disabled on the server or Redis &lt; 6.0
 */
public class JedisProtocolNotSupportedException extends JedisDataException {

  private static final long serialVersionUID = 1L;

  public JedisProtocolNotSupportedException(final String message) {
    super(message);
  }

  public JedisProtocolNotSupportedException(final Throwable cause) {
    super(cause);
  }

  public JedisProtocolNotSupportedException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
