package redis.clients.jedis.exceptions;

public class InvalidURIException extends JedisException {

  private static final long serialVersionUID = -781691993326357802L;

  public InvalidURIException(String message) {
    super(message);
  }

  public InvalidURIException(Throwable cause) {
    super(cause);
  }

  public InvalidURIException(String message, Throwable cause) {
    super(message, cause);
  }

}
