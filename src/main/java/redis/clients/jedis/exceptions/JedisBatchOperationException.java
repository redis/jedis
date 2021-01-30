package redis.clients.jedis.exceptions;

/**
 * @deprecated This class is extending {@link JedisDataException} to maintain backward
 * compatibility. But this will not do so in future. Instead, This would extend
 * {@link JedisException} directly.
 */
@Deprecated
public class JedisBatchOperationException extends JedisDataException {

  private static final long serialVersionUID = -1464241173812705493L;

  public JedisBatchOperationException(String message) {
    super(message);
  }
}
