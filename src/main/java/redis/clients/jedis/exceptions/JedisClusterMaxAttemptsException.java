package redis.clients.jedis.exceptions;

/**
 * @deprecated This exception will be removed in next major release. Use
 * {@link JedisClusterOperationException}.
 */
@Deprecated
public class JedisClusterMaxAttemptsException extends JedisClusterOperationException {
  private static final long serialVersionUID = 167600616259092761L;

  public JedisClusterMaxAttemptsException(String message) {
    super(message);
  }

  public JedisClusterMaxAttemptsException(Throwable cause) {
    super(cause);
  }

  public JedisClusterMaxAttemptsException(String message, Throwable cause) {
    super(message, cause);
  }
}
