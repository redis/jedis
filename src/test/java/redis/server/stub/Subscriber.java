package redis.server.stub;

/**
 * Interface for pub/sub subscribers.
 * <p>
 * Abstracts the concept of a subscriber from the implementation details of client connections. This
 * allows {@link PubSubManager} to work with any type of subscriber, not just {@link ClientHandler}.
 * <p>
 * <b>Design Benefits</b>:
 * <ul>
 * <li>Decouples pub/sub logic from client connection handling</li>
 * <li>Enables testing with mock subscribers</li>
 * <li>Allows future subscriber types (e.g., internal server subscribers)</li>
 * <li>Cleaner separation of concerns</li>
 * </ul>
 * <p>
 * <b>Subscription Management</b>: Subscribers are properly managed through:
 * <ul>
 * <li>Added via SUBSCRIBE/PSUBSCRIBE commands</li>
 * <li>Removed via UNSUBSCRIBE/PUNSUBSCRIBE commands</li>
 * <li>Removed on disconnect via {@link PubSubManager#unsubscribeAll(Subscriber)}</li>
 * </ul>
 * <p>
 * <b>Thread Safety</b>: Implementations must ensure {@link #sendPushMessage(String...)} is
 * thread-safe, as it may be called from the single-threaded command executor.
 */
public interface Subscriber {

  /**
   * Send a push message to this subscriber.
   * <p>
   * Push messages are RESP3 protocol messages sent asynchronously to subscribers (e.g., pub/sub
   * notifications, server events). This method must be thread-safe.
   * <p>
   * <b>RESP3 Push Format</b>:
   * 
   * <pre>
   * &gt;3        &lt;- Push indicator with element count
   * $7
   * message
   * $4
   * news
   * $12
   * Hello World!
   * </pre>
   * <p>
   * <b>Examples</b>:
   * <ul>
   * <li>Regular message: {@code sendPushMessage("message", "news", "Breaking story")}</li>
   * <li>Pattern message:
   * {@code sendPushMessage("pmessage", "news.*", "news.sports", "Game on!")}</li>
   * <li>Server event: {@code sendPushMessage("invalidate", "key1", "key2")}</li>
   * </ul>
   * @param args push message elements (first element is typically the message type)
   */
  void sendPushMessage(String... args);
}
