package redis.clients.jedis.exceptions;

/**
 * This exception is thrown when a cluster error response is received from Redis.
 */
public class JedisClusterException extends JedisDataException {
  private static final long serialVersionUID = 3878126572474819403L;

  public JedisClusterException(Throwable cause) {
    super(cause);
  }

  public JedisClusterException(String message, Throwable cause) {
    super(message, cause);
  }

  public JedisClusterException(String message) {
    super(message);
  }
}
