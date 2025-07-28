package redis.clients.jedis;

@FunctionalInterface
public interface PushListener {

  /**
   * Interface to be implemented by push message listeners that are interested in listening to
   * {@link PushMessage}. Requires Redis 6+ using RESP3.
   * @author Ivo Gaydajiev
   * @since 6.1
   * @see PushMessage
   */
  void onPush(PushMessage push);

}
