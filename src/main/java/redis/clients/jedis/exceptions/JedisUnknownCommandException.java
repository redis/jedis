package redis.clients.jedis.exceptions;

/**
 * {@code -ERR unknown command ...} reply from Redis.
 * <p>
 * Thrown when the server does not recognise the command that was sent, for example when
 * {@code HELLO} is issued against Redis &lt; 6.0.
 * </p>
 */
public class JedisUnknownCommandException extends JedisDataException {

  private static final long serialVersionUID = 1L;

  public JedisUnknownCommandException(final String message) {
    super(message);
  }

  public JedisUnknownCommandException(final Throwable cause) {
    super(cause);
  }

  public JedisUnknownCommandException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
