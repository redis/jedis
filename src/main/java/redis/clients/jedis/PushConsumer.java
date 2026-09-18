package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

@Experimental
public interface PushConsumer {

  /**
   * Process a push message. Each PushConsumer can decide to:
   * <ul>
   * <li>Allow the message to be returned to the caller by calling
   * {@link PushConsumerContext#propagate()}</li>
   * <li>Process the message and prevent it from being processed further by calling
   * {@link PushConsumerContext#drop()}</li>
   * </ul>
   * @see PushConsumerChain
   * @see PushConsumerContext
   * @param context The context of the push message
   */
  PushConsumerContext handle(PushConsumerContext context);

}
