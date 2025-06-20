package redis.clients.jedis;

@FunctionalInterface
public interface PushHandler {

  /**
   * Handle a push message.
   *
   *
   * @param message message to respond to.
   * @return push message to propagate, or null to stop propagation.
   */
  PushHandlerOutput handlePushMessage(PushEvent message);

}
