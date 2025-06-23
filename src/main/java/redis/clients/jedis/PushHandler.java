package redis.clients.jedis;

@FunctionalInterface
public interface PushHandler {

  /**
   * Handle a push message.
   *
   * Messages are not processed by default.  Handlers should update the context's processed flag to true if they
   * have processed the message.
   *
   * @param context The context of the message to respond to.
   */
  void handlePushMessage(PushHandlerContext context);

}
