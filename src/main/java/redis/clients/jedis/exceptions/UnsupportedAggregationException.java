package redis.clients.jedis.exceptions;

/**
 * Exception thrown when an unsupported aggregation operation is attempted.
 * <p>
 * This exception is thrown when the aggregation policy cannot handle the given
 * data types. For example, when trying to sum non-numeric values or compare
 * non-comparable types.
 * </p>
 */
public class UnsupportedAggregationException extends ClusterAggregationException {

  private static final long serialVersionUID = 1L;

  public UnsupportedAggregationException(String message) {
    super(message);
  }

  public UnsupportedAggregationException(Throwable cause) {
    super(cause);
  }

  public UnsupportedAggregationException(String message, Throwable cause) {
    super(message, cause);
  }
}

