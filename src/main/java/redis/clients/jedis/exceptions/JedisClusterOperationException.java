package redis.clients.jedis.exceptions;

import redis.clients.jedis.HostAndPort;

/**
 * Error while processing cluster operations. This is not an error reply from Redis.
 * <p>
 * This exception can optionally include the {@link HostAndPort} of the node that caused the
 * failure, which is useful for debugging and error reporting in multi-node operations.
 */
public class JedisClusterOperationException extends JedisException {

  private static final long serialVersionUID = 8124535086306604887L;

  private final HostAndPort node;

  public JedisClusterOperationException(String message) {
    super(message);
    this.node = null;
  }

  public JedisClusterOperationException(Throwable cause) {
    super(cause);
    this.node = null;
  }

  public JedisClusterOperationException(String message, Throwable cause) {
    super(message, cause);
    this.node = null;
  }

  /**
   * Creates an exception with the node that caused the failure.
   *
   * @param message the detail message
   * @param node the node that caused the failure (may be null)
   */
  public JedisClusterOperationException(String message, HostAndPort node) {
    super(message);
    this.node = node;
  }

  /**
   * Creates an exception with the node that caused the failure.
   *
   * @param message the detail message
   * @param cause the underlying cause
   * @param node the node that caused the failure (may be null)
   */
  public JedisClusterOperationException(String message, Throwable cause, HostAndPort node) {
    super(message, cause);
    this.node = node;
  }

  /**
   * Returns the node that caused this exception, if known.
   *
   * @return the node that caused the failure, or null if unknown
   */
  public HostAndPort getNode() {
    return node;
  }
}
