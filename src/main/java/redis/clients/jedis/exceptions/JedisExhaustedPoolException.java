package redis.clients.jedis.exceptions;

/**
 * This exception will be thrown when the Jedis client won't be able to retreive a connection from
 * the pool, since all the connections are being used (aka. an "exhausted" pool).
 */
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
