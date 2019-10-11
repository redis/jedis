package redis.clients.jedis.exceptions;

public class JedisAuthenticationException extends JedisException {

  public JedisAuthenticationException(String message) {
    super(message);
  }

  public JedisAuthenticationException(Throwable e) {
    super(e);
  }

  public JedisAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
