package redis.clients.jedis.exceptions;

/**
 * Exception thrown when cluster reply aggregation fails.
 * <p>
 * This exception is thrown when aggregating replies from multiple cluster nodes and the aggregation
 * policy requirements are not met (e.g., ALL_SUCCEEDED policy requires all replies to be equal, but
 * different values were received).
 * </p>
 */
public class ClusterAggregationException extends JedisClusterOperationException {

  private static final long serialVersionUID = 1L;

  public ClusterAggregationException(String message) {
    super(message);
  }

  public ClusterAggregationException(Throwable cause) {
    super(cause);
  }

  public ClusterAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}
