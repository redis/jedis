package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;

/**
 * Context object for push message processing.
 * <p>
 * Used by {@link PushConsumerChainImpl} to inspect and control message processing. The
 * {@link PushConsumerContext} object is created for each push message and passed to all registered
 * {@link PushConsumer}'s.
 * </p>
 * Consumers can inspect the message and decide weather to :
 * <ul>
 * <li>return to the caller and skip the rest of consumers by calling
 * {@link PushConsumerContext#propagate()}</li>
 * <li>drop message without processing it further by calling {@link PushConsumerContext#drop()}
 * to</li>
 * <li>inspect and let it be processed by following consumers</li>
 * </ul>
 */
@Experimental
public class PushConsumerContext {
  private final PushMessage message;

  private boolean propagate = false;
  private boolean drop = false;

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
   * Check if the message should be returned to the caller.
   * @return true if the message should be returned to the caller
   */
  public boolean shouldPropagate() {
    return propagate;
  }

  /**
   * Set whether the message should be returned to the caller. By default, if no consumer sets this
   * flag, the message will not be returned to the caller and will be silently consumed.
   */
  public void propagate() {
    this.propagate = true;
  }

  /**
   * Set whether the message should be dropped, it is not returned to caller or passed to following
   * consumers.
   */
  public void drop() {
    this.drop = true;
  }

  /**
   * Check if the message should be dropped and not processed further.
   * @return true if the message should be dropped
   */
  public boolean shouldDrop() {
    return drop;
  }
}