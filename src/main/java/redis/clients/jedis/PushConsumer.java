package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

@Internal
@FunctionalInterface
public interface PushConsumer {

  /**
   * Process a push message. Each PushConsumer can decide to:
   * <ul>
   * <li>Allow the message to be returned to the caller by calling
   * {@link PushConsumerContext#setReturnToCaller(boolean)}</li>
   * <li>Process the message and prevent it from being returned by not calling
   * {@link PushConsumerContext#setReturnToCaller(boolean)}</li>
   * </ul>
   * Following handlers in the chain can override the decision of previous handlers. By default, if
   * no handler sets the flag, the message will not be returned to the caller.
   * @see PushConsumerContext#setReturnToCaller(boolean)
   * @see PushConsumerChain
   * @param context The context of the push message
   */
  void accept(PushConsumerContext context);

}
