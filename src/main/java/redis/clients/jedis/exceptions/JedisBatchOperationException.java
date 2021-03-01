package redis.clients.jedis.exceptions;

/**
 * WARNING: This exception is extending {@link JedisDataException} to maintain backward
 * compatibility. But this will not do so in future. Instead, this would extend
 * {@link JedisException} directly.
 *
 * If you are trying to catch this exception, you should use either
 * {@link JedisBatchOperationException} or {@link JedisException}. You should not use
 * {@link JedisDataException} for this purpose.
 */
public class JedisBatchOperationException extends JedisDataException { // TODO

  private static final long serialVersionUID = -1464241173812705493L;

  public JedisBatchOperationException(String message) {
    super(message);
  }
}
