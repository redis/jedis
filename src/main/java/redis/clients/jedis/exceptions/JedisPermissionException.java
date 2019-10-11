package redis.clients.jedis.exceptions;

public class JedisPermissionException extends JedisException {
  public JedisPermissionException(String message) {
    super(message);
  }

  public JedisPermissionException(Throwable e) {
    super(e);
  }

  public JedisPermissionException(String message, Throwable cause) {
    super(message, cause);
  }
}
