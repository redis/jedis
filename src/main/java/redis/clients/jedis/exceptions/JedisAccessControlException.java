package redis.clients.jedis.exceptions;

/**
 * An access control error reply from Redis; i.e. {@code -WRONGPASS}, {@code -NOPERM}.
 */
public class JedisAccessControlException extends JedisDataException {

  public JedisAccessControlException(String message) {
    super(message);
  }

  public JedisAccessControlException(Throwable cause) {
    super(cause);
  }

  public JedisAccessControlException(String message, Throwable cause) {
    super(message, cause);
  }
}
