package redis.clients.jedis.exceptions;

/**
 * @deprecated This will be removed in next major release.
 */
@Deprecated
public class AbortedTransactionException extends JedisDataException {

  public AbortedTransactionException(final String message) {
    super(message);
  }

  public AbortedTransactionException(final Throwable cause) {
    super(cause);
  }

  public AbortedTransactionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
