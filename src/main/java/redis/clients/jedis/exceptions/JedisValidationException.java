package redis.clients.jedis.exceptions;

/**
 * A validation error.
 */
public class JedisValidationException extends JedisException {

  private static final long serialVersionUID = 1134169242443303479L;

  public JedisValidationException(String message) {
    super(message);
  }

  public JedisValidationException(Throwable cause) {
    super(cause);
  }

  public JedisValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
