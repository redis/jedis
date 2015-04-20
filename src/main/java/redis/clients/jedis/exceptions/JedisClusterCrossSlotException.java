package redis.clients.jedis.exceptions;

public class JedisClusterCrossSlotException extends JedisClusterException {
  private static final long serialVersionUID = -6355518994901704067L;

  public JedisClusterCrossSlotException(Throwable cause) {
    super(cause);
  }

  public JedisClusterCrossSlotException(String message, Throwable cause) {
    super(message, cause);
  }

  public JedisClusterCrossSlotException(String message) {
    super(message);
  }
}