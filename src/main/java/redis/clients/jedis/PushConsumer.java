package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

@Internal
@FunctionalInterface
public interface PushConsumer {

  /**
   * Process a push message. Each PushConsumer can decide to:
   * <ul>
   * <li>Allow the message to be returned to the client by calling
   * {@link PushConsumerContext#setForwardToClient(boolean)}</li>
   * <li>Process the message and prevent forwarding by not calling
   * {@link PushConsumerContext#setForwardToClient(boolean)}</li>
   * </ul>
   * Following handlers in the chain can override the decision of previous handlers. By default, if
   * no handler sets the forward flag, the message not be returned to the client.
   * @see PushConsumerContext#setForwardToClient(boolean)
   * @see PushConsumerChain
   * @param context The context of the push message
   */
  void accept(PushConsumerContext context);

}
