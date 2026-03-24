package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

/**
 * Context object for push message processing.
 * <p>
 * Used by {@link PushConsumer} to inspect and control message processing.
 * <p>
 * The {@link PushConsumerContext} object is created for each push message and passed to all
 * registered {@link PushConsumer} handlers. Handlers can inspect the message and decide whether to
 * let push message be returned to the client or process it themselves and prevent it from being
 * returned to the client.
 * </p>
 */
@Internal
public class PushConsumerContext {
  private final PushMessage message;

  private boolean forwardToClient = false;

  public PushConsumerContext(PushMessage message) {
    this.message = message;
  }

  /**
   * Get the push message being processed.
   * @return The push message
   */
  public PushMessage getMessage() {
    return message;
  }

  /**
   * Check if the message should be returned to the client.
   * @return true if the message should be returned to the client
   */
  public boolean isForwardToClient() {
    return forwardToClient;
  }

  /**
   * Set whether the message should be returned to the client. By default, if no handler sets this
   * flag, the message will not be returned to the client. and silently consumed.
   * @param forwardToClient
   */
  public void setForwardToClient(boolean forwardToClient) {
    this.forwardToClient = forwardToClient;
  }

}
