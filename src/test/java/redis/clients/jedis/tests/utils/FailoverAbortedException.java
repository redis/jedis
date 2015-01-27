package redis.clients.jedis.tests.utils;

public class FailoverAbortedException extends RuntimeException {
  private static final long serialVersionUID = 1925110762858409954L;

  public FailoverAbortedException(String message) {
    super(message);
  }

  public FailoverAbortedException(Throwable cause) {
    super(cause);
  }

  public FailoverAbortedException(String message, Throwable cause) {
    super(message, cause);
  }
}
