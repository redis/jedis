package redis.clients.jedis.exceptions;

/**
 * This exception will be thrown when the Jedis client isn't able to retrieve a connection from the
 * pool, since all the connections are being used (a.k.a. an "exhausted" pool).
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
