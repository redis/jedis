package redis.clients.jedis.exceptions;

public class JedisAuthenticationException extends JedisDataException {

  public JedisAuthenticationException(String message) {
    super(message);
  }

  public JedisAuthenticationException(Throwable cause) {
    super(cause);
  }

  public JedisAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
