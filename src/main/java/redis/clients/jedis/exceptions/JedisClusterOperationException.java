package redis.clients.jedis.exceptions;

/**
 * This exception is thrown due to a cluster operation error which may occur without any Redis operation.
 */
public class JedisClusterOperationException extends JedisException {
  private static final long serialVersionUID = 8124535086306604887L;

  public JedisClusterOperationException(String message) {
    super(message);
  }

  public JedisClusterOperationException(Throwable cause) {
    super(cause);
  }

  public JedisClusterOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}