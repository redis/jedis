package redis.clients.jedis.exceptions;

/**
 * The server cannot provide the requested protocol version. Either it replied {@code -NOPROTO}
 * (the version is not supported, or is disabled on the server, as Redis Software and Redis Cloud
 * allow), or it has no {@code HELLO} command (for example Redis before 6.0).
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
