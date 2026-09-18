package redis.clients.jedis.exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.HostAndPort;

/**
 * Exception thrown when a broadcast command fails on one or more cluster nodes.
 * <p>
 * This exception collects replies from all nodes, including both successful responses and errors.
 * Use {@link #getReplies()} to inspect the per-node results.
 * <p>
 * Note: This exception extends {@link JedisDataException} just so existing applications catching
 * JedisDataException do not get broken.
 */
// TODO: extends JedisException
public class JedisBroadcastException extends JedisDataException {

  private static final String BROADCAST_ERROR_MESSAGE = "A failure occurred while broadcasting the command.";

  private final Map<HostAndPort, Object> replies = new HashMap<>();

  public JedisBroadcastException() {
    super(BROADCAST_ERROR_MESSAGE);
  }

  public void addReply(HostAndPort node, Object reply) {
    replies.put(node, reply);
  }

  public Map<HostAndPort, Object> getReplies() {
    return Collections.unmodifiableMap(replies);
  }

  /**
   * Prepares the exception for throwing by:
   * <ul>
   *   <li>Refreshing the stack trace to show where it's thrown from (not where it was created)</li>
   *   <li>Setting the cause to the first error encountered for better debugging</li>
   * </ul>
   * @return this exception, ready to be thrown
   */
  public JedisBroadcastException prepareToThrow() {
    // Refresh stack trace to show throw location instead of creation location
    fillInStackTrace();

    // Set the first exception as the cause for better debugging
    if (getCause() == null) {
      for (Object reply : replies.values()) {
        if (reply instanceof Throwable) {
          initCause((Throwable) reply);
          break;
        }
      }
    }

    return this;
  }

  @Override
  public String getMessage() {
    StringBuilder sb = new StringBuilder(BROADCAST_ERROR_MESSAGE);

    int errorCount = 0;
    int successCount = 0;
    String firstErrorMessage = null;

    for (Object reply : replies.values()) {
      if (reply instanceof Throwable) {
        errorCount++;
        if (firstErrorMessage == null) {
          firstErrorMessage = ((Throwable) reply).getMessage();
        }
      } else {
        successCount++;
      }
    }

    sb.append(" (").append(successCount).append(" succeeded, ")
        .append(errorCount).append(" failed");

    if (firstErrorMessage != null) {
      sb.append("; first error: ").append(firstErrorMessage);
    }

    sb.append(")");

    return sb.toString();
  }
}
