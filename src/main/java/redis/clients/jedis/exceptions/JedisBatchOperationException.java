package redis.clients.jedis.exceptions;

public class JedisBatchOperationException extends JedisException {
  private static final long serialVersionUID = -1464241173812705493L;

  public JedisBatchOperationException(String message) {
    super(message);
  }

}
