package redis.clients.jedis.exceptions;

/**
 * @deprecated This exception will be removed in next major release. Use {@link JedisException}.
 */
@Deprecated
public class JedisExhaustedPoolException extends JedisException {

  public JedisExhaustedPoolException(String message) {
    super(message);
  }

  public JedisExhaustedPoolException(Throwable e) {
    super(e);
  }

  public JedisExhaustedPoolException(String message, Throwable cause) {
    super(message, cause);
  }
}
