package redis.clients.jedis.exceptions;

/**
 * A config error.
 */
public class JedisConfigException extends JedisException {

  private static final long serialVersionUID = 3878126572474819403L;

  public JedisConfigException(String message) {
    super(message);
  }

  public JedisConfigException(Throwable cause) {
    super(cause);
  }

  public JedisConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
