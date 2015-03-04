package redis.clients.jedis.exceptions;

public class JedisDataException extends JedisException {
  private static final long serialVersionUID = 3878126572474819403L;

  public JedisDataException(String message) {
    super(message);
  }

  public JedisDataException(Throwable cause) {
    super(cause);
  }

  public JedisDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
