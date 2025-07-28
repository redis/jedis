package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

@Internal
@FunctionalInterface
public interface PushConsumer {

  /**
   * Handle a push message.
   * <p>
   * Messages are not processed by default. Handlers should update the context's processed flag to
   * true if they have processed the message.
   * </p>
   * @param context The context of the message to respond to.
   */
  void accept(PushConsumerContext context);

}
